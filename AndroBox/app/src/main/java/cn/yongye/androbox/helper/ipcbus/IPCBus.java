package cn.yongye.androbox.helper.ipcbus;

import android.os.IBinder;

import java.lang.reflect.Proxy;

public class IPCBus {

    private static IServerCache sCache;

    public static void initialize(IServerCache cache) {
        sCache = cache;
    }

    private static void checkInitialized() {
        if (sCache == null) {
            throw new IllegalStateException("please call initialize() at first.");
        }
    }

    public static void register(Class<?> interfaceClass, Object server) {
        checkInitialized();
        //make interface method to IPCMethod of virtual service
        ServerInterface serverInterface = new ServerInterface(interfaceClass);
        //create binder object,
        TransformBinder binder = new TransformBinder(serverInterface, server);
        //save binder of virtual system service
        sCache.join(serverInterface.getInterfaceName(), binder);
    }

    public static <T> T get(Class<?> interfaceClass) {
        checkInitialized();
        //make interface method to IPCMethod of virtual service
        ServerInterface serverInterface = new ServerInterface(interfaceClass);
        //get binder object
        IBinder binder = sCache.query(serverInterface.getInterfaceName());
        if (binder == null) {
            return null;
        }
        //noinspection unchecked
        return (T) Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class[]{interfaceClass}, new IPCInvocationBridge(serverInterface, binder));
    }
}
