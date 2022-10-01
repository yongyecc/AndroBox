package cn.yongye.androbox.virtual.server.interfaces;

import android.content.*;
import android.content.pm.*;
import android.os.*;

import cn.yongye.androbox.remote.PendingResultData;

public interface IActivityManager {
    int startActivity(Intent intent, ActivityInfo info, IBinder resultTo, Bundle options, String resultWho, int requestCode, int userId) throws RemoteException;

    ComponentName startService(IBinder caller, Intent service, String resolvedType, int userId) throws RemoteException;

    int getUidByPid(int pid) throws RemoteException;

    void broadcastFinish(PendingResultData res) throws RemoteException;

    ComponentName getActivityClassForToken(int userId, IBinder token) throws RemoteException;

    void processRestarted(String packageName, String processName, int userId) throws RemoteException;

    void onActivityCreated(ComponentName component, ComponentName caller, IBinder token, Intent intent, String affinity, int taskId, int launchMode, int flags) throws RemoteException;
}
