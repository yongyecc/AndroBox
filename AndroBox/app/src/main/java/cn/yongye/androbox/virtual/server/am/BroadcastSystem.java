package cn.yongye.androbox.virtual.server.am;

import static android.content.Intent.FLAG_RECEIVER_REGISTERED_ONLY;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.yongye.androbox.VirtualCore;
import cn.yongye.androbox.client.env.SpecialComponentList;
import cn.yongye.androbox.helper.collection.ArrayMap;
import cn.yongye.androbox.helper.utils.VLog;
import cn.yongye.androbox.pm.parser.VPackage;
import cn.yongye.androbox.remote.PendingResultData;
import cn.yongye.androbox.virtual.server.pm.PackageSetting;
import cn.yongye.androbox.virtual.server.pm.VAppManagerService;

public class BroadcastSystem {

    private static final String TAG = BroadcastSystem.class.getSimpleName();
    /**
     * MUST < 10000.
     */
    private static final int BROADCAST_TIME_OUT = 8500;
    private static BroadcastSystem gDefault;

    private final ArrayMap<String, List<BroadcastReceiver>> mReceivers = new ArrayMap<>();
    private final Map<IBinder, BroadcastRecord> mBroadcastRecords = new HashMap<>();
    private final Context mContext;
    private final StaticScheduler mScheduler;
    private final TimeoutHandler mTimeoutHandler;
    private final VActivityManagerService mAMS;
    private final VAppManagerService mApp;

    private static final class StaticScheduler extends Handler {

        StaticScheduler(Looper looper) {
            super(looper);
        }
    }

    private BroadcastSystem(Context context, VActivityManagerService ams, VAppManagerService app) {
        this.mContext = context;
        this.mApp = app;
        this.mAMS = ams;
        HandlerThread broadcastThread = new HandlerThread("BroadcastThread");
        HandlerThread anrThread = new HandlerThread("BroadcastAnrThread");
        broadcastThread.start();
        anrThread.start();
        mScheduler = new StaticScheduler(broadcastThread.getLooper());
        mTimeoutHandler = new TimeoutHandler(anrThread.getLooper());
//        fuckHuaWeiVerifier();
    }

    private final class TimeoutHandler extends Handler {

        TimeoutHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            IBinder token = (IBinder) msg.obj;
            BroadcastRecord r = mBroadcastRecords.remove(token);
            if (r != null) {
                VLog.w(TAG, "Broadcast timeout, cancel to dispatch it.");
                r.pendingResult.finish();
            }
        }
    }

    public static void attach(VActivityManagerService ams, VAppManagerService app) {
        if (gDefault != null) {
            throw new IllegalStateException();
        }
        gDefault = new BroadcastSystem(VirtualCore.get().getContext(), ams, app);
    }

    public static BroadcastSystem get() {
        return gDefault;
    }

    void broadcastSent(int vuid, ActivityInfo receiverInfo, PendingResultData res) {
        BroadcastRecord record = new BroadcastRecord(vuid, receiverInfo, res);
        synchronized (mBroadcastRecords) {
            mBroadcastRecords.put(res.mToken, record);
        }
        Message msg = new Message();
        msg.obj = res.mToken;
        mTimeoutHandler.sendMessageDelayed(msg, BROADCAST_TIME_OUT);
    }

    private static final class BroadcastRecord {
        int vuid;
        ActivityInfo receiverInfo;
        PendingResultData pendingResult;

        BroadcastRecord(int vuid, ActivityInfo receiverInfo, PendingResultData pendingResult) {
            this.vuid = vuid;
            this.receiverInfo = receiverInfo;
            this.pendingResult = pendingResult;
        }
    }

    public void startApp(VPackage p) {
        PackageSetting setting = (PackageSetting) p.mExtras;
        for (VPackage.ActivityComponent receiver : p.receivers) {
            ActivityInfo info = receiver.info;
            List<BroadcastReceiver> receivers = mReceivers.get(p.packageName);
            if (receivers == null) {
                receivers = new ArrayList<>();
                mReceivers.put(p.packageName, receivers);
            }
            String componentAction = String.format("_VA_%s_%s", info.packageName, info.name);
            IntentFilter componentFilter = new IntentFilter(componentAction);
            BroadcastReceiver r = new StaticBroadcastReceiver(setting.appId, info, componentFilter);
            mContext.registerReceiver(r, componentFilter, null, mScheduler);
            receivers.add(r);
            for (VPackage.ActivityIntentInfo ci : receiver.intents) {
                IntentFilter cloneFilter = new IntentFilter(ci.filter);
                SpecialComponentList.protectIntentFilter(cloneFilter);
                r = new StaticBroadcastReceiver(setting.appId, info, cloneFilter);
                mContext.registerReceiver(r, cloneFilter, null, mScheduler);
                receivers.add(r);
            }
        }
    }

    private final class StaticBroadcastReceiver extends BroadcastReceiver {
        private int appId;
        private ActivityInfo info;
        @SuppressWarnings("unused")
        private IntentFilter filter;

        private StaticBroadcastReceiver(int appId, ActivityInfo info, IntentFilter filter) {
            this.appId = appId;
            this.info = info;
            this.filter = filter;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
//            if (mApp.isBooting()) {
//                return;
//            }
            if ((intent.getFlags() & FLAG_RECEIVER_REGISTERED_ONLY) != 0 || isInitialStickyBroadcast()) {
                return;
            }
            String privilegePkg = intent.getStringExtra("_VA_|_privilege_pkg_");
            if (privilegePkg != null && !info.packageName.equals(privilegePkg)) {
                return;
            }
            PendingResult result = goAsync();
            if (!mAMS.handleStaticBroadcast(appId, info, intent, new PendingResultData(result))) {
                result.finish();
            }
        }
    }
}
