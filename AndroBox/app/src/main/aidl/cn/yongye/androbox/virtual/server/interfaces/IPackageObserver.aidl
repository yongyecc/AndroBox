// IPackageObserver.aidl
package cn.yongye.androbox.virtual.server.interfaces;

// Declare any non-default types here with import statements


interface IPackageObserver {
    void onPackageInstalled(in String packageName);
    void onPackageUninstalled(in String packageName);
    void onPackageInstalledAsUser(in int userId, in String packageName);
    void onPackageUninstalledAsUser(in int userId, in String packageName);
}