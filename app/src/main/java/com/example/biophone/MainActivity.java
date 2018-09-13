package com.example.biophone;

import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import org.jtransforms.fft.DoubleFFT_1D;

import java.util.Timer;
import java.util.TimerTask;

import uk.me.berndporr.iirj.Butterworth;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
  // センサ関連
  SensorManager manager;
  Sensor sensor;

  // 表示用のテキストビュー
  TextView xTextView;
  TextView yTextView;
  TextView zTextView;
  TextView fftTextView;

  // 各加速度用の配列
  private double[] xValue = new double[15];
  private double[] yValue = new double[15];
  private double[] zValue = new double[15];

  // 平均値のための配列
  double[] ave = new double[3];

  // 7-13Hzを通す1次のバターワース型バンドパスフィルタ
  Butterworth butterworth1 = new Butterworth();

  // 0.66-2.5Hzを通す1次のバターワース型バンドパスフィルタ
  Butterworth butterworth2 = new Butterworth();

  // 各軸の2乗の和の平方根
  private double sumXYZ;

  // FFTのサイズ
  private int FFT_SIZE = 1000;

  // FFTのインスタンス生成
  DoubleFFT_1D fft = new DoubleFFT_1D(FFT_SIZE);

  // FFT後のデータ配列
  double[] fft_data = new double[FFT_SIZE];

  // サンプリング周波数
  int fs = 100;

  // ピーク周波数のための変数
  int maxInd;
  double magnitude;
  double maxMagnitude;

  // パルス波形データの配列
  private double[] pulseWave = new double[FFT_SIZE];

  // パルス波形のデータをカウントする
  private int pulseWaveCnt = 0;

  // フィルタ後の各加速度の配列
  private double xBandValue;
  private double yBandValue;
  private double zBandValue;

  private double x, y, z;

  // 生データの個数をカウントする
  private int raw_count = 0;

  // タイマー用の変数
  private Timer mainTimer;					           //タイマー用
  private MainTimerTask mainTimerTask;		     //タイマタスククラス
  private Handler mHandler = new Handler();   //UI Threadへのpost用ハンドラ
  private Timer hrTimer;
  private HeartRateTimerTask hrTimerTask;
  private Handler hrHandler = new Handler();

  // グラフ用の変数
  LineChart mChart;

  // START / STOPボタン
  Button button = null;
  private boolean flag = true;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    // TextViewの取得
    xTextView = (TextView) findViewById(R.id.xValue);
    yTextView = (TextView) findViewById(R.id.yValue);
    zTextView = (TextView) findViewById(R.id.zValue);
    fftTextView = (TextView) findViewById(R.id.fft);

    // LineChartの取得
    mChart = (LineChart) findViewById(R.id.lineChart);

    // Buttonの取得
    button = (Button) findViewById(R.id.button);

    // センサーを管理しているサービスの呼び出し
    manager = (SensorManager)getSystemService(SENSOR_SERVICE);

    // 標準の加速度センサーをsensorに登録
    sensor = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

    // 表のタイトルを空にする
    mChart.setDescription("");

    // 空のLineData型インスタンスを追加
    mChart.setData(new LineData());

    // 7-13Hzを通す1次のバターワース型バンドパスフィルタ
    // .bandPass(フィルタ次数, サンプリング周波数, 中心周波数, 周波数の幅)
    butterworth1.bandPass(1, 100, 10, 6);

    // 0.66-2.5Hzを通す1次のバターワース型バンドパスフィルタ
    butterworth2.bandPass(1, 100, 1.58, 1.84);

    // ボタンを押したとき
    button.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        // flag が true の時
        if (flag){
          flag = false;
          button.setText("STOP");

          //タイマーインスタンス生成
          mainTimer = new Timer();

          hrTimer = new Timer();

          //タスククラスインスタンス生成
          mainTimerTask = new MainTimerTask();

          hrTimerTask = new HeartRateTimerTask();

          // タイマースケジュール設定＆開始 mainTimer.schedule(new MainTimerTask(), long delay, long period)
          // delay: はじめのタスクが実行されるまでの時間（単位はミリ秒），period: タスクが実行される周期（単位はミリ秒）
          mainTimer.schedule(mainTimerTask, 0, 10);

          hrTimer.schedule(hrTimerTask, 0, 1000);
        }
        // flag が false の時
        else {
          flag = true;
          button.setText("START");

          // 実行中のタイマー処理を終了できるタイミングまで処理を行い、以降処理を再開させない
          mainTimer.cancel();

          hrTimer.cancel();
        }
      }
    });
  }

  public class MainTimerTask extends TimerTask {
    @Override
    public void run() {
      // 定期的に実行したい処理を記述（Viewはさらに別のスレッドが必要）
      mHandler.post( new Runnable() {
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
            ave[0] = xValue[raw_count-1] - ave[0];  // x軸
            ave[1] = yValue[raw_count-1] - ave[1];  // y軸
            ave[2] = zValue[raw_count-1] - ave[2];  // z軸
            //////////////////////////////////////////

            //////////////////////////////////////////
            // テキストビューに
            // 新たに得られた各軸の加速度から移動平均値を引いた値を表示する
            //xTextView.setText("X : " + ave[0]);
            //yTextView.setText("Y : " + ave[1]);
            //zTextView.setText("Z : " + ave[2]);
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

            // 各軸の2乗の和の平方根のデータがFFT_SIZE個集まったらFFTを行う
            if (pulseWaveCnt < FFT_SIZE) {
              ////////////////////////////////////////////////////////////
              // 0.66-2.5Hzを通す1次のバターワース型バンドパスフィルタをかける
              pulseWave[pulseWaveCnt] = butterworth2.filter(sumXYZ);
              pulseWaveCnt++;
              ////////////////////////////////////////////////////////////
            } else {
              ///////////////////////////////////////////////////////////
              // FFTを行う
              // データをコピー
              for (int i = 0; i < FFT_SIZE; i++) {
                fft_data[i] = pulseWave[i];
              }

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
              //System.out.println("maxInd : " + maxInd + "  maxMagnitude : " + maxMagnitude);
              //fftTextView.setText("ピーク周波数：" + (double) maxInd * fs / FFT_SIZE + "\n心拍数：" + (double) maxInd * fs / FFT_SIZE * 60);
              ///////////////////////////////////////////////////////////

              ///////////////////////////////////////////////////////////
              // グラフの描画
              /*
              LineData data = mChart.getLineData();
              if (data != null) {
                ILineDataSet set = data.getDataSetByIndex(0);
                if (set == null) {
                  set = createSet("heart_rate", Color.BLUE);
                  data.addDataSet(set);
                }
                data.addEntry( new Entry( data.getEntryCount(), (float) maxInd * fs / FFT_SIZE * 60 ), 0 );
                data.notifyDataChanged();
              }
              mChart.notifyDataSetChanged();  // 表示の更新のために変更を通知する
              mChart.setVisibleXRangeMaximum(50); // 表示の幅を決定する
              mChart.moveViewToX( data.getEntryCount() ); // 最新のデータまで表示を移動させる
              */
              ///////////////////////////////////////////////////////////

              ////////////////////////////////////////////////////////////
              // pulseWave値の更新
              for (int i = 0; i < pulseWaveCnt - 1; i++) {
                pulseWave[i] = pulseWave[i + 1];
              }
              pulseWave[pulseWaveCnt - 1] = butterworth2.filter(sumXYZ);
              ////////////////////////////////////////////////////////////
            }
          }
        }
      });
    }
  }

  public class HeartRateTimerTask extends TimerTask {
    public void run() {
      hrHandler.post(new Runnable() {
        @Override
        public void run() {
          //////////////////////////////////////////
          // テキストビューに
          // 新たに得られた各軸の加速度から移動平均値を引いた値を表示する
          xTextView.setText("X : " + ave[0]);
          yTextView.setText("Y : " + ave[1]);
          zTextView.setText("Z : " + ave[2]);
          //////////////////////////////////////////

          fftTextView.setText("ピーク周波数：" + (double) maxInd * fs / FFT_SIZE + "\n心拍数：" + (double) maxInd * fs / FFT_SIZE * 60);

          ///////////////////////////////////////////////////////////
          // グラフの描画
          LineData data = mChart.getLineData();
          if (data != null) {
            ILineDataSet set = data.getDataSetByIndex(0);
            if (set == null) {
              set = createSet("heart_rate", Color.BLUE);
              data.addDataSet(set);
            }
            data.addEntry( new Entry( data.getEntryCount(), (float) maxInd * fs / FFT_SIZE * 60 ), 0 );
            data.notifyDataChanged();
          }
          mChart.notifyDataSetChanged();  // 表示の更新のために変更を通知する
          mChart.setVisibleXRangeMaximum(50); // 表示の幅を決定する
          mChart.moveViewToX( data.getEntryCount() ); // 最新のデータまで表示を移動させる
          ///////////////////////////////////////////////////////////
        }
      });
    }
  }

  private LineDataSet createSet(String label, int color) {
    LineDataSet set = new LineDataSet(null, label);
    set.setLineWidth(2.5f); // 線の幅を指定
    set.setColor(color); // 線の色を指定
    set.setDrawCircles(false); // ポイントごとの円を表示しない
    set.setDrawValues(false); // 値を表示しない

    return set;
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

  @Override
  public void onAccuracyChanged(Sensor sensor, int accuracy) {

  }

  @Override
  protected void onResume() {
    super.onResume();
    manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI);
  }

  @Override
  protected void onPause() {
    super.onPause();
    manager.unregisterListener(this);
  }
}