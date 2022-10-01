// IAppRequestListener.aidl
package cn.yongye.androbox.virtual.server.interfaces;

// Declare any non-default types here with import statements

interface IAppRequestListener {
    void onRequestInstall(in String path);
    void onRequestUninstall(in String pkg);
}
