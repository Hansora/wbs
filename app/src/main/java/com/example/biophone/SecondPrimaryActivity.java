package com.example.biophone;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.view.View;
import android.app.Activity;

//wi-fiのライブラリー
import android.app.ListActivity;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.app.Activity;
import android.util.Log;
import android.net.wifi.ScanResult;
import android.widget.ArrayAdapter;

import java.util.List;

import android.os.Build;
import android.widget.TextView;
import android.widget.Toast;


import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.MqttException;

import static com.example.biophone.R.styleable.View;



public class SecondPrimaryActivity extends AppCompatActivity {
    TextView sw1;
    Button button;

    String ssid;
    String userId;

    private String preName = "INITIAL_SETTING";
    private final String dataUserIdPreTag = "dataUIPT";
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private Button button_segue;

//    private final String serviceTAG = "WifiSsidService";

    // MQTT 関連
    private MqttAndroidClient mqttAndroidClient;
    private final String URL = "tcp://rdlab.dip.jp:1883";


    @Override
    public void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_second_primary);

        sw1 = (TextView) findViewById(R.id.textView3);

        button_segue = (Button) findViewById(R.id.button_segue);

        button = (Button) findViewById(R.id.button3);


        SharedPreferences preferences= getSharedPreferences(preName, Context.MODE_PRIVATE);
        String userId = preferences.getString(dataUserIdPreTag, "");

        userId = preferences.getString(dataUserIdPreTag, "");


        WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        WifiInfo w_info = wifiManager.getConnectionInfo();


        //WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        //  WifiInfo w_info = wifiManager.getConnectionInfo();

        //　SSIDを取得
        ssid = String.valueOf(w_info.getSSID());

        //sw1.setText("ssid: " + ssid);
        // Log.i("Sample", "SSID:" + ssid);

        try {
            if (ssid != null) {
                sw1.setText(ssid);
                // Log.i("Sample", "SSID:" + ssid);
            } else {
                sw1.setText("ssid: " + ssid);
                Log.i("Sample", "Error");
                throw new NullPointerException();
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        // MQTT のインスタンス
        //    mqttAndroidClient  = new MqttAndroidClient(HeartRateService.this, URL, "");

        // MQTT ブローカーに接続
        //   try {
        // MQTT ブローカーに接続されていない場合
        //    if (!mqttAndroidClient.isConnected()) {
        //         mqttAndroidClient.connect();  // 接続
        //  }
        //} catch (MqttException e) {
        //  e.printStackTrace();
        // }


        // Intent i = new Intent(SecondPrimaryActivityActivity.this, WifiSsidService.class);

        // 端末の OS バージョンによって処理を変更
        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        //      startForegroundService(i);  // API 26 以上（Android OS 8.0 以上）
        //    } else {
        //          startService(i);
//        }

    }

    public void ssidBtn(View v) {
        Intent intent =  getIntent();
       // userId = sharedPreferences.getString(dataUserIdPreTag, "");
        userId = intent.getStringExtra("userId");
        //editor = sharedPreferences.edit();
        //editor.putString(dataUserIdPreTag, "17rmd21").apply();

       // editor.putString(dataUserIdPreTag, userI).apply();
        //Log.i("userId", userId);
    }


    public void onHeartrateClick(View v) {
        Intent intent = new Intent(getApplication(), SecondActivity.class);
        startActivity(intent);
    }

    public void onLightClick(View v) {
        Intent intent = new Intent(getApplication(), LightingActivty.class);
        intent.putExtra("ssid", ssid);
        startActivity(intent);

    }

    public void onAirconditioningClick(View v) {
        Intent intent = new Intent(getApplication(), AirconditioningActivity.class);
        startActivity(intent);
    }

}

