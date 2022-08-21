package cn.yongye.androbox.virtual.service.pm;

import android.content.pm.PackageInfo;
import android.os.RemoteException;

import java.util.concurrent.atomic.AtomicReference;

import cn.yongye.androbox.virtual.service.pm.PackageSetting;
import cn.yongye.androbox.pm.parser.PackageParserEx;
import cn.yongye.androbox.pm.parser.VPackage;
import cn.yongye.androbox.virtual.service.interfaces.IPackageManager;

public class VPackageManagerService implements IPackageManager {

    private static final AtomicReference<VPackageManagerService> gService = new AtomicReference<>();



    public static VPackageManagerService get() {
        return gService.get();
    }

    @Override
    public PackageInfo getPackageInfo(String packageName, int flags, int userId) throws RemoteException {
        return null;
    }

    private PackageInfo generatePackageInfo(VPackage p, PackageSetting ps, int flags, int userId) {

        PackageInfo packageInfo = PackageParserEx.generatePackageInfo(p, flags,
                ps.firstInstallTime, ps.lastUpdateTime, ps.readUserState(userId), userId);
        if (packageInfo != null) {
            return packageInfo;
        }
        return null;
    }
}
