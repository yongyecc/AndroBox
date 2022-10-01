package cn.yongye.helloworld;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class MyBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Toast.makeText(context, String.format("收到广播: %s", intent.getStringExtra("msg")),
                Toast.LENGTH_SHORT).show();
    }
}
