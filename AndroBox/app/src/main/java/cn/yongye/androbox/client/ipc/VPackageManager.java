package cn.yongye.androbox.client.ipc;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.ResolveInfo;
import android.os.RemoteException;

import cn.yongye.androbox.client.env.VirtualRuntime;
import cn.yongye.androbox.helper.ipcbus.IPCSingleton;
import cn.yongye.androbox.virtual.server.interfaces.IPackageManager;

public class VPackageManager {

    private static final VPackageManager sMgr = new VPackageManager();
    private IPCSingleton<IPackageManager> singleton = new IPCSingleton<>(IPackageManager.class);

    public static VPackageManager get() {
        return sMgr;
    }

    public ApplicationInfo getApplicationInfo(String packageName, int flags, int userId) {
        try {
            return getService().getApplicationInfo(packageName, flags, userId);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public IPackageManager getService() {
        return singleton.get();
    }

    public PackageInfo getPackageInfo(String packageName, int flags, int userId) {
        try {
            return getService().getPackageInfo(packageName, flags, userId);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public ActivityInfo getActivityInfo(ComponentName componentName, int flags, int userId) {
        try {
            return getService().getActivityInfo(componentName, flags, userId);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public ResolveInfo resolveIntent(Intent intent, String resolvedType, int flags, int userId) {
        try {
            return getService().resolveIntent(intent, resolvedType, flags, userId);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    public ResolveInfo resolveService(Intent intent, String resolvedType, int flags, int userId) {
        try {
            return getService().resolveService(intent, resolvedType, flags, userId);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }
}
