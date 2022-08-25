package cn.yongye.androbox.client.ipc;

import android.content.pm.PackageInfo;
import android.os.RemoteException;

import cn.yongye.androbox.client.env.VirtualRuntime;
import cn.yongye.androbox.helper.ipcbus.IPCSingleton;
import cn.yongye.androbox.virtual.service.interfaces.IPackageManager;

public class VPackageManager {

    private static final VPackageManager sMgr = new VPackageManager();
    private IPCSingleton<IPackageManager> singleton = new IPCSingleton<>(IPackageManager.class);

    public static VPackageManager get() {
        return sMgr;
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
}
