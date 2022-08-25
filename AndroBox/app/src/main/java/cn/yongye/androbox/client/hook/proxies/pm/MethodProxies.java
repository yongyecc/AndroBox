package cn.yongye.androbox.client.hook.proxies.pm;

import android.content.pm.PackageInfo;

import java.lang.reflect.Method;

import cn.yongye.androbox.client.hook.base.MethodProxy;
import cn.yongye.androbox.client.ipc.VPackageManager;

@SuppressWarnings("unused")
public class MethodProxies {

    static class IsPackageAvailable extends MethodProxy {

        @Override
        public String getMethodName() {
            return "isPackageAvailable";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            String pkgName = (String) args[0];
            if (isAppPkg(pkgName)) {
                return true;
            }
            return method.invoke(who, args);
        }

//        @Override
//        public boolean isEnable() {
//            return isAppProcess();
//        }
    }

    static final class GetPackageInfo extends MethodProxy {

        @Override
        public String getMethodName() {
            return "getPackageInfo";
        }

        @Override
        public boolean beforeCall(Object who, Method method, Object... args) {
            return args != null && args[0] != null;
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            String pkg = (String) args[0];
            int flags = (int) args[1];
//            int userId = VUserHandle.myUserId();
            PackageInfo packageInfo = VPackageManager.get().getPackageInfo(pkg, flags, 110);
            if (packageInfo != null) {
                return packageInfo;
            }
            packageInfo = (PackageInfo) method.invoke(who, args);
            if (packageInfo != null) {
//                if (isVisiblePackage(packageInfo.applicationInfo)) {
//                    return packageInfo;
//                }
            }
            return null;
        }

    }
}
