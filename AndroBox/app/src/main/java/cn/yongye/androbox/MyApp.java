package cn.yongye.androbox;

import android.app.Application;
import android.content.Context;

import cn.yongye.androbox.pm.LoadedApk;
import me.weishu.reflection.Reflection;

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


    }

    @Override
    public void onCreate() {
        super.onCreate();

        /**
         * 1. 安装虚拟应用。 apk-》loadedApk
         * 2. 创建虚拟应用进程，创建虚拟应用的Applicaiton。
         * 3. 怎么启动虚拟应用的Activity。
         */
        try {
            //1. apk to LoadedApk objection
            Object loadedApk = LoadedApk.getInstance(mApp).makeLoadedApk();
            //2. make virtual application
            VirtualCore.getInstance(mApp).makeVApplication(loadedApk);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
