package cn.yongye.androbox.client;

import android.annotation.SuppressLint;
import android.app.Application;
import android.app.Instrumentation;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.os.Build;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import cn.yongye.androbox.VirtualCore;
import cn.yongye.androbox.client.env.VirtualRuntime;
import cn.yongye.androbox.client.hook.delegate.AppInstrumentation;
import cn.yongye.androbox.client.ipc.VActivityManager;
import cn.yongye.androbox.reflect.RefInvoke;
import cn.yongye.androbox.remote.PendingResultData;
import mirror.android.app.ActivityThread;
import mirror.android.app.ContextImpl;
import mirror.com.android.internal.content.ReferrerIntent;

public final class VClientImpl extends IVClient.Stub {

    private static final int NEW_INTENT = 11;
    private static final int RECEIVER = 12;

    private static final String TAG = VClientImpl.class.getSimpleName();
    @SuppressLint("StaticFieldLeak")
    private static final VClientImpl gClient = new VClientImpl();
//    private final H mH = new H();
    private ConditionVariable mTempLock;
    private Instrumentation mInstrumentation = AppInstrumentation.getDefault();
    private IBinder token;
    private int vuid;
//    private VDeviceInfo deviceInfo;
    private AppBindData mBoundApplication;
    private Application mInitialApplication;
//    private CrashHandler crashHandler;

    public static VClientImpl get() {
        return gClient;
    }

    public void initProcess(IBinder token, int vuid) {
        this.token = token;
        this.vuid = vuid;
    }

    public ClassLoader getClassLoader(ApplicationInfo appInfo) {
        Context context = createPackageContext(appInfo.packageName);
        return context.getClassLoader();
    }

    private Context createPackageContext(String packageName) {
        try {
            Context hostContext = VirtualCore.get().getContext();
            return hostContext.createPackageContext(packageName, Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            VirtualRuntime.crash(new RemoteException());
        }
        throw new RuntimeException();
    }

    public Application getCurrentApplication() {
        return mInitialApplication;
    }

    public void makeVApplication(Context context, Object loadedApk){
        String stActivityThread = "android.app.ActivityThread";
        String stClassLoadedApk = "android.app.LoadedApk";
        Object obCurrentActivityThread = RefInvoke.invokeStaticMethod(stActivityThread,
                "currentActivityThread", new Class[]{}, new Object[]{});
        //AppBindData objection
        Object mBoundApplication = RefInvoke.getFieldObject(stActivityThread, obCurrentActivityThread, "mBoundApplication");
        //LoadedApk object
        Object loadedApkInfo = loadedApk;
//        Object loadedApkInfo = createPackageContext("cn.yongye.helloworld");
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
        mInitialApplication = application;
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

    @Override
    public void scheduleReceiver(String processName, ComponentName component, Intent intent, PendingResultData resultData) throws RemoteException {

    }

    @Override
    public void scheduleNewIntent(String creator, IBinder token, Intent intent) throws RemoteException {

    }

    @Override
    public void finishActivity(IBinder token) throws RemoteException {

    }

    @Override
    public IBinder createProxyService(ComponentName component, IBinder binder) throws RemoteException {
        return null;
    }

    @Override
    public IBinder acquireProviderClient(ProviderInfo info) throws RemoteException {
        return null;
    }

    @Override
    public IBinder getAppThread() throws RemoteException {
        return ActivityThread.getApplicationThread.call(VirtualCore.mainThread());
    }

    @Override
    public IBinder getToken() {
        return token;
    }

    @Override
    public String getDebugInfo() throws RemoteException {
        return null;
    }

    private final class NewIntentData {
        String creator;
        IBinder token;
        Intent intent;
    }

    private final class AppBindData {
        String processName;
        ApplicationInfo appInfo;
        List<ProviderInfo> providers;
        Object info;
    }

    private final class ReceiverData {
        PendingResultData resultData;
        Intent intent;
        ComponentName component;
        String processName;
    }

    public boolean isBound() {
        return mBoundApplication != null;
    }

    private class H extends Handler {

        private H() {
            super(Looper.getMainLooper());
        }

        private void handleNewIntent(NewIntentData data) {
            Intent intent;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                intent = ReferrerIntent.ctor.newInstance(data.intent, data.creator);
            } else {
                intent = data.intent;
            }
            if (ActivityThread.performNewIntents != null) {
                ActivityThread.performNewIntents.call(
                        VirtualCore.mainThread(),
                        data.token,
                        Collections.singletonList(intent)
                );
            }
//            else {
//                ActivityThreadNMR1.performNewIntents.call(
//                        VirtualCore.mainThread(),
//                        data.token,
//                        Collections.singletonList(intent),
//                        true);
//            }
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case NEW_INTENT: {
                    handleNewIntent((NewIntentData) msg.obj);
                }
                break;
                case RECEIVER: {
                    handleReceiver((ReceiverData) msg.obj);
                }
            }
        }

        private void handleReceiver(ReceiverData data) {
            BroadcastReceiver.PendingResult result = data.resultData.build();
            try {
//                if (!isBound()) {
//                    bindApplication(data.component.getPackageName(), data.processName);
//                }
                Context context = mInitialApplication.getBaseContext();
                Context receiverContext = ContextImpl.getReceiverRestrictedContext.call(context);
                String className = data.component.getClassName();
                BroadcastReceiver receiver = (BroadcastReceiver) context.getClassLoader().loadClass(className).newInstance();
                mirror.android.content.BroadcastReceiver.setPendingResult.call(receiver, result);
                data.intent.setExtrasClassLoader(context.getClassLoader());
                if (data.intent.getComponent() == null) {
                    data.intent.setComponent(data.component);
                }
                receiver.onReceive(receiverContext, data.intent);
                if (mirror.android.content.BroadcastReceiver.getPendingResult.call(receiver) != null) {
                    result.finish();
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(
                        "Unable to start receiver " + data.component
                                + ": " + e.toString(), e);
            }
            VActivityManager.get().broadcastFinish(data.resultData);
        }
    }

    public void bindApplication(final String packageName, final String processName) {
        if (Looper.getMainLooper() == Looper.myLooper()) {
            bindApplicationNoCheck(packageName, processName, new ConditionVariable());
        } else {
            final ConditionVariable lock = new ConditionVariable();
            VirtualRuntime.getUIHandler().post(new Runnable() {
                @Override
                public void run() {
                    bindApplicationNoCheck(packageName, processName, lock);
                    lock.open();
                }
            });
            lock.block();
        }
    }

    private void bindApplicationNoCheck(String packageName, String processName, ConditionVariable lock) {
        String stActivityThread = "android.app.ActivityThread";
        String stClassLoadedApk = "android.app.LoadedApk";
        Object obCurrentActivityThread = RefInvoke.invokeStaticMethod(stActivityThread,
                "currentActivityThread", new Class[]{}, new Object[]{});
        //AppBindData objection
        Object mBoundApplication = RefInvoke.getFieldObject(stActivityThread, obCurrentActivityThread, "mBoundApplication");
        //LoadedApk object
        AppBindData data = new AppBindData();
        this.mBoundApplication = data;
//        Object loadedApkInfo = loadedApk;
        Context context = createPackageContext(packageName);
        Object loadedApkInfo = ContextImpl.mPackageInfo.get(context);
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
        mInitialApplication = application;
        Map<?,?> mProviderMap = (Map<?,?>) RefInvoke.getFieldObject(stActivityThread, obCurrentActivityThread,
                "mProviderMap");
//        for (Map.Entry<?, ?> entry : mProviderMap.entrySet()) {
//            Object providerClientRecord = entry.getValue();
//            Object mLocalProvider = RefInvoke.getFieldObject(stActivityThread+"$ProviderClientRecord", providerClientRecord, "mLocalProvider");
//            RefInvoke.setFieldObject("android.content.ContentProvider", "mContext", mLocalProvider, application);
//        }
        Instrumentation mInstrumentation = AppInstrumentation.getDefault();
        mInstrumentation.callApplicationOnCreate(application);
        application.onCreate();
        Log.d(TAG, "makeVApplication end.");
    }
}
