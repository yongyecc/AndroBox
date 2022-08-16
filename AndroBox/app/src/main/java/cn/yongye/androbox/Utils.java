package cn.yongye.androbox;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.io.File;
import java.lang.reflect.Method;

public class Utils {

    /**
     * 获取 ApplicationInfo 实例
     *
     * @return
     */
    public static ApplicationInfo getAppInfo(File file) throws Exception {
        /*
            执行此方法获取 ApplicationInfo
            public static ApplicationInfo generateApplicationInfo(Package p, int flags,PackageUserState state)
         */
        Class<?> mPackageParserClass = Class.forName("android.content.pm.PackageParser");
        Class<?> mPackageClass = Class.forName("android.content.pm.PackageParser$Package");
        Class<?> mPackageUserStateClass = Class.forName("android.content.pm.PackageUserState");
        //获取 generateApplicationInfo 方法
        Method generateApplicationInfoMethod = mPackageParserClass.getDeclaredMethod("generateApplicationInfo",
                mPackageClass, int.class, mPackageUserStateClass);

        //创建 PackageParser 实例
        Object mmPackageParser = mPackageParserClass.newInstance();

        //获取 Package 实例
        /*
            执行此方法获取一个 Package 实例
            public Package parsePackage(File packageFile, int flags)
         */
        //获取 parsePackage 方法
        Method parsePackageMethod = mPackageParserClass.getDeclaredMethod("parsePackage", File.class, int.class);
        //执行 parsePackage 方法获取 Package 实例
        Object mPackage = parsePackageMethod.invoke(mmPackageParser, file, PackageManager.GET_ACTIVITIES);

        //执行 generateApplicationInfo 方法，获取 ApplicationInfo 实例
        ApplicationInfo applicationInfo = (ApplicationInfo) generateApplicationInfoMethod.invoke(null, mPackage, 0,
                mPackageUserStateClass.newInstance());
        //我们获取的 ApplicationInfo 默认路径是没有设置的，我们要自己设置
        // applicationInfo.sourceDir = 插件路径;
        // applicationInfo.publicSourceDir = 插件路径;
        applicationInfo.sourceDir = file.getAbsolutePath();
        applicationInfo.publicSourceDir = file.getAbsolutePath();
        return applicationInfo;
    }


}
