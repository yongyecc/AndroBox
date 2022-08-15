package cn.yongye.androbox;

import android.app.Application;
import android.content.Context;

public class MyApp extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        /**
         * 1. 安装虚拟应用。 apk-》loadedApk
         * 2. 创建虚拟应用进程，创建虚拟应用的Applicaiton。
         * 3. 怎么启动虚拟应用的Activity。
         */
    }
}
