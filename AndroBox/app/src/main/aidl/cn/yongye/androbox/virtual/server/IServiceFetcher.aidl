// IServiceFetcher.aidl
package cn.yongye.androbox.virtual.server;

// Declare any non-default types here with import statements

interface IServiceFetcher {

    IBinder getService(String name);
    void addService(String name,in IBinder service);
    void removeService(String name);
}