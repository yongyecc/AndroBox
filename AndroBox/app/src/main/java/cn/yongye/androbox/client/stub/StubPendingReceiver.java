package cn.yongye.androbox.client.stub;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import cn.yongye.androbox.helper.utils.ComponentUtils;
import cn.yongye.androbox.os.VUserHandle;

public class StubPendingReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent realIntent = intent.getParcelableExtra("_VA_|_intent_");
        int userId = intent.getIntExtra("_VA_|_user_id_", VUserHandle.USER_ALL);
        if (realIntent != null) {
            Intent newIntent = ComponentUtils.redirectBroadcastIntent(realIntent, userId);
            if (newIntent != null) {
                context.sendBroadcast(newIntent);
            }
        }
    }
}
