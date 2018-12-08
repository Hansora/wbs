package com.example.biophone;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;


import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.IOError;
import java.lang.annotation.Target;
import java.lang.reflect.Field;

public class MainActivity extends AppCompatActivity {
  TextView err_msgTextView;
  EditText userIdEdit;
  EditText ageEdit;
  RadioGroup radioGroup;
  Button sendBtn;

  String userId;
  int age;
  String gender = null;
  boolean send_data_flag;

  AsyncHttpTask httpTask;
  String data;
  private ProgressDialog progressDialog;

  private String preName = "INITIAL_SETTING";
  private final String dataWakePreTag = "dataWPT";
  private final String dataUserIdPreTag = "dataUIPT";
  private SharedPreferences sharedPreferences;
  private SharedPreferences.Editor editor;
  private int wake;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    // 端末に保存しておく変数（アプリをアンインストールするまで保持する）
    sharedPreferences = getSharedPreferences(preName, MODE_PRIVATE);

    wake = sharedPreferences.getInt(dataWakePreTag, 1);
    userId = sharedPreferences.getString(dataUserIdPreTag, "");

    Log.i("wake", String.valueOf(wake));

    if (wake > 0) {
      // 初回起動時ではない
      // Second Activity へ移動
      Intent second_activity = new Intent(getApplication(), SecondPrimaryActivity.class);
 //     second_activity.putExtra("userId", userId);
      userId = "hogehoge";
      editor = sharedPreferences.edit();
      editor.putString(dataUserIdPreTag, userId).apply();
      startActivity(second_activity);
      finish();
    } else {
      // 初回起動時
      err_msgTextView = findViewById(R.id.err_msg);
      sendBtn = findViewById(R.id.send_btn);

      userIdEdit = findViewById(R.id.user_id);
      ageEdit = findViewById(R.id.age);

      // ラジオボタンの設定
      radioGroup = findViewById(R.id.radioGroup);
      radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
          switch (checkedId) {
            case R.id.male:
              // 男性が選択された
              gender = "male";
              break;

            case R.id.female:
              // 女性が選択された
              gender = "female";
              break;

            default:
              // 何も選択されていない
              gender = "";
              break;
          }
        }
      });
    }
  }

  // 送信ボタンが押された場合の処理
  public void onSendBtnClick(View view) {
    err_msgTextView.setText("");

    // すべて記入されているかを確認
    if ( userIdEdit.getText().toString().length() != 0 && ageEdit.getText().toString().length() != 0 && gender != null ) {
      userId = userIdEdit.getText().toString();
      age = Integer.parseInt( ageEdit.getText().toString() );
      data = "userId=" + userId + "&" + "age=" + String.valueOf(age) + "&" + "gender=" + gender;

      // ユーザ ID, 年齢, 性別を VPS へ送信
      // send_data_flag = 送信処理
      httpTask = new AsyncHttpTask(progressDialog, MainActivity.this);
      httpTask.setOnCallBack( new AsyncHttpTask.CallBackTask() {
        @Override
        public void CallBack(String result) {
          super.CallBack(result);
          // 非同期処理が終わった後の処理
          // result には, doInBackgroud メソッドの戻り値が入る

          Log.i("result", result);

          if ("Connection failed".equals(result)) {
            err_msgTextView.setText("サーバに接続できませんでした。\nネットワークの設定を確認してください。");
          } else {
            String status = "";
            try {
              JSONObject jsonObject = new JSONObject(result);
              status = jsonObject.getString("status");
              userId = jsonObject.getString("userId");
            } catch (Exception e) {
              e.printStackTrace();
            }

            if ("OK".equals(status)) {
              // 送信した際にユーザ ID が重複していなければ次のアクティビティへ移動
              Log.i("status", status);
              Log.i("userId", userId);

              wake = 1;
              editor = sharedPreferences.edit();
              editor.putInt(dataWakePreTag, wake).apply();
              editor.putString(dataUserIdPreTag, userId).apply();



              // SecondActivity へ移動
              Intent second_activity = new Intent(MainActivity.this, SecondPrimaryActivity.class);
              second_activity.putExtra("userId", userId);
              startActivity(second_activity);
              finish();
            } else {
              Log.i("status", status);
              err_msgTextView.setText("記入したユーザ ID は、既に使用されています。");
            }
          }
        }
      });
      httpTask.execute("URLを記入",data);
    } else {
      // err_msg に表示
      err_msgTextView.setText("入力または選択されていない項目があります。");
    }
  }
}
