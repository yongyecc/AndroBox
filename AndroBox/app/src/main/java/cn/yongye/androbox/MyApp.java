package cn.yongye.androbox;

import android.app.Application;
import android.content.Context;

import java.io.File;

import cn.yongye.androbox.pm.LoadedApk;
import cn.yongye.androbox.pm.parser.PackageParserEx;
import cn.yongye.androbox.pm.parser.VPackage;
import cn.yongye.androbox.virtual.service.pm.PackageCacheManager;
import cn.yongye.androbox.virtual.service.pm.VAppManagerService;
import me.weishu.reflection.Reflection;
import mirror.android.content.pm.PackageParser;

public class MyApp extends Application {

    private static Context mApp;

    public static Context getInstance(){
        return mApp;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        mApp = this;
        Reflection.unseal(base);
        try {
            VirtualCore.get().startup(mApp);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        /**
         * 1. 安装虚拟应用，将APK文件实例化成一个Package对象。
         *  a. parse apk to Package, save Package into file
         * 2. 创建虚拟应用进程，创建虚拟应用的Applicaiton。
         * 3. 怎么启动虚拟应用的Activity。
         */
        try {
            //dump virtual app file
            File file = new File(String.format("%s/helloworld.apk",
                    MyApp.getInstance().getFilesDir().getAbsolutePath()));
            if(!file.exists())
                FileUtils.dumpFile("helloworld.apk", file.getAbsolutePath());
            //1. install apk
            //1.a parse apk to Package
            VAppManagerService.get().installPackage(file.getAbsolutePath(), 1, true);
            VPackage vPackage = PackageCacheManager.get("cn.yongye.helloworld");
            //1. apk to LoadedApk objection
            Object loadedApk = LoadedApk.getInstance(mApp).makeLoadedApk();
            //2. make virtual application
            VirtualCore.get().makeVApplication(loadedApk);
        } catch (Exception e) {
            e.printStackTrace();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }
}
