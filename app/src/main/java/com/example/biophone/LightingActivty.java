package com.example.biophone;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;



public class LightingActivty extends AppCompatActivity {
    SeekBar seekBar;
    TextView tv1;

    //image buttonの変数
    private TextView textButton;
    private boolean flg = true:

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lighting_activty);
    // seek barの設定
        seekBar = (SeekBar)findViewById(R.id.seekBar);
        tv1 = (TextView)findViewById(R.id.TextView01);

        //image button の設定
        ImageButton imageButton = findViewById(R.id.green):
        ImageButton imageButton = findViewById(R.id.red):
        ImageButton imageButton = findViewById(R.id.blue):
        ImageButton imageButton = findViewById(R.id.purple):
        ImageButton imageButton = findViewById(R.id.white):
        ImageButton imageButton = findViewById(R.id.orange):


        // シークバーの初期値をTextViewに表示
        tv1.setText("Current Value:"+seekBar.getProgress());

        seekBar.setOnSeekBarChangeListener(
                new OnSeekBarChangeListener() {
                    public void onProgressChanged(SeekBar seekBar,int progress, boolean fromUser) {
                        // ツマミをドラッグしたときに呼ばれる
                        tv1.setText("Current Value:"+progress);
                    }

                    public void onStartTrackingTouch(SeekBar seekBar) {
                        // ツマミに触れたときに呼ばれる
                    }

                    public void onStopTrackingTouch(SeekBar seekBar) {
                        // ツマミを離したときに呼ばれる
                    }
                }
        );
    }
}