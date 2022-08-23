package cn.yongye.androbox.virtual.service.pm;

import android.content.pm.PackageInfo;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

import cn.yongye.androbox.os.VEnvironment;
import cn.yongye.androbox.pm.parser.PackageParserEx;
import cn.yongye.androbox.pm.parser.VPackage;

    public class VAppManagerService {

    private static final String TAG = VAppManagerService.class.getSimpleName();
    private static final AtomicReference<VAppManagerService> sService = new AtomicReference<>();


    public static VAppManagerService get() {
        VEnvironment.systemReady();
        VAppManagerService instance = new VAppManagerService();
        sService.set(instance);
        return sService.get();
    }

    public static void systemReady() {

    }

    public synchronized InstallResult installPackage(String path, int flags, boolean notify) {
        long installTime = System.currentTimeMillis();
        if (path == null) {
            return InstallResult.makeFailure("path = NULL");
        }
        File packageFile = new File(path);
        if (!packageFile.exists() || !packageFile.isFile()) {
            return InstallResult.makeFailure("Package File is not exist.");
        }
        VPackage pkg = null;
        try {
            pkg = PackageParserEx.parsePackage(packageFile);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        InstallResult res = new InstallResult();
        res.packageName = pkg.packageName;
        File appDir = VEnvironment.getDataAppPackageDirectory(pkg.packageName);
        File libDir = new File(appDir, "lib");
        PackageSetting ps;
        ps = new PackageSetting();
        ps.dependSystem = false;
        ps.apkPath = packageFile.getPath();
        ps.libPath = libDir.getPath();
        ps.packageName = pkg.packageName;
        ps.appId = 110;
        if (res.isUpdate) {
            ps.lastUpdateTime = installTime;
        } else {
            ps.firstInstallTime = installTime;
            ps.lastUpdateTime = installTime;
//            for (int userId : VUserManagerService.get().getUserIds()) {
//                boolean installed = userId == 0;
//                ps.setUserState(userId, false/*launched*/, false/*hidden*/, installed);
//            }
        }
        //save VPackage into file
        PackageParserEx.savePackageCache(pkg);
        PackageCacheManager.put(pkg, ps);
        return res;
    }
}
