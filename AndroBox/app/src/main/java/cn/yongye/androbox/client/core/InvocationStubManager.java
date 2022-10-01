package cn.yongye.androbox.client.core;

import java.util.HashMap;
import java.util.Map;

import cn.yongye.androbox.client.hook.delegate.AppInstrumentation;
import cn.yongye.androbox.client.hook.proxies.am.ActivityManagerStub;
import cn.yongye.androbox.client.hook.proxies.am.HCallbackStub;
import cn.yongye.androbox.client.hook.proxies.pm.PackageManagerStub;
import cn.yongye.androbox.interfaces.IInjector;

public final class InvocationStubManager {

    private static InvocationStubManager sInstance = new InvocationStubManager();
    private static boolean sInit;

    private Map<Class<?>, IInjector> mInjectors = new HashMap<>(13);

    private InvocationStubManager() {
    }

    public static InvocationStubManager getInstance() {
        return sInstance;
    }

    public void injectAll() throws Throwable {
        for (IInjector injector : mInjectors.values()) {
            injector.inject();
        }
//         XXX: Lazy inject the Instrumentation,
        addInjector(AppInstrumentation.getDefault());
    }

    public <T extends IInjector> void checkEnv(Class<T> clazz) {
        IInjector IInjector = findInjector(clazz);
        if (IInjector != null && IInjector.isEnvBad()) {
            try {
                IInjector.inject();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    public <T extends IInjector> T findInjector(Class<T> clazz) {
        // noinspection unchecked
        return (T) mInjectors.get(clazz);
    }

    /**
     * @return if the InvocationStubManager has been initialized.
     */
    public boolean isInit() {
        return sInit;
    }

    public void init() throws Throwable {
        if (isInit()) {
            throw new IllegalStateException("InvocationStubManager Has been initialized.");
        }
        injectInternal();
        sInit = true;
    }

    //将系统服务的代理保存到map中。
    private void injectInternal() throws Throwable {
        //create and save proxy object for system service
        addInjector(new PackageManagerStub());
        addInjector(new ActivityManagerStub());
        addInjector(HCallbackStub.getDefault());

    }



    private void addInjector(IInjector IInjector) {
        mInjectors.put(IInjector.getClass(), IInjector);
    }
}
