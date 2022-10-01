package cn.yongye.androbox.pm;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Environment;
import android.util.ArrayMap;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import cn.yongye.androbox.FileUtils;
import cn.yongye.androbox.MyApp;
import cn.yongye.androbox.Utils;
import cn.yongye.androbox.client.env.VirtualRuntime;
import dalvik.system.DexClassLoader;

public class LoadedApk {

    private Context context;
    private static LoadedApk instance;

    private LoadedApk(Context context) {
        this.context = context;
    }

    public static LoadedApk getInstance(Context context) {
        if (instance == null) {
            synchronized (LoadedApk.class) {
                if (instance == null) {
                    instance = new LoadedApk(context);
                }
            }
        }
        return instance;
    }

    /**
     * 将插件APK实例化成一个LoadedApk对象，并加载进主线程的mPackages字段中。
     *
     * @throws Exception
     */
    public Object makeLoadedApk() throws Exception {
        File file = new File(String.format("%s/helloworld.apk",
                MyApp.getInstance().getFilesDir().getAbsolutePath()));
        if(!file.exists())
            FileUtils.dumpFile("helloworld.apk", file.getAbsolutePath());
        //获取 ActivityThread 类
        Class<?> mActivityThreadClass = Class.forName("android.app.ActivityThread");
        //获取 ActivityThread 的 currentActivityThread() 方法
        Method currentActivityThread = mActivityThreadClass.getDeclaredMethod("currentActivityThread");
        currentActivityThread.setAccessible(true);
        //获取 ActivityThread 实例
        Object mActivityThread = currentActivityThread.invoke(null);
        //获取 mPackages 属性
        Field mPackagesField = mActivityThreadClass.getDeclaredField("mPackages");
        mPackagesField.setAccessible(true);
        //获取 mPackages 属性的值
        ArrayMap<String, Object> mPackages = (ArrayMap<String, Object>) mPackagesField.get(mActivityThread);


        Class<?> mCompatibilityInfoClass = Class.forName("android.content.res.CompatibilityInfo");
        Method getLoadedApkMethod = mActivityThreadClass.getDeclaredMethod("getPackageInfoNoCheck",
                ApplicationInfo.class, mCompatibilityInfoClass);

        /*
             public static final CompatibilityInfo DEFAULT_COMPATIBILITY_INFO = new CompatibilityInfo() {};
         */
        //以上注释是获取默认的 CompatibilityInfo 实例
        Field mCompatibilityInfoDefaultField = mCompatibilityInfoClass.getDeclaredField("DEFAULT_COMPATIBILITY_INFO");
        Object mCompatibilityInfo = mCompatibilityInfoDefaultField.get(null);

        //获取一个 ApplicationInfo实例
        ApplicationInfo applicationInfo = Utils.getAppInfo(file);
        //执行此方法，获取一个 LoadedApk
        Object mLoadedApk = getLoadedApkMethod.invoke(mActivityThread, applicationInfo, mCompatibilityInfo);

        //自定义一个 ClassLoader
        String optimizedDirectory = context.getDir("helloworld", Context.MODE_PRIVATE).getAbsolutePath();
        DexClassLoader classLoader = new DexClassLoader(file.getAbsolutePath(), optimizedDirectory,
                null, context.getClassLoader());

        //private ClassLoader mClassLoader;
        //获取 LoadedApk 的 mClassLoader 属性
        Field mClassLoaderField = mLoadedApk.getClass().getDeclaredField("mClassLoader");
        mClassLoaderField.setAccessible(true);
        //设置自定义的 classLoader 到 mClassLoader 属性中
        mClassLoaderField.set(mLoadedApk, classLoader);

        WeakReference loadApkReference = new WeakReference(mLoadedApk);
        //添加自定义的 LoadedApk
        mPackages.put(applicationInfo.packageName, loadApkReference);
        //重新设置 mPackages
        mPackagesField.set(mActivityThread, mPackages);
        Thread.sleep(2000);
        return mLoadedApk;
    }
}
