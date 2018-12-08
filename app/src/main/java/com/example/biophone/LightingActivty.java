package com.example.biophone;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;

import android.view.View;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;


public class LightingActivty extends AppCompatActivity {
    SeekBar seekBar;
    TextView bri1;
    TextView color;
    String userId;
    String ssid;


    //image buttonの変数
    private TextView textButton;
    private boolean flg = true;

    // MQTT 関連
    private MqttAndroidClient mqttAndroidClient;
    private final String URL = "tcp://rdlab.dip.jp:1883";

    private String preName = "INITIAL_SETTING";
    private final String dataUserIdPreTag = "dataUIPT";
    private SharedPreferences sharedPreferences;
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lighting_activty);
        // seek barの設定
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        bri1 = (TextView) findViewById(R.id.TextView01);

        color = (TextView) findViewById(R.id.textView2);

        // 端末に保存しておく変数（アプリをアンインストールするまで保持する）
        sharedPreferences = getSharedPreferences(preName, MODE_PRIVATE);
        userId = sharedPreferences.getString(dataUserIdPreTag, "");

        final Intent intent = getIntent();
        ssid= intent.getStringExtra("ssid");

        // MQTT のインスタンス
        mqttAndroidClient  = new MqttAndroidClient(LightingActivty.this, URL, "");

        // MQTT ブローカーに接続
        try {
            // MQTT ブローカーに接続されていない場合
            if (!mqttAndroidClient.isConnected()) {

                IMqttToken token = mqttAndroidClient.connect();  // 接続
                token.setActionCallback(new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken iMqttToken) {
                        Log.d("Succes", "connection");
                    }


                    @Override
                    public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
                        Log.d("failure","connection");
                    }
                });
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }


        // シークバーの初期値をTextViewに表示
        bri1.setText("Current Value:" + seekBar.getProgress());

        seekBar.setOnSeekBarChangeListener(
                new OnSeekBarChangeListener() {

                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        // ツマミをドラッグしたときに呼ばれる
                        bri1.setText("Current Value:" + progress);
                    }

                    public void onStartTrackingTouch(SeekBar seekBar) {
                        // ツマミに触れたときに呼ばれる
                    }

                    public void onStopTrackingTouch(SeekBar seekBar) {
                        // ツマミを離したときに呼ばれる
                    }

                });
    }

    public void onClick(View v){

            switch (v.getId()) {
                case R.id.red:
                    // クリック処理
                    color.setText("red");

                    break;


                case R.id.green:
                    color.setText("green");
                    break;

                case R.id.blue:
                    color.setText("blue");
                    break;

                case R.id.white:
                    // クリック処理
                    color.setText("white");

                    break;


                case R.id.purple:
                    color.setText("purple");
                    break;

                case R.id.orange:
                    color.setText("orange");
                    break;

                default:
                    break;
            }

    }


    public void send(View v){
        // MQTT で VPS にデータを送信
        Log.d("Color", String.valueOf(color.getText()));
        String message = userId + ',' + String.valueOf(color.getText()) + ',' + String.valueOf(seekBar.getProgress()) + ',' + ssid ;
        Log.d("Message", message);
        try {
            mqttAndroidClient.publish("topic/lighting", message.getBytes(), 0, false);
        } catch (MqttPersistenceException e) {
            e.printStackTrace();
        } catch (MqttException e) {
            e.printStackTrace();
        }
        ////////////////////////////////////////////////////////////
    }
}




