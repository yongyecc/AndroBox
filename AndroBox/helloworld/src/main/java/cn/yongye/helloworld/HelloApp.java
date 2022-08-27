package cn.yongye.helloworld;

import android.app.Application;
import android.content.Context;
import android.util.Log;

public class HelloApp extends Application {

    public static String TAG = "helloworld";

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        Log.d(TAG, "[HelloApp] attachBaseContext called.");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "[HelloApp] onCreate called.");
    }
}
