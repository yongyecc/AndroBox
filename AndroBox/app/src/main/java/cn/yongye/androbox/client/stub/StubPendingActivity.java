package cn.yongye.androbox.client.stub;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import cn.yongye.androbox.client.ipc.VActivityManager;
import cn.yongye.androbox.remote.StubActivityRecord;


public class StubPendingActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        finish();
        Intent intent = getIntent();
        StubActivityRecord r = new StubActivityRecord(intent);
        if (r.intent == null) {
            return;
        }
        r.intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
        VActivityManager.get().startActivity(r.intent, r.userId);
    }
}

