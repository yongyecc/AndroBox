// IVClient.aidl
package cn.yongye.androbox.client;

// Declare any non-default types here with import statements

import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ProviderInfo;

import cn.yongye.androbox.remote.PendingResultData;

interface IVClient {
    void scheduleReceiver(in String processName,in ComponentName component, in Intent intent, in PendingResultData resultData);
    void scheduleNewIntent(in String creator, in IBinder token, in Intent intent);
    void finishActivity(in IBinder token);
    IBinder createProxyService(in ComponentName component, in IBinder binder);
    IBinder acquireProviderClient(in ProviderInfo info);
    IBinder getAppThread();
    IBinder getToken();
    String getDebugInfo();
}