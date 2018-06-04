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

  private float[] xValue = { 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f };
  private float[] yValue = { 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f };
  private float[] zValue = { 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f };

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

  private int raw_count = 0;

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
          xTextView.setText(String.valueOf(currentAccelerationValues[0]));
          yTextView.setText(String.valueOf(currentAccelerationValues[1]));
          zTextView.setText(String.valueOf(currentAccelerationValues[2]));

          LineData data = mChart.getLineData();
          if (data != null){
              for (int i = 0; i < 3; i++) { // 3軸なのでそれぞれ処理します
                  ILineDataSet set = data.getDataSetByIndex(i);
                  if (set == null) {
                      set = createSet(names[i], colors[i]); // ILineDataSetの初期化は別メソッドにまとめました
                      data.addDataSet(set);
                  }
                  data.addEntry(new Entry(set.getEntryCount(), currentAccelerationValues[i]), i); // 実際にデータを追加する
                  data.notifyDataChanged();
              }
              mChart.notifyDataSetChanged(); // 表示の更新のために変更を通知する
              mChart.setVisibleXRangeMaximum(50); // 表示の幅を決定する
              mChart.moveViewToX(data.getEntryCount()); // 最新のデータまで表示を移動させる
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
      currentOrientationValues[0] = event.values[0] * 0.1f + currentOrientationValues[0] * (1.0f - 0.1f);
      currentOrientationValues[1] = event.values[1] * 0.1f + currentOrientationValues[1] * (1.0f - 0.1f);
      currentOrientationValues[2] = event.values[2] * 0.1f + currentOrientationValues[2] * (1.0f - 0.1f);

      // 重力値を取り除く
      currentAccelerationValues[0] = event.values[0] - currentOrientationValues[0];
      currentAccelerationValues[1] = event.values[1] - currentOrientationValues[1];
      currentAccelerationValues[2] = event.values[2] - currentOrientationValues[2];
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