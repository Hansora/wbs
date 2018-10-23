package com.example.biophone;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.util.StringBuilderPrinter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class AsyncHttpTask extends AsyncTask<String, Integer, String> {
  private CallBackTask callBackTask;
  private ProgressDialog progressDialog;
  private Activity activity;

  // コンストラクタ
  public AsyncHttpTask(ProgressDialog progressDialog, Activity activity) {
    super();
    this.progressDialog = progressDialog;
    this.activity = activity;
  }

  @Override
  // バックグラウンドで処理を始める前に行う処理
  protected void onPreExecute() {
    progressDialog = new ProgressDialog(this.activity);
    progressDialog.setMessage("送信中...");
    progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
    progressDialog.setCancelable(false);
    progressDialog.show();
  }

  @Override
  protected String doInBackground(String... params) {
    HttpURLConnection httpCon = null;
    StringBuilder stringBuilder = new StringBuilder();
    String result = null;

    try {
      URL url = new URL(params[0]);
      httpCon = (HttpURLConnection) url.openConnection();

      // HTTP リクエストメソッドの設定（POST）
      httpCon.setRequestMethod("POST");

      // リダイレクト拒否
      httpCon.setInstanceFollowRedirects(false);

      // データを書き込む
      httpCon.setDoOutput(true);

      // 時間制限
      httpCon.setReadTimeout(10000);
      httpCon.setConnectTimeout(10000);

      // 接続
      httpCon.connect();

      // POST データ送信処理
      OutputStream outputStream = null;
      try {
        outputStream = httpCon.getOutputStream();
        outputStream.write( params[1].getBytes("UTF-8") );
        outputStream.flush();
      } catch (IOException e) {
        e.printStackTrace();
        result = "POST error";
      } finally {
        if (outputStream != null) {
          outputStream.close();
        }
      }

      final int status = httpCon.getResponseCode();
      if (status == HttpURLConnection.HTTP_OK) {
        // レスポンスを受け取る
        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(httpCon.getInputStream(), "UTF-8"));
        String line;
        while ( ( line = bufferedReader.readLine() ) != null ) {
          stringBuilder.append(line);
        }
        result = stringBuilder.toString();
      } else {
        result = "status" + String.valueOf(status);
      }
    } catch (IOException e) {
      e.printStackTrace();
      result = "Connection failed";
    } finally {
      if (httpCon != null) {
        httpCon.disconnect();
      }
    }
    return result;
  }

  // 進捗状況をレイアウトに反映するための処理
  @Override
  protected void onProgressUpdate(Integer... values) {
    Log.i("url", values[0].toString());
  }

  // doInBackground メソッドの事後処理
  @Override
  protected void onPostExecute(String result){
    super.onPostExecute(result);

    // プログレスダイアログを閉じる
    if (progressDialog != null && progressDialog.isShowing()) {
      progressDialog.dismiss();
    }
    callBackTask.CallBack(result);
  }

  // doInBackground メソッドの処理をキャンセルした時の処理
  @Override
  protected void onCancelled() {
    // プログレスダイアログを閉じる
    if (progressDialog != null && progressDialog.isShowing()) {
      progressDialog.dismiss();
    }
  }

  public void setOnCallBack(CallBackTask cbj) {
    callBackTask = cbj;
  }

  /*
   * コールバック用のクラス
   */
  public static class CallBackTask {
    public void CallBack(String result) {

    }
  }
}
