package com.example.biophone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class SecondActivity extends AppCompatActivity {
  // 表示用のテキストビュー
  TextView hrTextView;

  // START / STOPボタン
  Button button;

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

  // ユーザ ID
  String userId;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_second);

    // MainActivity からユーザ ID を受け取る
    Intent intent = new Intent();
    userId = intent.getStringExtra("userId");

    // TextViewの取得
    hrTextView = findViewById(R.id.heart_rate);

    // Buttonの取得
    button = findViewById(R.id.button);

    // レシーバの設定
    receiver = new MyBroadcastReceiver();
    intentFilter = new IntentFilter();
    intentFilter.addAction(serviceTAG);
    registerReceiver(receiver, intentFilter);

    // 端末に保存しておく変数（アプリをアンインストールするまで保持する）
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

    try {
      unregisterReceiver(receiver);
    } catch(IllegalArgumentException e) {
      e.printStackTrace();
    }
  }

  // ボタンが押された時の処理
  public void onClick(View view) {
    editor = sharedPreferences.edit();
    if (flag) {
      flag = false;
      editor.putBoolean(dataFlagPreTag, flag).apply();

      button.setText("STOP");
      hrTextView.setText("計測中...");

      try {
        registerReceiver(receiver, intentFilter);
      } catch(IllegalArgumentException e) {
        e.printStackTrace();
      }

      Intent i = new Intent(SecondActivity.this, HeartRateService.class);
      i.putExtra("userId", userId);           // ユーザ ID をサービスに送る
      // 端末の OS バージョンによって処理を変更
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        startForegroundService(i);  // API 26 以上（Android OS 8.0 以上）
      } else {
        startService(i);
      }
    } else {
      flag = true;
      editor.putBoolean(dataFlagPreTag, flag).apply();

      try {
        unregisterReceiver(receiver);
      } catch(IllegalArgumentException e) {
        e.printStackTrace();
      }

      button.setText("START");
      hrTextView.setText("計測を開始するには START ボタンを\nタッチしてください");

      Intent i = new Intent(SecondActivity.this, HeartRateService.class);
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
    }
  }
}
