package cn.yongye.androbox.pm.parser;

import static android.os.Build.VERSION_CODES.JELLY_BEAN;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;
import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static android.os.Build.VERSION_CODES.LOLLIPOP_MR1;
import static android.os.Build.VERSION_CODES.M;
import static android.os.Build.VERSION_CODES.N;

import android.content.pm.PackageParser.Package;
import android.app.Activity;
import android.app.Service;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageParser;
import android.content.pm.ProviderInfo;
import android.content.pm.ServiceInfo;
import android.os.Build;

import java.io.File;
import java.security.Provider;
import android.os.Process;
import cn.yongye.androbox.VirtualCore;
import mirror.android.content.pm.PackageParserMarshmallow;
import mirror.android.content.pm.PackageParserNougat;
import mirror.android.content.pm.PackageUserState;

public class PackageParserCompat {

    public static final int[] GIDS = VirtualCore.get().getGids();
    private static final int API_LEVEL = Build.VERSION.SDK_INT;
    private static final int myUserId = Process.myUid();
    private static final Object sUserState = API_LEVEL >= JELLY_BEAN_MR1 ? PackageUserState.ctor.newInstance() : null;


    public static PackageParser createParser(File packageFile) {
        if (API_LEVEL >= M) {
            return PackageParserMarshmallow.ctor.newInstance();
        } else if (API_LEVEL >= LOLLIPOP_MR1) {
//            return PackageParserLollipop22.ctor.newInstance();
        } else if (API_LEVEL >= LOLLIPOP) {
//            return PackageParserLollipop.ctor.newInstance();
        } else if (API_LEVEL >= JELLY_BEAN_MR1) {
//            return PackageParserJellyBean17.ctor.newInstance(packageFile.getAbsolutePath());
        } else if (API_LEVEL >= JELLY_BEAN) {
//            return PackageParserJellyBean.ctor.newInstance(packageFile.getAbsolutePath());
        } else {
            return mirror.android.content.pm.PackageParser.ctor.newInstance(packageFile.getAbsolutePath());
        }
        return null;
    }

    public static Package parsePackage(PackageParser parser, File packageFile, int flags) throws Throwable {
        if (API_LEVEL >= M) {
            return PackageParserMarshmallow.parsePackage.callWithException(parser, packageFile, flags);
        } else if (API_LEVEL >= LOLLIPOP_MR1) {
//            return PackageParserLollipop22.parsePackage.callWithException(parser, packageFile, flags);
        } else if (API_LEVEL >= LOLLIPOP) {
//            return PackageParserLollipop.parsePackage.callWithException(parser, packageFile, flags);
        } else if (API_LEVEL >= JELLY_BEAN_MR1) {
//            return PackageParserJellyBean17.parsePackage.callWithException(parser, packageFile, null,
//                    new DisplayMetrics(), flags);
        } else if (API_LEVEL >= JELLY_BEAN) {
//            return PackageParserJellyBean.parsePackage.callWithException(parser, packageFile, null,
//                    new DisplayMetrics(), flags);
        } else {
//            return mirror.android.content.pm.PackageParser.parsePackage.callWithException(parser, packageFile, null,
//                    new DisplayMetrics(), flags);
        }
        return null;
    }

    public static ServiceInfo generateServiceInfo(Service service, int flags) {
        if (API_LEVEL >= M) {
            return PackageParserMarshmallow.generateServiceInfo.call(service, flags, sUserState, myUserId);
        } else if (API_LEVEL >= LOLLIPOP_MR1) {
//            return PackageParserLollipop22.generateServiceInfo.call(service, flags, sUserState, myUserId);
        } else if (API_LEVEL >= LOLLIPOP) {
//            return PackageParserLollipop.generateServiceInfo.call(service, flags, sUserState, myUserId);
        } else if (API_LEVEL >= JELLY_BEAN_MR1) {
//            return PackageParserJellyBean17.generateServiceInfo.call(service, flags, sUserState, myUserId);
        } else if (API_LEVEL >= JELLY_BEAN) {
            //           return PackageParserJellyBean.generateServiceInfo.call(service, flags, false, 1, myUserId);
        } else {
            return mirror.android.content.pm.PackageParser.generateServiceInfo.call(service, flags);
        }
        return null;
    }

    public static ApplicationInfo generateApplicationInfo(Package p, int flags) {
        if (API_LEVEL >= M) {
            return PackageParserMarshmallow.generateApplicationInfo.call(p, flags, sUserState);
        } else if (API_LEVEL >= LOLLIPOP_MR1) {
//            return PackageParserLollipop22.generateApplicationInfo.call(p, flags, sUserState);
        } else if (API_LEVEL >= LOLLIPOP) {
//            return PackageParserLollipop.generateApplicationInfo.call(p, flags, sUserState);
        } else if (API_LEVEL >= JELLY_BEAN_MR1) {
//            return PackageParserJellyBean17.generateApplicationInfo.call(p, flags, sUserState);
        } else if (API_LEVEL >= JELLY_BEAN) {
//            return PackageParserJellyBean.generateApplicationInfo.call(p, flags, false, 1);
        } else {
//            return mirror.android.content.pm.PackageParser.generateApplicationInfo.call(p, flags);
        }
        return null;
    }

    public static ActivityInfo generateActivityInfo(Activity activity, int flags) {
        if (API_LEVEL >= M) {
            return PackageParserMarshmallow.generateActivityInfo.call(activity, flags, sUserState, myUserId);
        } else if (API_LEVEL >= LOLLIPOP_MR1) {
//            return PackageParserLollipop22.generateActivityInfo.call(activity, flags, sUserState, myUserId);
        } else if (API_LEVEL >= LOLLIPOP) {
//            return PackageParserLollipop.generateActivityInfo.call(activity, flags, sUserState, myUserId);
        } else if (API_LEVEL >= JELLY_BEAN_MR1) {
//            return PackageParserJellyBean17.generateActivityInfo.call(activity, flags, sUserState, myUserId);
        } else if (API_LEVEL >= JELLY_BEAN) {
//            return PackageParserJellyBean.generateActivityInfo.call(activity, flags, false, 1, myUserId);
        } else {
            return mirror.android.content.pm.PackageParser.generateActivityInfo.call(activity, flags);
        }
        return null;
    }

    public static ProviderInfo generateProviderInfo(Provider provider, int flags) {
        if (API_LEVEL >= M) {
            return PackageParserMarshmallow.generateProviderInfo.call(provider, flags, sUserState, myUserId);
        } else if (API_LEVEL >= LOLLIPOP_MR1) {
//            return PackageParserLollipop22.generateProviderInfo.call(provider, flags, sUserState, myUserId);
        } else if (API_LEVEL >= LOLLIPOP) {
//            return PackageParserLollipop.generateProviderInfo.call(provider, flags, sUserState, myUserId);
        } else if (API_LEVEL >= JELLY_BEAN_MR1) {
//            return PackageParserJellyBean17.generateProviderInfo.call(provider, flags, sUserState, myUserId);
        } else if (API_LEVEL >= JELLY_BEAN) {
//            return PackageParserJellyBean.generateProviderInfo.call(provider, flags, false, 1, myUserId);
        } else {
            return mirror.android.content.pm.PackageParser.generateProviderInfo.call(provider, flags);
        }
        return null;
    }

    public static PackageInfo generatePackageInfo(Package p, int flags, long firstInstallTime, long lastUpdateTime) {
        if (API_LEVEL >= M) {
            return PackageParserMarshmallow.generatePackageInfo.call(p, GIDS, flags, firstInstallTime, lastUpdateTime,
                    null, sUserState);
        }
//        } else if (API_LEVEL >= LOLLIPOP) {
//            if (PackageParserLollipop22.generatePackageInfo != null) {
//                return PackageParserLollipop22.generatePackageInfo.call(p, GIDS, flags, firstInstallTime, lastUpdateTime,
//                        null, sUserState);
//            } else {
//                return PackageParserLollipop.generatePackageInfo.call(p, GIDS, flags, firstInstallTime, lastUpdateTime,
//                        null, sUserState);
//            }
//        } else if (API_LEVEL >= JELLY_BEAN_MR1) {
//            return PackageParserJellyBean17.generatePackageInfo.call(p, GIDS, flags, firstInstallTime, lastUpdateTime,
//                    null, sUserState);
//        } else if (API_LEVEL >= JELLY_BEAN) {
//            return PackageParserJellyBean.generatePackageInfo.call(p, GIDS, flags, firstInstallTime, lastUpdateTime,
//                    null);
//        } else {
//            return mirror.android.content.pm.PackageParser.generatePackageInfo.call(p, GIDS, flags, firstInstallTime,
//                    lastUpdateTime);
//        }
        return null;
    }

    public static void collectCertificates(PackageParser parser, Package p, int flags) throws Throwable {
        if (API_LEVEL >= N) {
            PackageParserNougat.collectCertificates.callWithException(p, flags);
        }
        return;
//        } else if (API_LEVEL >= M) {
//            PackageParserMarshmallow.collectCertificates.callWithException(parser, p, flags);
//        } else if (API_LEVEL >= LOLLIPOP_MR1) {
//            PackageParserLollipop22.collectCertificates.callWithException(parser, p, flags);
//        } else if (API_LEVEL >= LOLLIPOP) {
//            PackageParserLollipop.collectCertificates.callWithException(parser, p, flags);
//        } else if (API_LEVEL >= JELLY_BEAN_MR1) {
//            PackageParserJellyBean17.collectCertificates.callWithException(parser, p, flags);
//        } else if (API_LEVEL >= JELLY_BEAN) {
//            PackageParserJellyBean.collectCertificates.callWithException(parser, p, flags);
//        } else {
//            mirror.android.content.pm.PackageParser.collectCertificates.call(parser, p, flags);
//        }
    }
}
