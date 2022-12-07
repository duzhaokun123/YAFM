package io.github.duzhaokun123.yafm.utils;

import android.content.ContentValues;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class Http {
    public static void getData(String link, Handler handler) {
        byte[] res = null;
        try {
            URL url = new URL(link);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5 * 1000);
            int resCode = conn.getResponseCode();
            if (resCode != 200) {
                Log.i(ContentValues.TAG, "异常HTTP返回码[" + resCode + "]");
                return;
            }

            InputStream is = conn.getInputStream();
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            int len;
            byte[] buffer = new byte[204800];  //200kb
            while ((len = is.read(buffer)) != -1)
                os.write(buffer, 0, len);

            is.close();
            os.close();
            res = os.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            Log.i(ContentValues.TAG, "Network IO异常");
        }
        if (res == null)
            return;

        Message msg = new Message();
        Bundle data = new Bundle();
        data.putByteArray("response", res);
        msg.setData(data);
        handler.sendMessage(msg);
    }
}
