package cn.yongye.androbox.virtual.server.pm;

import android.os.Build;
import android.os.RemoteCallbackList;
import android.os.RemoteException;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import cn.yongye.androbox.FileUtils;
import cn.yongye.androbox.VirtualCore;
import cn.yongye.androbox.client.env.VirtualRuntime;
import cn.yongye.androbox.helper.utils.VLog;
import cn.yongye.androbox.os.VEnvironment;
import cn.yongye.androbox.pm.parser.PackageParserEx;
import cn.yongye.androbox.pm.parser.VPackage;
import cn.yongye.androbox.virtual.server.am.BroadcastSystem;
import cn.yongye.androbox.virtual.server.interfaces.IAppManager;
import cn.yongye.androbox.virtual.server.interfaces.IAppRequestListener;
import cn.yongye.androbox.virtual.server.interfaces.IPackageObserver;

public class VAppManagerService implements IAppManager {

    private static final String TAG = VAppManagerService.class.getSimpleName();
    private static final AtomicReference<VAppManagerService> sService = new AtomicReference<>();
//    private final UidSystem mUidSystem = new UidSystem();
    private final PackagePersistenceLayer mPersistenceLayer = new PackagePersistenceLayer(this);
    private final Set<String> mVisibleOutsidePackages = new HashSet<>();
    private boolean mBooting;
    private RemoteCallbackList<IPackageObserver> mRemoteCallbackList = new RemoteCallbackList<>();
    private IAppRequestListener mAppRequestListener;


    public static VAppManagerService get() {
        VEnvironment.systemReady();
        VAppManagerService instance = new VAppManagerService();
        sService.set(instance);
        return sService.get();
    }

    public int getAppId(String packageName) {
        PackageSetting setting = PackageCacheManager.getSetting(packageName);
        return setting != null ? setting.appId : -1;
    }

    public static void systemReady() {
        VEnvironment.systemReady();
        VAppManagerService instance = new VAppManagerService();
//        instance.mUidSystem.initUidList();
        sService.set(instance);
    }

    void restoreFactoryState() {
        VLog.w(TAG, "Warning: Restore the factory state...");
        VEnvironment.getDalvikCacheDirectory().delete();
        VEnvironment.getUserSystemDirectory().delete();
        VEnvironment.getDataAppDirectory().delete();
    }

    synchronized void loadPackage(PackageSetting setting) {
        if (!loadPackageInnerLocked(setting)) {
            cleanUpResidualFiles(setting);
        }
    }

    private void cleanUpResidualFiles(PackageSetting ps) {
        File dataAppDir = VEnvironment.getDataAppPackageDirectory(ps.packageName);
        FileUtils.deleteDir(dataAppDir);
//        for (int userId : VUserManagerService.get().getUserIds()) {
//            FileUtils.deleteDir(VEnvironment.getDataUserPackageDirectory(userId, ps.packageName));
//        }
    }

    private boolean loadPackageInnerLocked(PackageSetting ps) {
        if (ps.dependSystem) {
//            if (!VirtualCore.get().isOutsideInstalled(ps.packageName)) {
//                return false;
//            }
        }
        File cacheFile = VEnvironment.getPackageCacheFile(ps.packageName);
        VPackage pkg = null;
        try {
            pkg = PackageParserEx.readPackageCache(ps.packageName);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        if (pkg == null || pkg.packageName == null) {
            return false;
        }
        chmodPackageDictionary(cacheFile);
        PackageCacheManager.put(pkg, ps);
        BroadcastSystem.get().startApp(pkg);
        return true;
    }

    private void chmodPackageDictionary(File packageFile) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (FileUtils.isSymlink(packageFile)) {
                    return;
                }
                FileUtils.chmod(packageFile.getParentFile().getAbsolutePath(), FileUtils.FileMode.MODE_755);
                FileUtils.chmod(packageFile.getAbsolutePath(), FileUtils.FileMode.MODE_755);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    public void savePersistenceData() {
        mPersistenceLayer.save();
    }

    @Override
    public InstalledAppInfo getInstalledAppInfo(String packageName, int flags) {
        synchronized (PackageCacheManager.class) {
            if (packageName != null) {
                PackageSetting setting = PackageCacheManager.getSetting(packageName);
                if (setting != null) {
                    return setting.getAppInfo();
                }
            }
            return null;
        }
    }

    @Override
    public IAppRequestListener getAppRequestListener() {
        return mAppRequestListener;
    }
}
