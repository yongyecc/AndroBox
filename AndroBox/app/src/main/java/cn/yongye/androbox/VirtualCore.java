package cn.yongye.androbox;

import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;

import java.util.List;
import java.util.Map;

import cn.yongye.androbox.pm.LoadedApk;
import cn.yongye.androbox.reflect.RefInvoke;

public class VirtualCore {

    private Context context;
    private static VirtualCore instance;

    private VirtualCore(Context context) {
        this.context = context;
    }

    public static VirtualCore getInstance(Context context) {
        if (instance == null) {
            synchronized (VirtualCore.class) {
                if (instance == null) {
                    instance = new VirtualCore(context);
                }
            }
        }
        return instance;
    }


    public void makeVApplication(Object loadedApk){
        String stActivityThread = "android.app.ActivityThread";
        String stClassLoadedApk = "android.app.LoadedApk";
        Object obCurrentActivityThread = RefInvoke.invokeStaticMethod(stActivityThread,
                "currentActivityThread", new Class[]{}, new Object[]{});
        //AppBindData objection
        Object mBoundApplication = RefInvoke.getFieldObject(stActivityThread, obCurrentActivityThread, "mBoundApplication");
        //LoadedApk object
        Object loadedApkInfo = loadedApk;
//        Object loadedApkInfo = RefInvoke.getFieldObject(stActivityThread +"$AppBindData", mBoundApplication, "info");
        //LoadedApk.Application = null
//        RefInvoke.setFieldObject(stClassLoadedApk, "mApplication", loadedApkInfo, null);
        //ActivityThread.Applition
        Object mInitApplication = RefInvoke.getFieldObject(stActivityThread, obCurrentActivityThread, "mInitialApplication");
        //ActivityThread.Applitions
        List<Application> mAllApplications = (List<Application>) RefInvoke.getFieldObject(stActivityThread, obCurrentActivityThread, "mAllApplications");
        mAllApplications.remove(mInitApplication);
        //LoadedApk.ApplicationInfo.classname=
//        ((ApplicationInfo) RefInvoke.getFieldObject(stClassLoadedApk, loadedApkInfo, "mApplicationInfo")).className = "";
        //call LoadedApk.makeApplication, make Applicaiton object
        Application application = (Application) RefInvoke.invokeMethod(stClassLoadedApk,
                "makeApplication", loadedApkInfo, new Class[]{boolean.class, Instrumentation.class}, new Object[]{false, null});
        //ActivityThread.Applition = LoadedApk.Applicaiton
        RefInvoke.setFieldObject(stActivityThread, "mInitialApplication", obCurrentActivityThread,
                application);
        Map<?,?> mProviderMap = (Map<?,?>) RefInvoke.getFieldObject(stActivityThread, obCurrentActivityThread,
                "mProviderMap");
        for (Map.Entry<?, ?> entry : mProviderMap.entrySet()) {
            Object providerClientRecord = entry.getValue();
            Object mLocalProvider = RefInvoke.getFieldObject(stActivityThread+"$ProviderClientRecord", providerClientRecord, "mLocalProvider");
            RefInvoke.setFieldObject("android.content.ContentProvider", "mContext", mLocalProvider, application);
        }
        application.onCreate();
    }
}
