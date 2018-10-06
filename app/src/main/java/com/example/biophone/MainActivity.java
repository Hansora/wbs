package com.example.biophone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

public class MainActivity extends AppCompatActivity {
  // 表示用のテキストビュー
  TextView hrTextView;

  // START / STOPボタン
  Button button;

  // グラフ用の変数
  LineChart mChart;

  MyBroadcastReceiver receiver;
  IntentFilter intentFilter;

  private final String preName = "MAIN_SETTING";

  private final String dataFlagPreTag = "dataFPT";
  private SharedPreferences sharedPreferences;
  private SharedPreferences.Editor editor;

  private boolean flag;

  private final String serviceTAG = "HeartRateService";

  // 通知関連
  protected static final int ACTIVITY_ID = 0;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    // TextViewの取得
    hrTextView = (TextView) findViewById(R.id.heart_rate);

    // グラフ関連
    mChart = (LineChart) findViewById(R.id.lineChart);  // LineChartの取得
    mChart.setDescription("");                           // 表のタイトルを空にする
    mChart.setData(new LineData());                      // 空のLineData型インスタンスを追加

    // Buttonの取得
    button = (Button) findViewById(R.id.button);

    receiver = new MyBroadcastReceiver();
    intentFilter = new IntentFilter();
    intentFilter.addAction(serviceTAG);
    /*registerReceiver(receiver, intentFilter);*/

    sharedPreferences = getSharedPreferences(preName, MODE_PRIVATE);

    flag = sharedPreferences.getBoolean(dataFlagPreTag, true);

    if (flag) {
      hrTextView.setText("計測を開始するには START ボタンを\nタッチしてください");
      button.setText("START");
    } else {
      hrTextView.setText("計測中");
      button.setText("STOP");
    }
  }

  public void onPause() {
    super.onPause();

    // ブロードキャストレシーバーを解除する
    unregisterReceiver(receiver);
  }

  // ボタンが押された時の処理
  public void onClick(View view) {
    editor = sharedPreferences.edit();
    if (flag) {
      flag = false;
      editor.putBoolean(dataFlagPreTag, flag).apply();

      button.setText("STOP");
      hrTextView.setText("計測中...");

      registerReceiver(receiver, intentFilter);

      Intent i = new Intent(MainActivity.this, HeartRateService.class);
      // 端末の OS バージョンによって処理を変更
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        startForegroundService(i);  // API 26 以上（Android OS 8.0 以上）
      } else {
        startService(i);
      }
    } else {
      flag = true;
      editor.putBoolean(dataFlagPreTag, flag).apply();

      unregisterReceiver(receiver);

      button.setText("START");
      hrTextView.setText("計測を開始するには START ボタンを\nタッチしてください");

      Intent i = new Intent(MainActivity.this, HeartRateService.class);
      stopService(i);
    }
  }

  public class MyBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
      // ブロードキャスト受信時の処理
      Bundle bundle = intent.getExtras();
      int heart_rate = bundle.getInt("heart_rate");

      // テキストビューに心拍数を表示
      hrTextView.setText("心拍数 : " + String.valueOf(heart_rate) + "bpm");

      ///////////////////////////////////////////////////////////
      // グラフの描画
      LineData data = mChart.getLineData();
      if (data != null) {
        ILineDataSet set = data.getDataSetByIndex(0);
        if (set == null) {
          set = createSet("heart_rate", Color.BLUE);
          data.addDataSet(set);
        }
        data.addEntry(new Entry(data.getEntryCount(), (float) heart_rate), 0);
        data.notifyDataChanged();
      }
      mChart.notifyDataSetChanged();  // 表示の更新のために変更を通知する
      mChart.setVisibleXRangeMaximum(50); // 表示の幅を決定する
      mChart.moveViewToX(data.getEntryCount()); // 最新のデータまで表示を移動させる
      ///////////////////////////////////////////////////////////
    }
  }

  // グラフの描画処理
  private LineDataSet createSet(String label, int color) {
    LineDataSet set = new LineDataSet(null, label);
    set.setLineWidth(2.5f); // 線の幅を指定
    set.setColor(color); // 線の色を指定
    set.setDrawCircles(false); // ポイントごとの円を表示しない
    set.setDrawValues(false); // 値を表示しない

    return set;
  }
}
