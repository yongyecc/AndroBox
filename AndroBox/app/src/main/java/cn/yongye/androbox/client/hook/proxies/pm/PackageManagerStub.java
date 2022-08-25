package cn.yongye.androbox.client.hook.proxies.pm;

import android.os.Build;
import android.os.IInterface;

import androidx.core.os.BuildCompat;

import cn.yongye.androbox.client.hook.base.Inject;
import cn.yongye.androbox.client.hook.base.MethodInvocationProxy;
import cn.yongye.androbox.client.hook.base.MethodInvocationStub;
import mirror.android.app.ActivityThread;

@Inject(MethodProxies.class)
public final class PackageManagerStub extends MethodInvocationProxy<MethodInvocationStub<IInterface>> {

    //
    public PackageManagerStub() {
        super(new MethodInvocationStub<>(ActivityThread.sPackageManager.get()));
    }

    @Override
    protected void onBindMethods() {
        super.onBindMethods();
//        addMethodProxy(new ResultStaticMethodProxy("addPermissionAsync", true));
//        addMethodProxy(new ResultStaticMethodProxy("addPermission", true));
//        addMethodProxy(new ResultStaticMethodProxy("performDexOpt", true));
//        addMethodProxy(new ResultStaticMethodProxy("performDexOptIfNeeded", false));
//        addMethodProxy(new ResultStaticMethodProxy("performDexOptSecondary", true));
//        addMethodProxy(new ResultStaticMethodProxy("addOnPermissionsChangeListener", 0));
//        addMethodProxy(new ResultStaticMethodProxy("removeOnPermissionsChangeListener", 0));
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//            addMethodProxy(new ResultStaticMethodProxy("checkPackageStartable", 0));
//        }
//        if (BuildCompat.isOreo()) {
//            addMethodProxy(new ResultStaticMethodProxy("notifyDexLoad", 0));
//            addMethodProxy(new ResultStaticMethodProxy("notifyPackageUse", 0));
//            addMethodProxy(new ResultStaticMethodProxy("setInstantAppCookie", false));
//            addMethodProxy(new ResultStaticMethodProxy("isInstantApp", false));
//        }

    }

    @Override
    public void inject() throws Throwable {
        //get dynamic proxy object
        final IInterface hookedPM = getInvocationStub().getProxyInterface();
        //replace proxy object into system
        ActivityThread.sPackageManager.set(hookedPM);

//        BinderInvocationStub pmHookBinder = new BinderInvocationStub(getInvocationStub().getBaseInterface());
//        pmHookBinder.copyMethodProxies(getInvocationStub());
//        pmHookBinder.replaceService("package");
    }

    @Override
    public boolean isEnvBad() {
        return getInvocationStub().getProxyInterface() != ActivityThread.sPackageManager.get();
    }
}
