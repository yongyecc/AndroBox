package cn.yongye.androbox.virtual.server.interfaces;

import android.os.RemoteException;

import cn.yongye.androbox.virtual.server.pm.InstalledAppInfo;

public interface IAppManager {

    InstalledAppInfo getInstalledAppInfo(String pkg, int flags) throws RemoteException;

    IAppRequestListener getAppRequestListener() throws RemoteException;
}
