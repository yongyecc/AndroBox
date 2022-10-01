package cn.yongye.androbox.virtual.server.am;

import static android.os.Process.killProcess;
import static cn.yongye.androbox.os.VBinder.getCallingPid;
import static cn.yongye.androbox.os.VUserHandle.getUserId;

import android.app.ActivityManager;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.SparseArray;
import android.os.Process;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import cn.yongye.androbox.VirtualCore;
import cn.yongye.androbox.client.IVClient;
import cn.yongye.androbox.client.env.SpecialComponentList;
import cn.yongye.androbox.client.ipc.ProviderCall;
import cn.yongye.androbox.client.stub.VASettings;
import cn.yongye.androbox.helper.compat.ApplicationThreadCompat;
import cn.yongye.androbox.helper.compat.BundleCompat;
import cn.yongye.androbox.helper.compat.IApplicationThreadCompat;
import cn.yongye.androbox.helper.utils.ComponentUtils;
import cn.yongye.androbox.helper.utils.VLog;
import cn.yongye.androbox.os.VUserHandle;
import cn.yongye.androbox.remote.BadgerInfo;
import cn.yongye.androbox.remote.PendingResultData;
import cn.yongye.androbox.virtual.server.interfaces.IActivityManager;
import cn.yongye.androbox.virtual.server.pm.PackageCacheManager;
import cn.yongye.androbox.virtual.server.pm.PackageSetting;
import cn.yongye.androbox.virtual.server.pm.VAppManagerService;
import cn.yongye.androbox.virtual.server.pm.VPackageManagerService;

public class VActivityManagerService implements IActivityManager {

    private static final boolean BROADCAST_NOT_STARTED_PKG = false;

    private static final AtomicReference<VActivityManagerService> sService = new AtomicReference<>();
    private static final String TAG = VActivityManagerService.class.getSimpleName();
    private final SparseArray<ProcessRecord> mPidsSelfLocked = new SparseArray<ProcessRecord>();
    private final ActivityStack mMainStack = new ActivityStack(this);
    private final Set<ServiceRecord> mHistory = new HashSet<ServiceRecord>();
    private final ProcessMap<ProcessRecord> mProcessNames = new ProcessMap<ProcessRecord>();
    private final PendingIntents mPendingIntents = new PendingIntents();
    private ActivityManager am = (ActivityManager) VirtualCore.get().getContext()
            .getSystemService(Context.ACTIVITY_SERVICE);
    private NotificationManager nm = (NotificationManager) VirtualCore.get().getContext()
            .getSystemService(Context.NOTIFICATION_SERVICE);

    public static VActivityManagerService get() {
        return sService.get();
    }

    public static void systemReady(Context context) {
        new VActivityManagerService().onCreate(context);
    }

    public void onCreate(Context context) {
        AttributeCache.init(context);
        PackageManager pm = context.getPackageManager();
        PackageInfo packageInfo = null;
        try {
            packageInfo = pm.getPackageInfo(context.getPackageName(),
                    PackageManager.GET_ACTIVITIES | PackageManager.GET_PROVIDERS | PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        if (packageInfo == null) {
            throw new RuntimeException("Unable to found PackageInfo : " + context.getPackageName());
        }
        sService.set(this);
    }




    @Override
    public int startActivity(Intent intent, ActivityInfo info, IBinder resultTo, Bundle options, String resultWho, int requestCode, int userId) {
        synchronized (this) {
            return mMainStack.startActivityLocked(userId, intent, info, resultTo, options, resultWho, requestCode);
        }
    }

    @Override
    public ComponentName startService(IBinder caller, Intent service, String resolvedType, int userId) {
        synchronized (this) {
            return startServiceCommon(service, true, userId);
        }
    }

    ProcessRecord startProcessIfNeedLocked(String processName, int userId, String packageName) {
//        if (VActivityManagerService.get().getFreeStubCount() < 3) {
//            // run GC
//            killAllApps();
//        }
        PackageSetting ps = PackageCacheManager.getSetting(packageName);
        ApplicationInfo info = VPackageManagerService.get().getApplicationInfo(packageName, 0, userId);
        if (ps == null || info == null) {
            return null;
        }
        if (!ps.isLaunched(userId)) {
            sendFirstLaunchBroadcast(ps, userId);
            ps.setLaunched(userId, true);
            VAppManagerService.get().savePersistenceData();
        }
        int uid = VUserHandle.getUid(userId, ps.appId);
        ProcessRecord app = mProcessNames.get(processName, uid);
        if (app != null && app.client.asBinder().isBinderAlive()) {
            return app;
        }
        int vpid = queryFreeStubProcessLocked();
        if (vpid == -1) {
            return null;
        }
        app = performStartProcessLocked(uid, vpid, info, processName);
        if (app != null) {
            app.pkgList.add(info.packageName);
        }
        return app;
    }

    private ProcessRecord performStartProcessLocked(int vuid, int vpid, ApplicationInfo info, String processName) {
        ProcessRecord app = new ProcessRecord(info, processName, vuid, vpid);
        Bundle extras = new Bundle();
        BundleCompat.putBinder(extras, "_VA_|_binder_", app);
        extras.putInt("_VA_|_vuid_", vuid);
        extras.putString("_VA_|_process_", processName);
        extras.putString("_VA_|_pkg_", info.packageName);
        Bundle res = ProviderCall.call(VASettings.getStubAuthority(vpid), "_VA_|_init_process_", null, extras);
        if (res == null) {
            return null;
        }
        int pid = res.getInt("_VA_|_pid_");
        IBinder clientBinder = BundleCompat.getBinder(res, "_VA_|_client_");
        attachClient(pid, clientBinder);
        return app;
    }

    private void attachClient(int pid, final IBinder clientBinder) {
        final IVClient client = IVClient.Stub.asInterface(clientBinder);
        if (client == null) {
            killProcess(pid);
            return;
        }
        IInterface thread = null;
        try {
            thread = ApplicationThreadCompat.asInterface(client.getAppThread());
        } catch (RemoteException e) {
            // process has dead
        }
        if (thread == null) {
            killProcess(pid);
            return;
        }
        ProcessRecord app = null;
        try {
            IBinder token = client.getToken();
            if (token instanceof ProcessRecord) {
                app = (ProcessRecord) token;
            }
        } catch (RemoteException e) {
            // process has dead
        }
        if (app == null) {
            killProcess(pid);
            return;
        }
        try {
            final ProcessRecord record = app;
            clientBinder.linkToDeath(new IBinder.DeathRecipient() {
                @Override
                public void binderDied() {
                    clientBinder.unlinkToDeath(this, 0);
                    onProcessDead(record);
                }
            }, 0);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        app.client = client;
        app.appThread = thread;
        app.pid = pid;
        synchronized (mProcessNames) {
            mProcessNames.put(app.processName, app.vuid, app);
            mPidsSelfLocked.put(app.pid, app);
        }
    }

    private void onProcessDead(ProcessRecord record) {
        mProcessNames.remove(record.processName, record.vuid);
        mPidsSelfLocked.remove(record.pid);
        processDead(record);
        record.lock.open();
    }

    private void processDead(ProcessRecord record) {
        synchronized (mHistory) {
            Iterator<ServiceRecord> iterator = mHistory.iterator();
            while (iterator.hasNext()) {
                ServiceRecord r = iterator.next();
                if (r.process != null && r.process.pid == record.pid) {
                    iterator.remove();
                }
            }
            mMainStack.processDied(record);
        }
    }

    private int queryFreeStubProcessLocked() {
        for (int vpid = 0; vpid < VASettings.STUB_COUNT; vpid++) {
            int N = mPidsSelfLocked.size();
            boolean using = false;
            while (N-- > 0) {
                ProcessRecord r = mPidsSelfLocked.valueAt(N);
                if (r.vpid == vpid) {
                    using = true;
                    break;
                }
            }
            if (using) {
                continue;
            }
            return vpid;
        }
        return -1;
    }

    private void sendFirstLaunchBroadcast(PackageSetting ps, int userId) {
        Intent intent = new Intent(Intent.ACTION_PACKAGE_FIRST_LAUNCH, Uri.fromParts("package", ps.packageName, null));
        intent.setPackage(ps.packageName);
        intent.putExtra(Intent.EXTRA_UID, VUserHandle.getUid(ps.appId, userId));
        intent.putExtra("android.intent.extra.user_handle", userId);
        sendBroadcastAsUser(intent, null);
    }

    boolean handleStaticBroadcast(int appId, ActivityInfo info, Intent intent,
                                  PendingResultData result) {
        Intent realIntent = intent.getParcelableExtra("_VA_|_intent_");
        ComponentName component = intent.getParcelableExtra("_VA_|_component_");
        int userId = intent.getIntExtra("_VA_|_user_id_", VUserHandle.USER_NULL);
        if (realIntent == null) {
            return false;
        }
        if (userId < 0) {
            VLog.w(TAG, "Sent a broadcast without userId " + realIntent);
            return false;
        }
        int vuid = VUserHandle.getUid(userId, appId);
        return handleUserBroadcast(vuid, info, component, realIntent, result);
    }

    private boolean handleUserBroadcast(int vuid, ActivityInfo info, ComponentName component, Intent realIntent, PendingResultData result) {
        if (component != null && !ComponentUtils.toComponentName(info).equals(component)) {
            // Verify the component.
            return false;
        }
        String originAction = SpecialComponentList.unprotectAction(realIntent.getAction());
        if (originAction != null) {
            // restore to origin action.
            realIntent.setAction(originAction);
        }
        handleStaticBroadcastAsUser(vuid, info, realIntent, result);
        return true;
    }

    private void handleStaticBroadcastAsUser(int vuid, ActivityInfo info, Intent intent,
                                             PendingResultData result) {
        synchronized (this) {
            ProcessRecord r = findProcessLocked(info.processName, vuid);
            if (BROADCAST_NOT_STARTED_PKG && r == null) {
                r = startProcessIfNeedLocked(info.processName, getUserId(vuid), info.packageName);
            }
            if (r != null && r.appThread != null) {
                performScheduleReceiver(r.client, vuid, info, intent,
                        result);
            }
        }
    }

    private void performScheduleReceiver(IVClient client, int vuid, ActivityInfo info, Intent intent,
                                         PendingResultData result) {

        ComponentName componentName = ComponentUtils.toComponentName(info);
        BroadcastSystem.get().broadcastSent(vuid, info, result);
        try {
            client.scheduleReceiver(info.processName, componentName, intent, result);
        } catch (Throwable e) {
            if (result != null) {
                result.finish();
            }
        }
    }

    public void sendBroadcastAsUser(Intent intent, VUserHandle user) {
        SpecialComponentList.protectIntent(intent);
        Context context = VirtualCore.get().getContext();
        if (user != null) {
            intent.putExtra("_VA_|_user_id_", user.getIdentifier());
        }
        context.sendBroadcast(intent);
    }

    private ComponentName startServiceCommon(Intent service,
                                             boolean scheduleServiceArgs, int userId) {
        ServiceInfo serviceInfo = resolveServiceInfo(service, userId);
        if (serviceInfo == null) {
            return null;
        }
        ProcessRecord targetApp = startProcessIfNeedLocked(ComponentUtils.getProcessName(serviceInfo),
                userId,
                serviceInfo.packageName);

        if (targetApp == null) {
            VLog.e(TAG, "Unable to start new Process for : " + ComponentUtils.toComponentName(serviceInfo));
            return null;
        }
        IInterface appThread = targetApp.appThread;
        ServiceRecord r = findRecordLocked(userId, serviceInfo);
        if (r == null) {
            r = new ServiceRecord();
            r.startId = 0;
            r.activeSince = SystemClock.elapsedRealtime();
            r.process = targetApp;
            r.serviceInfo = serviceInfo;
            try {
                IApplicationThreadCompat.scheduleCreateService(appThread, r, r.serviceInfo, 0);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            addRecord(r);
        }
        r.lastActivityTime = SystemClock.uptimeMillis();
        if (scheduleServiceArgs) {
            r.startId++;
            boolean taskRemoved = serviceInfo.applicationInfo != null
                    && serviceInfo.applicationInfo.targetSdkVersion < Build.VERSION_CODES.ECLAIR;
            try {
                IApplicationThreadCompat.scheduleServiceArgs(appThread, r, taskRemoved, r.startId, 0, service);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return ComponentUtils.toComponentName(serviceInfo);
    }

    private void addRecord(ServiceRecord r) {
        mHistory.add(r);
    }

    private ServiceRecord findRecordLocked(int userId, ServiceInfo serviceInfo) {
        synchronized (mHistory) {
            for (ServiceRecord r : mHistory) {
                // If service is not created, and bindService with the flag that is
                // not BIND_AUTO_CREATE, r.process is null
                if ((r.process == null || r.process.userId == userId)
                        && ComponentUtils.isSameComponent(serviceInfo, r.serviceInfo)) {
                    return r;
                }
            }
            return null;
        }
    }

    /**
     * Should guard by {@link VActivityManagerService#mPidsSelfLocked}
     *
     * @param pid pid
     */
    public ProcessRecord findProcessLocked(int pid) {
        return mPidsSelfLocked.get(pid);
    }

    /**
     * Should guard by {@link VActivityManagerService#mProcessNames}
     *
     * @param uid vuid
     */
    public ProcessRecord findProcessLocked(String processName, int uid) {
        return mProcessNames.get(processName, uid);
    }

    @Override
    public int getUidByPid(int pid) {
        synchronized (mPidsSelfLocked) {
            ProcessRecord r = findProcessLocked(pid);
            if (r != null) {
                return r.vuid;
            }
        }
        return Process.myUid();
    }

    @Override
    public void broadcastFinish(PendingResultData res) throws RemoteException {

    }

    @Override
    public ComponentName getActivityClassForToken(int userId, IBinder token) {
        return mMainStack.getActivityClassForToken(userId, token);
    }

    @Override
    public void processRestarted(String packageName, String processName, int userId) {
        int callingPid = getCallingPid();
        int appId = VAppManagerService.get().getAppId(packageName);
        int uid = VUserHandle.getUid(userId, appId);
        synchronized (this) {
            ProcessRecord app = findProcessLocked(callingPid);
            if (app == null) {
                ApplicationInfo appInfo = VPackageManagerService.get().getApplicationInfo(packageName, 0, userId);
                appInfo.flags |= ApplicationInfo.FLAG_HAS_CODE;
                String stubProcessName = getProcessName(callingPid);
                int vpid = parseVPid(stubProcessName);
                if (vpid != -1) {
                    performStartProcessLocked(uid, vpid, appInfo, processName);
                }
            }
        }
    }

    @Override
    public void onActivityCreated(ComponentName component, ComponentName caller, IBinder token, Intent intent, String affinity, int taskId, int launchMode, int flags) {
        int pid = Binder.getCallingPid();
        ProcessRecord targetApp = findProcessLocked(pid);
        if (targetApp != null) {
            mMainStack.onActivityCreated(targetApp, component, caller, token, intent, affinity, taskId, launchMode, flags);
        }
    }

    private int parseVPid(String stubProcessName) {
        String prefix = VirtualCore.get().getHostPkg() + ":p";
        if (stubProcessName != null && stubProcessName.startsWith(prefix)) {
            try {
                return Integer.parseInt(stubProcessName.substring(prefix.length()));
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        return -1;
    }


    private String getProcessName(int pid) {
        for (ActivityManager.RunningAppProcessInfo info : am.getRunningAppProcesses()) {
            if (info.pid == pid) {
                return info.processName;
            }
        }
        return null;
    }

    private static ServiceInfo resolveServiceInfo(Intent service, int userId) {
        if (service != null) {
            ServiceInfo serviceInfo = VirtualCore.get().resolveServiceInfo(service, userId);
            if (serviceInfo != null) {
                return serviceInfo;
            }
        }
        return null;
    }

    @Override
    public void addPendingIntent(IBinder binder, String creator) {
        mPendingIntents.addPendingIntent(binder, creator);
    }

    @Override
    public void notifyBadgerChange(BadgerInfo info) {
        Intent intent = new Intent(VASettings.ACTION_BADGER_CHANGE);
        intent.putExtra("userId", info.userId);
        intent.putExtra("packageName", info.packageName);
        intent.putExtra("badgerCount", info.badgerCount);
        VirtualCore.get().getContext().sendBroadcast(intent);
    }
}
