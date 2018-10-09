package com.example.biophone;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.jtransforms.fft.DoubleFFT_1D;

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import uk.me.berndporr.iirj.Butterworth;

public class HeartRateService extends Service implements SensorEventListener {
  // センサ関連
  SensorManager sensorManager;
  Sensor sensor;

  // センサから取得した加速度データ（生データ）
  private double x, y, z;

  // 生データの個数をカウントする
  private int raw_count = 0;

  // 各加速度用の配列
  private double[] xValue = new double[15];
  private double[] yValue = new double[15];
  private double[] zValue = new double[15];

  // 各加速度の15個のデータの平均値のための配列
  // 0 : x軸, 1 : y軸, 2 : z軸
  double[] ave = new double[3];

  // 7-13Hzを通す1次のバターワース型バンドパスフィルタ
  Butterworth butterworth1 = new Butterworth();

  // フィルタ後の各加速度
  private double xBandValue;
  private double yBandValue;
  private double zBandValue;

  // 0.66-2.5Hzを通す1次のバターワース型バンドパスフィルタ
  Butterworth butterworth2 = new Butterworth();

  // 各軸の2乗の和の平方根
  private double sumXYZ;

  // FFT 関連
  private int FFT_SIZE = 1000;                     // FFT するデータの個数
  DoubleFFT_1D fft = new DoubleFFT_1D(FFT_SIZE);    // FFTのインスタンス生成
  double[] fft_data = new double[FFT_SIZE];      // FFT後のデータ配列

  // サンプリング周波数
  int fs = 100;

  // ピーク周波数のための変数
  int maxInd;
  double magnitude;
  double maxMagnitude;

  // パルス波形関連
  private double[] pulseWave = new double[FFT_SIZE];   // パルス波形データの配列
  private int pulseWaveCnt = 0;                           // パルス波形のデータをカウントする

  // 得られた心拍数を int 型に変換するために double 型で一時的に保持
  double HRtmp;

  // 心拍数データ関連
  private int HR_SIZE = 100;                       // 心拍数データの個数
  private int[] heartRate = new int[HR_SIZE];   // 心拍数データの配列
  private int aveHeartRate;                       // 10ミリ秒間隔で算出した心拍数データの平均値
  private int heartRateCnt = 0;                   // 心拍数のデータをカウントする

  // タイマー用の変数
  // 加速度取得 & 心拍数算出
  private Timer heartRateTimer;					                //タイマー用
  private HeartRateTimerTask heartRateTimerTask;	      //タイマタスククラス

  // MQTT 関連
  private MqttAndroidClient mqttAndroidClient;
  private final String URL = "URLとポートを記入";

  // ブロードキャスト（MainActivity へデータを送る）
  private final String serviceTAG = "HeartRateService";

  // 通知関連
  private NotificationManager notificationManager;
  private static final int NOTIFY_ID = 2212231;                          // 通知 ID
  private static final String ChannelID = "Biophone_channel";         // システムに登録するチャンネル ID（アプリでユニークな ID）
  private static final CharSequence channelName = "Biophone";          // カテゴリー名（通知設定画面に表示される情報）
  private static final int imp = NotificationManager.IMPORTANCE_HIGH;  // チャンネルの重要度（0～4）
  private static final String desc = "Biophone の通知設定";             // 通知の詳細情報（通知設定画面に表示される情報）


  public void onCreate() {
    super.onCreate();

    // MQTT のインスタンス
    mqttAndroidClient  = new MqttAndroidClient(HeartRateService.this, URL, "");

    // MQTT ブローカーに接続
    try {
      // MQTT ブローカーに接続されていない場合
      if (!mqttAndroidClient.isConnected()) {
        mqttAndroidClient.connect();  // 接続
      }
    } catch (MqttException e) {
      e.printStackTrace();
    }

    // 加速度センサー関連
    sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);       // センサーを管理しているサービスの呼び出し
    sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);  // 標準の加速度センサーをsensorに登録
    sensorManager.registerListener(this, sensor, 10000);                   // registerListener(listener, sensor, micro sec)

    // 7-13Hzを通す1次のバターワース型バンドパスフィルタ
    // .bandPass(フィルタ次数, サンプリング周波数, 中心周波数, 周波数の幅)
    butterworth1.bandPass(1, 100, 10, 6);

    // 0.66-2.5Hzを通す1次のバターワース型バンドパスフィルタ
    butterworth2.bandPass(1, 100, 1.58, 1.84);

    // タイマーインスタンスを生成
    heartRateTimer = new Timer();

    // タイマータスクインスタンスを生成
    heartRateTimerTask = new HeartRateTimerTask();

    // タイマースケジュール設定＆開始 mainTimer.schedule(new MainTimerTask(), long delay, long period)
    // delay: はじめのタスクが実行されるまでの時間（単位はミリ秒），period: タスクが実行される周期（単位はミリ秒）
    heartRateTimer.schedule(heartRateTimerTask, 5000, 10);
  }

  public int onStartCommand(Intent intent, int flags, int startId) {
    // 通知設定
    notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

    // 端末の OS バージョンによって処理を変更
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      NotificationChannel mChannel = new NotificationChannel(ChannelID, channelName, imp);
      mChannel.setDescription(desc);
      mChannel.setLightColor(Color.CYAN);
      mChannel.canShowBadge();
      mChannel.setShowBadge(true);
      notificationManager.createNotificationChannel(mChannel);

      Notification notification = new Notification.Builder(this, ChannelID)
        .setContentTitle("Biophone")
        .setContentText("心拍数を計測しています")
        .setBadgeIconType(R.drawable.hearts_notif_icon)
        .setSmallIcon(R.drawable.hearts_notif_icon)
        .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher_round))
        .setColor(ContextCompat.getColor(HeartRateService.this, R.color.colorNotifIcon))
        .setAutoCancel(true)
        .setContentIntent(
          PendingIntent.getActivity(
            this, MainActivity.ACTIVITY_ID,
            new Intent(this, MainActivity.class), PendingIntent.FLAG_CANCEL_CURRENT
          )
        )
        .build();

      startForeground(NOTIFY_ID, notification);
    } else {
      Notification notification = new Notification.Builder(this)
        .setContentTitle("Biophone")
        .setContentText("心拍数を計測しています")
        .setSmallIcon(R.drawable.hearts_notif_icon)
        .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher_round))
        .setColor(ContextCompat.getColor(HeartRateService.this, R.color.colorNotifIcon))
        .setAutoCancel(true)
        .setContentIntent(
          PendingIntent.getActivity(
            this, MainActivity.ACTIVITY_ID,
            new Intent(this, MainActivity.class), PendingIntent.FLAG_CANCEL_CURRENT
          )
        )
        .build();

      startForeground(NOTIFY_ID, notification);
    }

    return START_NOT_STICKY;
  }

  public void onDestroy() {
    super.onDestroy();
    notificationManager.cancel(NOTIFY_ID);
    sensorManager.unregisterListener(this);

    // 実行中のタイマー処理を終了できるタイミングまで処理を行い、以降処理を再開させない
    heartRateTimer.cancel();

    // MQTT ブローカとの接続を切断
    try {
      // MQTT ブローカと接続している場合
      if (mqttAndroidClient.isConnected()) {
        mqttAndroidClient.disconnect();  // 切断
        mqttAndroidClient.unregisterResources();
      }
    } catch (MqttException e) {
      e.printStackTrace();
    }
  }

  // 心拍数を計測する処理（MainActivity とは別のスレッド）
  public class HeartRateTimerTask extends TimerTask {
    @Override
    public void run() {
      // 生データが15個集まったら各軸の平均値を求める
      if (raw_count < 15) {
        // 各加速度の値を更新
        xValue[raw_count] = x;
        yValue[raw_count] = y;
        zValue[raw_count] = z;
        raw_count++;
      } else {
        //////////////////////////////////////////
        // 各軸の加速度の平均値を求める
        // 平均値の配列を0で初期化する
        for (int i = 0; i < 3; i++) {
          ave[i] = 0;
        }

        // 各軸の加速度の平均値を求める
        for (int i = 0; i < raw_count; i++) {
          ave[0] += xValue[i];  // x軸
          ave[1] += yValue[i];  // y軸
          ave[2] += zValue[i];  // z軸
        }
        ave[0] /= raw_count;  // x軸の平均値
        ave[1] /= raw_count;  // y軸の平均値
        ave[2] /= raw_count;  // z軸の平均値
        //////////////////////////////////////////

        //////////////////////////////////////////
        // 各軸の加速度の値を更新
        for (int i = 0; i < raw_count - 1; i++) {
          xValue[i] = xValue[i + 1];
          yValue[i] = yValue[i + 1];
          zValue[i] = zValue[i + 1];
        }
        xValue[raw_count - 1] = x;
        yValue[raw_count - 1] = y;
        zValue[raw_count - 1] = z;
        //////////////////////////////////////////

        //////////////////////////////////////////
        // 各軸の加速度値から各軸の移動平均値を引く
        ave[0] = xValue[raw_count - 1] - ave[0];  // x軸
        ave[1] = yValue[raw_count - 1] - ave[1];  // y軸
        ave[2] = zValue[raw_count - 1] - ave[2];  // z軸
        //////////////////////////////////////////

        ////////////////////////////////////////////////////////////
        // 7-13Hzを通す1次のバターワース型バンドパスフィルタをかける
        xBandValue = butterworth1.filter(ave[0]);
        yBandValue = butterworth1.filter(ave[1]);
        zBandValue = butterworth1.filter(ave[2]);
        ////////////////////////////////////////////////////////////

        ////////////////////////////////////////////////////////////
        // 各軸の2乗の和の平方根を求める
        sumXYZ = Math.sqrt( Math.pow(xBandValue, 2) + Math.pow(yBandValue, 2) + Math.pow(zBandValue, 2) );
        ////////////////////////////////////////////////////////////

        // 各軸の2乗の和の平方根のデータ（pulseWave 配列）がFFT_SIZE個集まったらFFTを行う
        if (pulseWaveCnt < FFT_SIZE) {
          ////////////////////////////////////////////////////////////
          // 0.66-2.5Hzを通す1次のバターワース型バンドパスフィルタをかける
          pulseWave[pulseWaveCnt] = butterworth2.filter(sumXYZ);
          pulseWaveCnt++;
          ////////////////////////////////////////////////////////////
        } else {
          ///////////////////////////////////////////////////////////
          // データをコピー
          for (int i = 0; i < FFT_SIZE; i++) {
            fft_data[i] = pulseWave[i];
          }
          ///////////////////////////////////////////////////////////

          ////////////////////////////////////////////////////////////
          // pulseWave値の更新
          for (int i = 0; i < pulseWaveCnt - 1; i++) {
            pulseWave[i] = pulseWave[i + 1];
          }
          pulseWave[pulseWaveCnt - 1] = butterworth2.filter(sumXYZ);
          ////////////////////////////////////////////////////////////

          //////////////////////////////////////////////////////////
          // FFT
          fft.realForward(fft_data);
          //////////////////////////////////////////////////////////

          //////////////////////////////////////////////////////////
          // ピーク周波数を求める
          maxInd = 0;
          maxMagnitude = -1;
          for (int i = 0; i < FFT_SIZE / 2; i++) {
            magnitude = Math.sqrt( Math.pow(fft_data[2*i], 2) + Math.pow(fft_data[2*i+1], 2) );
            if (  0.66 <= (double) i * fs / FFT_SIZE && (double) i * fs / FFT_SIZE <= 2.5 && maxMagnitude < magnitude ) {
              maxMagnitude = magnitude;
              maxInd = i;
            }
          }
          //////////////////////////////////////////////////////////

          ///////////////////////////////////////////////////////////
          // 1 秒間に計測した心拍数の平均値を求める（heartRateCnt が HR_SIZE 以上の場合）
          if (heartRateCnt < HR_SIZE) {
            // 心拍数データの個数が配列の最大値未満
            HRtmp = (double) maxInd * fs / FFT_SIZE * 60;   // 心拍数を算出
            heartRate[heartRateCnt] = (int) HRtmp;          // 整数に変換
            heartRateCnt++;
          } else {
            // heartRateCnt を初期化
            heartRateCnt = 0;

            //////////////////////////////////////////////////////////
            // 10ミリ秒間隔で算出した心拍数データの平均値を求める
            aveHeartRate = 0;
            for (int i = 0; i < HR_SIZE; i++) {
              aveHeartRate += heartRate[i];
            }
            HRtmp = aveHeartRate / HR_SIZE;
            aveHeartRate = (int) HRtmp;
            //////////////////////////////////////////////////////////

            //////////////////////////////////////////////////////////
            // 心拍数データの更新
            for (int i = 0; i < HR_SIZE - 1; i++) {
              heartRate[i] = heartRate[i + 1];
            }
            HRtmp = (double) maxInd * fs / FFT_SIZE * 60;
            heartRate[HR_SIZE - 1] = (int) HRtmp;
            //////////////////////////////////////////////////////////

            //////////////////////////////////////////////////////////
            // MainActivity へ取得した心拍数を送信
            Intent broadcastIntent = new Intent();
            broadcastIntent.putExtra("heart_rate", aveHeartRate);
            broadcastIntent.setAction(serviceTAG);
            sendBroadcast(broadcastIntent);
            //////////////////////////////////////////////////////////

            Log.i("HeartRate", String.valueOf(aveHeartRate));

            ////////////////////////////////////////////////////////////
            // 現在の日時を取得
            CharSequence timeTXT = android.text.format.DateFormat.format("yyyy-MM-dd kk:mm:ss", Calendar.getInstance());

            // 送信するデータの作成（日付,心拍数）
            String message = String.valueOf(timeTXT) + ',' + String.valueOf(aveHeartRate);

            // MQTT で VPS にデータを送信
            try {
              mqttAndroidClient.publish("topic/heart_rate", message.getBytes(), 0, false);
            } catch (MqttPersistenceException e) {
              e.printStackTrace();
            } catch (MqttException e) {
              e.printStackTrace();
            }
            ////////////////////////////////////////////////////////////
          }
        }
      }
    }
  }

  // センサーの値が変わるたびに呼び出されるメソッド
  @Override
  public void onSensorChanged(SensorEvent event) {
    // 加速度センサの値を変数に代入
    if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
      x = event.values[0];
      y = event.values[1];
      z = event.values[2];
    }
  }

  // センサの精度を変更する際に呼び出されるメソッド
  @Override
  public void onAccuracyChanged(Sensor sensor, int accuracy) {

  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }
}
