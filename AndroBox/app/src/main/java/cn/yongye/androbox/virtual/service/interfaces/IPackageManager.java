package cn.yongye.androbox.virtual.service.interfaces;

import android.content.pm.PackageInfo;
import android.os.RemoteException;

public interface IPackageManager {

    PackageInfo getPackageInfo(String packageName, int flags, int userId) throws RemoteException;
}
