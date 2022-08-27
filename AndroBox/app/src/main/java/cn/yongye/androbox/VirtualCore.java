package cn.yongye.androbox;

import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;

import java.util.List;
import java.util.Map;

import cn.yongye.androbox.client.core.InvocationStubManager;
import cn.yongye.androbox.client.hook.delegate.AppInstrumentation;
import cn.yongye.androbox.client.ipc.ServiceManagerNative;
import cn.yongye.androbox.helper.ipcbus.IPCBus;
import cn.yongye.androbox.helper.ipcbus.IServerCache;
import cn.yongye.androbox.pm.LoadedApk;
import cn.yongye.androbox.reflect.RefInvoke;
import cn.yongye.androbox.virtual.service.ServiceCache;
import mirror.android.app.ActivityThread;

public class VirtualCore {

    public String TAG = this.getClass().getSimpleName();

    private Context context;
    private static VirtualCore instance;
    private PackageInfo hostPkgInfo;
    /**
     * ActivityThread instance
     */
    private Object mainThread;
    private static VirtualCore gCore = new VirtualCore();
    private final int myUid = Process.myUid();
    private boolean isStartUp;

    /**
     * Client Package Manager
     */
    private PackageManager unHookPackageManager;
    /**
     * Host package name
     */
    private String hostPkgName;

    public static VirtualCore get() {
        return gCore;
    }

    public String getHostPkg() {
        return hostPkgName;
    }

    public Context getContext() {
        return context;
    }

    public boolean isStartup() {
        return isStartUp;
    }

    public static Object mainThread() {
        return get().mainThread;
    }

    public void startup(Context context) throws Throwable {
        this.context = context;
        unHookPackageManager = context.getPackageManager();
        hostPkgInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_PROVIDERS);
        mainThread = ActivityThread.currentActivityThread.call();
        detectProcessType();

        IPCBus.initialize(new IServerCache() {
            @Override
            public void join(String serverName, IBinder binder) {
                ServiceCache.addService(serverName, binder);
            }

            @Override
            public IBinder query(String serverName) {
                return ServiceManagerNative.getService(serverName);
            }
        });

        isStartUp = true;

        InvocationStubManager invocationStubManager = InvocationStubManager.getInstance();
        //Create proxy object for system services and save them to map.
        invocationStubManager.init();
        //The realization process of dynamic proxy
        invocationStubManager.injectAll();
    }

    private void detectProcessType() {
        // Host package name
        hostPkgName = context.getApplicationInfo().packageName;
//        // Main process name
//        mainProcessName = context.getApplicationInfo().processName;
//        // Current process name
//        processName = ActivityThread.getProcessName.call(mainThread);
//        if (processName.equals(mainProcessName)) {
//            processType = ProcessType.Main;
//        } else if (processName.endsWith(Constants.SERVER_PROCESS_NAME)) {
//            processType = ProcessType.Server;
//        } else if (VActivityManager.get().isAppProcess(processName)) {
//            processType = ProcessType.VAppClient;
//        } else {
//            processType = ProcessType.CHILD;
//        }
//        if (isVAppProcess()) {
//            systemPid = VActivityManager.get().getSystemPid();
//        }
    }

    public PackageManager getUnHookPackageManager() {
        return unHookPackageManager;
    }


    public int[] getGids() {
        return hostPkgInfo.gids;
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
        //make packageInfo for apk


        //call LoadedApk.makeApplication, make Applicaiton object
        //dynamic proxy PackageManager, genarate packageInfo
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
        Instrumentation mInstrumentation = AppInstrumentation.getDefault();
        mInstrumentation.callApplicationOnCreate(application);
        application.onCreate();
        Log.d(TAG, "makeVApplication end.");
    }



}
