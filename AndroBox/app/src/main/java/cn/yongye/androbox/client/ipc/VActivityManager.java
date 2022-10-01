package cn.yongye.androbox.client.ipc;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;


import java.util.HashMap;
import java.util.Map;

import cn.yongye.androbox.VirtualCore;
import cn.yongye.androbox.client.env.VirtualRuntime;
import cn.yongye.androbox.helper.compat.ActivityManagerCompat;
import cn.yongye.androbox.helper.ipcbus.IPCSingleton;
import cn.yongye.androbox.os.VUserHandle;
import cn.yongye.androbox.remote.PendingResultData;
import cn.yongye.androbox.virtual.server.interfaces.IActivityManager;
import mirror.android.app.ActivityThread;

public class VActivityManager {

    private static final VActivityManager sAM = new VActivityManager();
    private final Map<IBinder, ActivityClientRecord> mActivities = new HashMap<IBinder, ActivityClientRecord>(6);
    private IPCSingleton<IActivityManager> singleton = new IPCSingleton<>(IActivityManager.class);

    public static VActivityManager get() {
        return sAM;
    }

    public void sendActivityResult(IBinder resultTo, String resultWho, int requestCode) {
        ActivityClientRecord r = mActivities.get(resultTo);
        if (r != null && r.activity != null) {
            Object mainThread = VirtualCore.mainThread();
            ActivityThread.sendActivityResult.call(mainThread, resultTo, resultWho, requestCode, 0, null);
        }
    }

    public void processRestarted(String packageName, String processName, int userId) {
        try {
            getService().processRestarted(packageName, processName, userId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public ActivityClientRecord onActivityCreate(ComponentName component, ComponentName caller, IBinder token, ActivityInfo info, Intent intent, String affinity, int taskId, int launchMode, int flags) {
        ActivityClientRecord r = new ActivityClientRecord();
        r.info = info;
        mActivities.put(token, r);
        try {
            getService().onActivityCreated(component, caller, token, intent, affinity, taskId, launchMode, flags);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return r;
    }

    public ComponentName getActivityForToken(IBinder token) {
        try {
            return getService().getActivityClassForToken(VUserHandle.myUserId(), token);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public void finishActivity(IBinder token) {
        ActivityClientRecord r = getActivityRecord(token);
        if (r != null) {
            Activity activity = r.activity;
            while (true) {
                // We shouldn't use Activity.getParent(),
                // because It may be overwritten.
                Activity parent = mirror.android.app.Activity.mParent.get(activity);
                if (parent == null) {
                    break;
                }
                activity = parent;
            }
            // We shouldn't use Activity.isFinishing(),
            // because It may be overwritten.
            if (!mirror.android.app.Activity.mFinished.get(activity)) {
                int resultCode = mirror.android.app.Activity.mResultCode.get(activity);
                Intent resultData = mirror.android.app.Activity.mResultData.get(activity);
                ActivityManagerCompat.finishActivity(token, resultCode, resultData);
                mirror.android.app.Activity.mFinished.set(activity, true);
            }
        }
    }

    public ActivityClientRecord getActivityRecord(IBinder token) {
        synchronized (mActivities) {
            return token == null ? null : mActivities.get(token);
        }
    }

    public boolean isAppProcess(String processName) {
//        try {
//            return getService().isAppProcess(processName);
//        } catch (RemoteException e) {
//            return VirtualRuntime.crash(e);
//        }
        return true;
    }

    public IActivityManager getService() {
        return singleton.get();
    }


    public int startActivity(Intent intent, ActivityInfo info, IBinder resultTo, Bundle options, String resultWho, int requestCode, int userId) {
        try {
            return getService().startActivity(intent, info, resultTo, options, resultWho, requestCode, userId);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public int startActivity(Intent intent, int userId) {
        if (userId < 0) {
            return ActivityManagerCompat.START_NOT_CURRENT_USER_ACTIVITY;
        }
        ActivityInfo info = VirtualCore.get().resolveActivityInfo(intent, userId);
        if (info == null) {
            return ActivityManagerCompat.START_INTENT_NOT_RESOLVED;
        }
        return startActivity(intent, info, null, null, null, 0, userId);
    }

    public int getUidByPid(int pid) {
        try {
            return getService().getUidByPid(pid);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public void broadcastFinish(PendingResultData res) {
        try {
            getService().broadcastFinish(res);
        } catch (RemoteException e) {
            VirtualRuntime.crash(e);
        }
    }

    public ComponentName startService(IInterface caller, Intent service, String resolvedType, int userId) {
        try {
            return getService().startService(caller != null ? caller.asBinder() : null, service, resolvedType, userId);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }
}
