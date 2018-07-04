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

import java.util.Timer;
import java.util.TimerTask;

import uk.me.berndporr.iirj.Butterworth;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
  SensorManager manager;
  Sensor sensor;

  TextView xTextView;
  TextView yTextView;
  TextView zTextView;

  // 加速度用の変数
  // 端末が実際に取得した加速度値
  private float[] currentOrientationValues = { 0.0f, 0.0f, 0.0f };

  // ローパス、ハイパスフィルタ後の加速度値
  private float[] currentAccelerationValues = { 0.0f, 0.0f, 0.0f };

  // 各加速度の配列
  private float[] xValue = new float[15];
  private float[] yValue = new float[15];
  private float[] zValue = new float[15];

  // 平方和の配列
  private float sumXYZ;

  // パルス波形データの配列
  private float[] pulseWave = new float[1000];

  // フィルタ後の各加速度の配列
  private float xBandValue;
  private float yBandValue;
  private float zBandValue;

  private float x, y, z;

  // 生データの個数をカウントする
  private int raw_count = 0;

  // パルス波形のデータをカウントする
  private int pulseWaveCnt = 0;

  // タイマー用の変数
  private Timer mainTimer;					//タイマー用
  private MainTimerTask mainTimerTask;		//タイマタスククラス
  private Handler mHandler = new Handler();   //UI Threadへのpost用ハンドラ

  // グラフ用の変数
  LineChart mChart;
  String[] names = new String[]{"x-value", "y-value", "z-value"};
  int[] colors = new int[]{Color.RED, Color.GREEN, Color.BLUE};

  Button button = null;
  private boolean flag = true;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    // TextViewの取得
    xTextView = (TextView)findViewById(R.id.xValue);
    yTextView = (TextView)findViewById(R.id.yValue);
    zTextView = (TextView)findViewById(R.id.zValue);

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

    // ボタンを押したとき
    button.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        // flag が true の時
        if (flag){
          button.setText("STOP");

          //タイマーインスタンス生成
          mainTimer = new Timer();

          //タスククラスインスタンス生成
          mainTimerTask = new MainTimerTask();

          // タイマースケジュール設定＆開始 mainTimer.schedule(new MainTimerTask(), long delay, long period)
          // delay: はじめのタスクが実行されるまでの時間（単位はミリ秒），period: タスクが実行される周期（単位はミリ秒）
          mainTimer.schedule(mainTimerTask, 100, 10);

          flag = false;
        }
        // flag が false の時
        else {
          button.setText("START");

          // 実行中のタイマー処理を終了できるタイミングまで処理を行い、以降処理を再開させない
          mainTimer.cancel();
          flag = true;
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
          // TextViewに各加速度センサーの値を表示させる
          /*
          xTextView.setText(String.valueOf(currentAccelerationValues[0]));
          yTextView.setText(String.valueOf(currentAccelerationValues[1]));
          zTextView.setText(String.valueOf(currentAccelerationValues[2]));
          */

          if (raw_count < 15) {
            // 各加速度の値を更新
            xValue[raw_count] = x;
            yValue[raw_count] = y;
            zValue[raw_count] = z;
            raw_count++;
          } else {
            // 各軸の加速度の平均値を求める
            float[] ave = {0.0f, 0.0f, 0.0f};
            for (int i = 0; i < raw_count; i++) {
              ave[0] += xValue[i];
              ave[1] += yValue[i];
              ave[2] += zValue[i];
            }
            ave[0] /= raw_count;
            ave[1] /= raw_count;
            ave[2] /= raw_count;

            // 各軸の加速度の値を更新
            for (int i = 0; i < raw_count - 1; i++) {
              xValue[i] = xValue[i + 1];
              yValue[i] = yValue[i + 1];
              zValue[i] = zValue[i + 1];
            }
            xValue[raw_count - 1] = x;
            yValue[raw_count - 1] = y;
            zValue[raw_count - 1] = z;

            xTextView.setText("X : " + String.valueOf(xValue[raw_count-1]) + " - " + String.valueOf(ave[0]) + " = " + String.valueOf(xValue[raw_count-1] - ave[0]));
            yTextView.setText("Y : " + String.valueOf(yValue[raw_count-1]) + " - " + String.valueOf(ave[1]) + " = " + String.valueOf(yValue[raw_count-1] - ave[1]));
            zTextView.setText("Z : " + String.valueOf(zValue[raw_count-1]) + " - " + String.valueOf(ave[2]) + " = " + String.valueOf(zValue[raw_count-1] - ave[2]));

            // 各軸の加速度値から各軸の移動平均値を引く
            ave[0] = xValue[raw_count-1] - ave[0];
            ave[1] = yValue[raw_count-1] - ave[1];
            ave[2] = zValue[raw_count-1] - ave[2];

            ////////////////////////////////////////////////////////////
            // バターワース型バンドパスフィルタのインスタンス生成
            Butterworth butterworth = new Butterworth();
            butterworth.bandPass(1, 100, 10, 6);  // 7-13Hzを通すバンドパスフィルタ
            ////////////////////////////////////////////////////////////

            ////////////////////////////////////////////////////////////
            // 7-13Hzを通すバターワース型バンドパスフィルタをかける
            xBandValue = (float) butterworth.filter(ave[0]);
            yBandValue = (float) butterworth.filter(ave[1]);
            zBandValue = (float) butterworth.filter(ave[2]);
            ////////////////////////////////////////////////////////////

            ////////////////////////////////////////////////////////////
            // 各軸の2乗の和の平方根を求める
            sumXYZ = (float) Math.sqrt( Math.pow(xBandValue, 2) + Math.pow(yBandValue, 2) + Math.pow(zBandValue, 2) );
            ////////////////////////////////////////////////////////////

            ////////////////////////////////////////////////////////////
            // 0.66-2.5Hzを通すバターワース型バンドパスフィルタをかける
            butterworth.bandPass(1, 100, 1.58, 1.84);  // 0.66-2.5Hzを通すバンドパスフィルタ
            if (pulseWaveCnt < 1000) {
              pulseWave[pulseWaveCnt] = (float) butterworth.filter(sumXYZ);
              pulseWaveCnt++;
            } else {
              ////////////////////////////////////////
              // FFTを行う
              ////////////////////////////////////////

              for (int i = 0; i < pulseWaveCnt - 1; i++) {
                pulseWave[i] = pulseWave[i + 1];
              }
              pulseWave[pulseWaveCnt - 1] = (float) butterworth.filter(sumXYZ);
            }
            ////////////////////////////////////////////////////////////

            ////////////////////////////////////////////////////////////
            // FFTを行う

            // グラフの描画
            LineData data = mChart.getLineData();
            if (data != null) {
              for (int i = 0; i < 3; i++) { // 3軸なのでそれぞれ処理します
                ILineDataSet set = data.getDataSetByIndex(i);
                if (set == null) {
                  set = createSet(names[i], colors[i]); // ILineDataSetの初期化は別メソッドにまとめました
                  data.addDataSet(set);
                }
                //data.addEntry(new Entry(set.getEntryCount(), currentAccelerationValues[i]), i); // 実際にデータを追加する
                data.addEntry(new Entry(set.getEntryCount(), ave[i]), i); // 実際にデータを追加する
                data.notifyDataChanged();
              }
              mChart.notifyDataSetChanged(); // 表示の更新のために変更を通知する
              mChart.setVisibleXRangeMaximum(50); // 表示の幅を決定する
              mChart.moveViewToX(data.getEntryCount()); // 最新のデータまで表示を移動させる
            }
          }
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
      // ローパスフィルタで重力値を抽出
      /*
      currentOrientationValues[0] = event.values[0] * 0.1f + currentOrientationValues[0] * (1.0f - 0.1f);
      currentOrientationValues[1] = event.values[1] * 0.1f + currentOrientationValues[1] * (1.0f - 0.1f);
      currentOrientationValues[2] = event.values[2] * 0.1f + currentOrientationValues[2] * (1.0f - 0.1f);

      // 重力値を取り除く
      currentAccelerationValues[0] = event.values[0] - currentOrientationValues[0];
      currentAccelerationValues[1] = event.values[1] - currentOrientationValues[1];
      currentAccelerationValues[2] = event.values[2] - currentOrientationValues[2];
      */

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