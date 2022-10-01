package cn.yongye.helloworld;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class MyBroadcastReceiver extends BroadcastReceiver {

    static String TAG = MyBroadcastReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "[helloworld][Called] MyBroadcastReceiver.onReceive");
        Toast.makeText(context, String.format("收到广播: %s", intent.getStringExtra("msg")),
                Toast.LENGTH_SHORT).show();
    }
}
