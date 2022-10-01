package cn.yongye.androbox;

import android.app.Application;
import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.ConditionVariable;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;

import java.util.List;
import java.util.Map;

import cn.yongye.androbox.client.core.InvocationStubManager;
import cn.yongye.androbox.client.env.Constants;
import cn.yongye.androbox.client.env.VirtualRuntime;
import cn.yongye.androbox.client.hook.delegate.AppInstrumentation;
import cn.yongye.androbox.client.ipc.ServiceManagerNative;
import cn.yongye.androbox.client.ipc.VActivityManager;
import cn.yongye.androbox.client.ipc.VPackageManager;
import cn.yongye.androbox.helper.ipcbus.IPCBus;
import cn.yongye.androbox.helper.ipcbus.IPCSingleton;
import cn.yongye.androbox.helper.ipcbus.IServerCache;
import cn.yongye.androbox.reflect.RefInvoke;
import cn.yongye.androbox.virtual.server.ServiceCache;
import cn.yongye.androbox.virtual.server.interfaces.IAppManager;
import cn.yongye.androbox.virtual.server.interfaces.IAppRequestListener;
import cn.yongye.androbox.virtual.server.pm.InstalledAppInfo;
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
    private ProcessType processType;
    private boolean isStartUp;
    private ConditionVariable initLock = new ConditionVariable();
    private IPCSingleton<IAppManager> singleton = new IPCSingleton<>(IAppManager.class);

    /**
     * Client Package Manager
     */
    private PackageManager unHookPackageManager;
    /**
     * Host package name
     */
    private String hostPkgName;
    private String processName;
    /**
     * Main ProcessName
     */
    private String mainProcessName;
    private int systemPid;
    public static VirtualCore get() {
        return gCore;
    }

    public IAppRequestListener getAppRequestListener() {
        try {
            return getService().getAppRequestListener();
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
    }

    /**
     * Process type
     */
    private enum ProcessType {
        /**
         * Server process
         */
        Server,
        /**
         * Virtual app process
         */
        VAppClient,
        /**
         * Main process
         */
        Main,
        /**
         * Child process
         */
        CHILD
    }

    /**
     * @return If the current process is used to VA.
     */
    public boolean isVAppProcess() {
        return ProcessType.VAppClient == processType;
    }

    public ConditionVariable getInitLock() {
        return initLock;
    }

    private IAppManager getService() {
        return singleton.get();
    }

    public Resources getResources(String pkg) throws Resources.NotFoundException {
        InstalledAppInfo installedAppInfo = getInstalledAppInfo(pkg, 0);
        if (installedAppInfo != null) {
            AssetManager assets = mirror.android.content.res.AssetManager.ctor.newInstance();
            mirror.android.content.res.AssetManager.addAssetPath.call(assets, installedAppInfo.apkPath);
            Resources hostRes = context.getResources();
            return new Resources(assets, hostRes.getDisplayMetrics(), hostRes.getConfiguration());
        }
        throw new Resources.NotFoundException(pkg);
    }

    public InstalledAppInfo getInstalledAppInfo(String pkg, int flags) {
        try {
            return getService().getInstalledAppInfo(pkg, flags);
        } catch (RemoteException e) {
            return VirtualRuntime.crash(e);
        }
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
        // Main process name
        mainProcessName = context.getApplicationInfo().processName;
        // Current process name
        processName = ActivityThread.getProcessName.call(mainThread);
        if (processName.equals(mainProcessName)) {
            processType = ProcessType.Main;
        } else if (processName.endsWith(Constants.SERVER_PROCESS_NAME)) {
            processType = ProcessType.Server;
        } else if (VActivityManager.get().isAppProcess(processName)) {
            processType = ProcessType.VAppClient;
        } else {
            processType = ProcessType.CHILD;
        }
        if (isVAppProcess()) {
//            systemPid = VActivityManager.get().getSystemPid();
        }
    }

    public PackageManager getUnHookPackageManager() {
        return unHookPackageManager;
    }


    public int[] getGids() {
        return hostPkgInfo.gids;
    }

    public ServiceInfo resolveServiceInfo(Intent intent, int userId) {
        ServiceInfo serviceInfo = null;
        ResolveInfo resolveInfo = VPackageManager.get().resolveService(intent, intent.getType(), 0, userId);
        if (resolveInfo != null) {
            serviceInfo = resolveInfo.serviceInfo;
        }
        return serviceInfo;
    }


    public synchronized ActivityInfo resolveActivityInfo(Intent intent, int userId) {
        ActivityInfo activityInfo = null;
        if (intent.getComponent() == null) {
            ResolveInfo resolveInfo = VPackageManager.get().resolveIntent(intent, intent.getType(), 0, userId);
            if (resolveInfo != null && resolveInfo.activityInfo != null) {
                activityInfo = resolveInfo.activityInfo;
                intent.setClassName(activityInfo.packageName, activityInfo.name);
            }
        } else {
            activityInfo = resolveActivityInfo(intent.getComponent(), userId);
        }
        if (activityInfo != null) {
            if (activityInfo.targetActivity != null) {
                ComponentName componentName = new ComponentName(activityInfo.packageName, activityInfo.targetActivity);
                activityInfo = VPackageManager.get().getActivityInfo(componentName, 0, userId);
                intent.setComponent(componentName);
            }
        }
        return activityInfo;
    }

    public ActivityInfo resolveActivityInfo(ComponentName componentName, int userId) {
        return VPackageManager.get().getActivityInfo(componentName, 0, userId);
    }
}
