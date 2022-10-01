package cn.yongye.androbox.os;

import android.os.Binder;

import cn.yongye.androbox.client.ipc.VActivityManager;

public class VBinder {

    public static int getCallingUid() {
        return VActivityManager.get().getUidByPid(Binder.getCallingPid());
    }

    public static int getBaseCallingUid() {
        return VUserHandle.getAppId(getCallingUid());
    }

    public static int getCallingPid() {
        return Binder.getCallingPid();
    }

    public static VUserHandle getCallingUserHandle() {
        return new VUserHandle(VUserHandle.getUserId(getCallingUid()));
    }
}
