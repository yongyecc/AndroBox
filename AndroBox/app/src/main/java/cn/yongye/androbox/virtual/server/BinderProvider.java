package cn.yongye.androbox.virtual.server;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.BundleCompat;

import cn.yongye.androbox.VirtualCore;
import cn.yongye.androbox.helper.ipcbus.IPCBus;
import cn.yongye.androbox.virtual.server.am.BroadcastSystem;
import cn.yongye.androbox.virtual.server.am.VActivityManagerService;
import cn.yongye.androbox.virtual.server.interfaces.IActivityManager;
import cn.yongye.androbox.virtual.server.interfaces.IAppManager;
import cn.yongye.androbox.virtual.server.interfaces.IPackageManager;
import cn.yongye.androbox.virtual.server.pm.VAppManagerService;
import cn.yongye.androbox.virtual.server.pm.VPackageManagerService;

public final class BinderProvider extends ContentProvider {

    private final ServiceFetcher mServiceFetcher = new ServiceFetcher();

    @Override
    public boolean onCreate() {
        Context context = getContext();
//        DaemonService.startup(context);
        if (!VirtualCore.get().isStartup()) {
            return true;
        }

        VPackageManagerService.systemReady();
        // register virtual system service: VPackageManagerService
        IPCBus.register(IPackageManager.class, VPackageManagerService.get());
        VActivityManagerService.systemReady(context);
        IPCBus.register(IActivityManager.class, VActivityManagerService.get());
        VAppManagerService.systemReady();
        IPCBus.register(IAppManager.class, VAppManagerService.get());
        BroadcastSystem.attach(VActivityManagerService.get(), VAppManagerService.get());
        return true;
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        if ("@".equals(method)) {
            Bundle bundle = new Bundle();
            BundleCompat.putBinder(bundle, "_VA_|_binder_", mServiceFetcher);
            return bundle;
        }
        if ("register".equals(method)) {

        }
        return null;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] strings, @Nullable String s, @Nullable String[] strings1, @Nullable String s1) {
        return null;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String s, @Nullable String[] strings) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues contentValues, @Nullable String s, @Nullable String[] strings) {
        return 0;
    }

    private class ServiceFetcher extends IServiceFetcher.Stub {
        @Override
        public IBinder getService(String name) throws RemoteException {
            if (name != null) {
                return ServiceCache.getService(name);
            }
            return null;
        }

        @Override
        public void addService(String name, IBinder service) throws RemoteException {
            if (name != null && service != null) {
                ServiceCache.addService(name, service);
            }
        }

        @Override
        public void removeService(String name) throws RemoteException {
            if (name != null) {
                ServiceCache.removeService(name);
            }
        }
    }
}
