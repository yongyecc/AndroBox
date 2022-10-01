package cn.yongye.androbox.virtual.server.interfaces;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.RemoteException;

import java.util.List;

public interface IPackageManager {

    PackageInfo getPackageInfo(String packageName, int flags, int userId) throws RemoteException;

    ActivityInfo getActivityInfo(ComponentName componentName, int flags, int userId) throws RemoteException;

    ResolveInfo resolveIntent(Intent intent, String resolvedType, int flags, int userId) throws RemoteException;

    ResolveInfo resolveService(Intent intent, String resolvedType, int flags, int userId) throws RemoteException;

    List<ResolveInfo> queryIntentActivities(Intent intent, String resolvedType, int flags, int userId) throws RemoteException;

    List<ResolveInfo> queryIntentServices(Intent intent, String resolvedType, int flags, int userId) throws RemoteException;

    ServiceInfo getServiceInfo(ComponentName componentName, int flags, int userId) throws RemoteException;

    ApplicationInfo getApplicationInfo(String packageName, int flags, int userId) throws RemoteException;
}
