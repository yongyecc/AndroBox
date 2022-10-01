package cn.yongye.androbox.virtual.server.pm;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.RemoteException;
import android.util.Log;

import androidx.core.util.ObjectsCompat;

import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import cn.yongye.androbox.helper.utils.ComponentFixer;
import cn.yongye.androbox.pm.parser.PackageParserEx;
import cn.yongye.androbox.pm.parser.VPackage;
import cn.yongye.androbox.virtual.server.interfaces.IPackageManager;

public class VPackageManagerService implements IPackageManager {

    public String TAG = "VPackageManagerService";
    static final Comparator<ResolveInfo> sResolvePrioritySorter = new Comparator<ResolveInfo>() {
        public int compare(ResolveInfo r1, ResolveInfo r2) {
            int v1 = r1.priority;
            int v2 = r2.priority;
            if (v1 != v2) {
                return (v1 > v2) ? -1 : 1;
            }
            v1 = r1.preferredOrder;
            v2 = r2.preferredOrder;
            if (v1 != v2) {
                return (v1 > v2) ? -1 : 1;
            }
            if (r1.isDefault != r2.isDefault) {
                return r1.isDefault ? -1 : 1;
            }
            v1 = r1.match;
            v2 = r2.match;
            if (v1 != v2) {
                return (v1 > v2) ? -1 : 1;
            }
            return 0;
        }
    };

    private static final AtomicReference<VPackageManagerService> gService = new AtomicReference<>();

    private final ActivityIntentResolver mActivities = new ActivityIntentResolver();
    private final ServiceIntentResolver mServices = new ServiceIntentResolver();
    private final ActivityIntentResolver mReceivers = new ActivityIntentResolver();
//    private final ProviderIntentResolver mProviders = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ? new ProviderIntentResolver() : null;

    private final HashMap<ComponentName, VPackage.ProviderComponent> mProvidersByComponent = new HashMap<>();

    private final HashMap<String, VPackage.PermissionComponent> mPermissions = new HashMap<>();
    private final HashMap<String, VPackage.PermissionGroupComponent> mPermissionGroups = new HashMap<>();
    private final HashMap<String, VPackage.ProviderComponent> mProvidersByAuthority = new HashMap<>();

    private final Map<String, VPackage> mPackages = PackageCacheManager.PACKAGE_CACHE;

    public static VPackageManagerService get() {
        return gService.get();
    }

    public static void systemReady() {
        VPackageManagerService instance = new VPackageManagerService();
//        new VUserManagerService(VirtualCore.get().getContext(), instance, new char[0], instance.mPackages);
        gService.set(instance);
    }

    @Override
    public PackageInfo getPackageInfo(String packageName, int flags, int userId) throws RemoteException {
//        checkUserId(userId);
        synchronized (mPackages) {
            VPackage p = mPackages.get(packageName);
            if (p != null) {
                PackageSetting ps = (PackageSetting) p.mExtras;
                return generatePackageInfo(p, ps, flags, userId);
            }
        }
        return null;
    }

    @Override
    public ActivityInfo getActivityInfo(ComponentName component, int flags, int userId) {
//        checkUserId(userId);
        flags = updateFlagsNought(flags);
        synchronized (mPackages) {
            VPackage p = mPackages.get(component.getPackageName());
            if (p != null) {
                PackageSetting ps = (PackageSetting) p.mExtras;
                VPackage.ActivityComponent a = mActivities.mActivities.get(component);
                if (a != null) {
                    ActivityInfo activityInfo = PackageParserEx.generateActivityInfo(a, flags, ps.readUserState(userId), userId);
                    ComponentFixer.fixComponentInfo(ps, activityInfo, userId);
                    return activityInfo;
                }
            }
        }
        return null;
    }

    private int updateFlagsNought(int flags) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return flags;
        }
        if ((flags & (PackageManager.MATCH_DIRECT_BOOT_UNAWARE
                | PackageManager.MATCH_DIRECT_BOOT_AWARE)) != 0) {
            // Caller expressed an explicit opinion about what encryption
            // aware/unaware components they want to see, so fall through and
            // give them what they want
        } else {
            // Caller expressed no opinion, so match based on user state
            flags |= PackageManager.MATCH_DIRECT_BOOT_AWARE | PackageManager.MATCH_DIRECT_BOOT_UNAWARE;
        }
        return flags;
    }

    @Override
    public ResolveInfo resolveIntent(Intent intent, String resolvedType, int flags, int userId) {
//        checkUserId(userId);
        flags = updateFlagsNought(flags);
        List<ResolveInfo> query = queryIntentActivities(intent, resolvedType, flags, 0);
        return chooseBestActivity(intent, resolvedType, flags, query);
    }

    private ResolveInfo chooseBestActivity(Intent intent, String resolvedType, int flags, List<ResolveInfo> query) {
        if (query != null) {
            final int N = query.size();
            if (N == 1) {
                return query.get(0);
            } else if (N > 1) {
                // If there is more than one activity with the same priority,
                // then let the user decide between them.
                ResolveInfo r0 = query.get(0);
                ResolveInfo r1 = query.get(1);
                // If the first activity has a higher priority, or a different
                // default, then it is always desireable to pick it.
                if (r0.priority != r1.priority || r0.preferredOrder != r1.preferredOrder
                        || r0.isDefault != r1.isDefault) {
                    return query.get(0);
                }
                // If we have saved a preference for a preferred activity for
                // this Intent, use that.

                //从候选列表中查找一个最合适的，如果候选列表没有最合适的返回null
                //然后从系统中查找合适的打开intent
                ResolveInfo ri = findPreferredActivity(intent, resolvedType,
                        flags, query, r0.priority);
                //noinspection ConstantConditions
                if (ri != null) {
                    return ri;
                }

                return null;
            }
        }
        return null;
    }

    private ResolveInfo findPreferredActivity(Intent intent, String resolvedType, int flags, List<ResolveInfo> query, int priority) {

        try {
            Class clazz = Class.forName("com.virtual.helper.VALibHelper");
            Method method = clazz.getDeclaredMethod("findPreferredActivity", Intent.class, String.class, int.class, List.class, int.class);
            return (ResolveInfo) method.invoke(null, intent, resolvedType, flags, query, priority);
        } catch (Exception e) {
            e.printStackTrace();
            return query.get(0);
        }

//        return null;
    }

    @Override
    public ResolveInfo resolveService(Intent intent, String resolvedType, int flags, int userId) {
//        checkUserId(userId);
        flags = updateFlagsNought(flags);
        List<ResolveInfo> query = queryIntentServices(intent, resolvedType, flags, userId);
        if (query != null) {
            if (query.size() >= 1) {
                // If there is more than one service with the same priority,
                // just arbitrarily pick the first one.
                return query.get(0);
            }
        }
        return null;
    }

    @Override
    public List<ResolveInfo> queryIntentActivities(Intent intent, String resolvedType, int flags, int userId) {
//        checkUserId(userId);
        flags = updateFlagsNought(flags);
        ComponentName comp = intent.getComponent();
        if (comp == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                if (intent.getSelector() != null) {
                    intent = intent.getSelector();
                    comp = intent.getComponent();
                }
            }
        }
        if (comp != null) {
            final List<ResolveInfo> list = new ArrayList<ResolveInfo>(1);
            final ActivityInfo ai = getActivityInfo(comp, flags, userId);
            if (ai != null) {
                final ResolveInfo ri = new ResolveInfo();
                ri.activityInfo = ai;
                list.add(ri);
            }
            return list;
        }

        // reader
        synchronized (mPackages) {
            final String pkgName = intent.getPackage();
            if (pkgName == null) {
                return mActivities.queryIntent(intent, resolvedType, flags, userId);
            }
            final VPackage pkg = mPackages.get(pkgName);
            if (pkg != null) {
                return mActivities.queryIntentForPackage(intent, resolvedType, flags, pkg.activities, userId);
            }
            return Collections.emptyList();
        }
    }

    @Override
    public List<ResolveInfo> queryIntentServices(Intent intent, String resolvedType, int flags, int userId) {
//        checkUserId(userId);
        flags = updateFlagsNought(flags);
        ComponentName comp = intent.getComponent();
        if (comp == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                if (intent.getSelector() != null) {
                    intent = intent.getSelector();
                    comp = intent.getComponent();
                }
            }
        }
        if (comp != null) {
            final List<ResolveInfo> list = new ArrayList<ResolveInfo>(1);
            final ServiceInfo si = getServiceInfo(comp, flags, userId);
            if (si != null) {
                final ResolveInfo ri = new ResolveInfo();
                ri.serviceInfo = si;
                list.add(ri);
            }
            return list;
        }

        // reader
        synchronized (mPackages) {
            String pkgName = intent.getPackage();
            if (pkgName == null) {
                return mServices.queryIntent(intent, resolvedType, flags, userId);
            }
            final VPackage pkg = mPackages.get(pkgName);
            if (pkg != null) {
                return mServices.queryIntentForPackage(intent, resolvedType, flags, pkg.services, userId);
            }
            return Collections.emptyList();
        }
    }

    @Override
    public ServiceInfo getServiceInfo(ComponentName component, int flags, int userId) {
//        checkUserId(userId);
        flags = updateFlagsNought(flags);
        synchronized (mPackages) {
            VPackage p = mPackages.get(component.getPackageName());
            if (p != null) {
                PackageSetting ps = (PackageSetting) p.mExtras;
                VPackage.ServiceComponent s = mServices.mServices.get(component);
                if (s != null) {
                    ServiceInfo serviceInfo = PackageParserEx.generateServiceInfo(s, flags, ps.readUserState(userId), userId);
                    ComponentFixer.fixComponentInfo(ps, serviceInfo, userId);
                    return serviceInfo;
                }
            }
        }
        return null;
    }

    @Override
    public ApplicationInfo getApplicationInfo(String packageName, int flags, int userId) {
//        checkUserId(userId);
        flags = updateFlagsNought(flags);
        synchronized (mPackages) {
            VPackage p = mPackages.get(packageName);
            if (p != null) {
                PackageSetting ps = (PackageSetting) p.mExtras;
                return PackageParserEx.generateApplicationInfo(p, flags, ps.readUserState(userId), userId);
            }
        }
        return null;
    }

    private PackageInfo generatePackageInfo(VPackage p, PackageSetting ps, int flags, int userId) {

        PackageInfo packageInfo = PackageParserEx.generatePackageInfo(p, flags,
                ps.firstInstallTime, ps.lastUpdateTime, ps.readUserState(userId), userId);
        if (packageInfo != null) {
            return packageInfo;
        }
        return null;
    }

    void analyzePackageLocked(VPackage pkg) {
        int N = pkg.activities.size();
        for (int i = 0; i < N; i++) {
            VPackage.ActivityComponent a = pkg.activities.get(i);
            if (a.info.processName == null) {
                a.info.processName = a.info.packageName;
            }
            mActivities.addActivity(a, "activity");
        }
        N = pkg.services.size();
        for (int i = 0; i < N; i++) {
            VPackage.ServiceComponent a = pkg.services.get(i);
            if (a.info.processName == null) {
                a.info.processName = a.info.packageName;
            }
            mServices.addService(a);
        }
        N = pkg.receivers.size();
        for (int i = 0; i < N; i++) {
            VPackage.ActivityComponent a = pkg.receivers.get(i);
            if (a.info.processName == null) {
                a.info.processName = a.info.packageName;
            }
            mReceivers.addActivity(a, "receiver");
        }

        N = pkg.providers.size();
        for (int i = 0; i < N; i++) {
            VPackage.ProviderComponent p = pkg.providers.get(i);
            if (p.info.processName == null) {
                p.info.processName = p.info.packageName;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//                mProviders.addProvider(p);
            }
            String names[] = p.info.authority.split(";");
            for (String name : names) {
                if (!mProvidersByAuthority.containsKey(name)) {
                    mProvidersByAuthority.put(name, p);
                }
            }
            mProvidersByComponent.put(p.getComponentName(), p);
        }

        N = pkg.permissions.size();
        for (int i = 0; i < N; i++) {
            VPackage.PermissionComponent permission = pkg.permissions.get(i);
            mPermissions.put(permission.className, permission);
        }
        N = pkg.permissionGroups.size();
        for (int i = 0; i < N; i++) {
            VPackage.PermissionGroupComponent group = pkg.permissionGroups.get(i);
            mPermissionGroups.put(group.className, group);
        }
    }


    private final class ActivityIntentResolver extends IntentResolver<VPackage.ActivityIntentInfo, ResolveInfo> {
        // Keys are String (activity class name), values are Activity.
        private final HashMap<ComponentName, VPackage.ActivityComponent> mActivities = new HashMap<>();
        private int mFlags;

        public List<ResolveInfo> queryIntent(Intent intent, String resolvedType, boolean defaultOnly, int userId) {
            mFlags = defaultOnly ? PackageManager.MATCH_DEFAULT_ONLY : 0;
            return super.queryIntent(intent, resolvedType, defaultOnly, userId);
        }

        List<ResolveInfo> queryIntent(Intent intent, String resolvedType, int flags, int userId) {
            mFlags = flags;
            return super.queryIntent(intent, resolvedType, (flags & PackageManager.MATCH_DEFAULT_ONLY) != 0, userId);
        }

        List<ResolveInfo> queryIntentForPackage(Intent intent, String resolvedType, int flags,
                                                ArrayList<VPackage.ActivityComponent> packageActivities, int userId) {
            if (packageActivities == null) {
                return null;
            }
            mFlags = flags;
            final boolean defaultOnly = (flags & PackageManager.MATCH_DEFAULT_ONLY) != 0;
            final int N = packageActivities.size();
            ArrayList<VPackage.ActivityIntentInfo[]> listCut = new ArrayList<VPackage.ActivityIntentInfo[]>(
                    N);

            ArrayList<VPackage.ActivityIntentInfo> intentFilters;
            for (int i = 0; i < N; ++i) {
                intentFilters = packageActivities.get(i).intents;
                if (intentFilters != null && intentFilters.size() > 0) {
                    VPackage.ActivityIntentInfo[] array = new VPackage.ActivityIntentInfo[intentFilters
                            .size()];
                    intentFilters.toArray(array);
                    listCut.add(array);
                }
            }
            return super.queryIntentFromList(intent, resolvedType, defaultOnly, listCut, userId);
        }

        public final void addActivity(VPackage.ActivityComponent a, String type) {
            mActivities.put(a.getComponentName(), a);
            final int NI = a.intents.size();
            for (int j = 0; j < NI; j++) {
                VPackage.ActivityIntentInfo intent = a.intents.get(j);
                if (intent.filter.getPriority() > 0 && "activity".equals(type)) {
                    intent.filter.setPriority(0);
                    Log.w(TAG, "Package " + a.info.applicationInfo.packageName + " has activity " + a.className
                            + " with priority > 0, forcing to 0");
                }
                addFilter(intent);
            }
        }

        public final void removeActivity(VPackage.ActivityComponent a, String type) {
            mActivities.remove(a.getComponentName());
            final int NI = a.intents.size();
            for (int j = 0; j < NI; j++) {
                VPackage.ActivityIntentInfo intent = a.intents.get(j);
                removeFilter(intent);
            }
        }

        @Override
        protected boolean allowFilterResult(VPackage.ActivityIntentInfo filter, List<ResolveInfo> dest) {
            ActivityInfo filterAi = filter.activity.info;
            for (int i = dest.size() - 1; i >= 0; i--) {
                ActivityInfo destAi = dest.get(i).activityInfo;
                if (ObjectsCompat.equals(destAi.name, filterAi.name) && ObjectsCompat.equals(destAi.packageName, filterAi.packageName)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        protected VPackage.ActivityIntentInfo[] newArray(int size) {
            return new VPackage.ActivityIntentInfo[size];
        }

        @Override
        protected boolean isFilterStopped(VPackage.ActivityIntentInfo filter) {
            return false;
        }

        @Override
        protected boolean isPackageForFilter(String packageName, VPackage.ActivityIntentInfo info) {
            return packageName.equals(info.activity.owner.packageName);
        }

        @Override
        protected ResolveInfo newResult(VPackage.ActivityIntentInfo info, int match, int userId) {
            final VPackage.ActivityComponent activity = info.activity;
            PackageSetting ps = (PackageSetting) activity.owner.mExtras;
            ActivityInfo ai = PackageParserEx.generateActivityInfo(activity, mFlags, ps.readUserState(userId), userId);
            if (ai == null) {
                return null;
            }
            final ResolveInfo res = new ResolveInfo();
            res.activityInfo = ai;
            if ((mFlags & PackageManager.GET_RESOLVED_FILTER) != 0) {
                res.filter = info.filter;
            }
            res.priority = info.filter.getPriority();
            res.preferredOrder = activity.owner.mPreferredOrder;
            res.match = match;
            res.isDefault = info.hasDefault;
            res.labelRes = info.labelRes;
            res.nonLocalizedLabel = info.nonLocalizedLabel;
            res.icon = info.icon;
            return res;
        }

        @Override
        protected void sortResults(List<ResolveInfo> results) {
            Collections.sort(results, sResolvePrioritySorter);
        }

        @Override
        protected void dumpFilter(PrintWriter out, String prefix, VPackage.ActivityIntentInfo filter) {

        }

        @Override
        protected Object filterToLabel(VPackage.ActivityIntentInfo filter) {
            return filter.activity;
        }

        protected void dumpFilterLabel(PrintWriter out, String prefix, Object label, int count) {

        }
    }

    private final class ServiceIntentResolver extends IntentResolver<VPackage.ServiceIntentInfo, ResolveInfo> {
        // Keys are String (activity class name), values are Activity.
        private final HashMap<ComponentName, VPackage.ServiceComponent> mServices = new HashMap<>();
        private int mFlags;

        public List<ResolveInfo> queryIntent(Intent intent, String resolvedType, boolean defaultOnly, int userId) {
            mFlags = defaultOnly ? PackageManager.MATCH_DEFAULT_ONLY : 0;
            return super.queryIntent(intent, resolvedType, defaultOnly, userId);
        }

        public List<ResolveInfo> queryIntent(Intent intent, String resolvedType, int flags, int userId) {
            mFlags = flags;
            return super.queryIntent(intent, resolvedType, (flags & PackageManager.MATCH_DEFAULT_ONLY) != 0, userId);
        }

        public List<ResolveInfo> queryIntentForPackage(Intent intent, String resolvedType, int flags,
                                                       ArrayList<VPackage.ServiceComponent> packageServices, int userId) {
            if (packageServices == null) {
                return null;
            }
            mFlags = flags;
            final boolean defaultOnly = (flags & PackageManager.MATCH_DEFAULT_ONLY) != 0;
            final int N = packageServices.size();
            ArrayList<VPackage.ServiceIntentInfo[]> listCut = new ArrayList<VPackage.ServiceIntentInfo[]>(N);

            ArrayList<VPackage.ServiceIntentInfo> intentFilters;
            for (int i = 0; i < N; ++i) {
                intentFilters = packageServices.get(i).intents;
                if (intentFilters != null && intentFilters.size() > 0) {
                    VPackage.ServiceIntentInfo[] array = new VPackage.ServiceIntentInfo[intentFilters.size()];
                    intentFilters.toArray(array);
                    listCut.add(array);
                }
            }
            return super.queryIntentFromList(intent, resolvedType, defaultOnly, listCut, userId);
        }

        public final void addService(VPackage.ServiceComponent s) {
            mServices.put(s.getComponentName(), s);
            final int NI = s.intents.size();
            int j;
            for (j = 0; j < NI; j++) {
                VPackage.ServiceIntentInfo intent = s.intents.get(j);
                addFilter(intent);
            }
        }

        public final void removeService(VPackage.ServiceComponent s) {
            mServices.remove(s.getComponentName());
            final int NI = s.intents.size();
            int j;
            for (j = 0; j < NI; j++) {
                VPackage.ServiceIntentInfo intent = s.intents.get(j);
                removeFilter(intent);
            }
        }

        @Override
        protected boolean allowFilterResult(VPackage.ServiceIntentInfo filter, List<ResolveInfo> dest) {
            ServiceInfo filterSi = filter.service.info;
            for (int i = dest.size() - 1; i >= 0; i--) {
                ServiceInfo destAi = dest.get(i).serviceInfo;
                if (ObjectsCompat.equals(destAi.name, filterSi.name)
                        && ObjectsCompat.equals(destAi.packageName, filterSi.packageName)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        protected VPackage.ServiceIntentInfo[] newArray(int size) {
            return new VPackage.ServiceIntentInfo[size];
        }

        @Override
        protected boolean isFilterStopped(VPackage.ServiceIntentInfo filter) {
            return false;
        }

        @Override
        protected boolean isPackageForFilter(String packageName, VPackage.ServiceIntentInfo info) {
            return packageName.equals(info.service.owner.packageName);
        }

        @Override
        protected ResolveInfo newResult(VPackage.ServiceIntentInfo filter, int match, int userId) {
            final VPackage.ServiceComponent service = filter.service;
            PackageSetting ps = (PackageSetting) service.owner.mExtras;
            ServiceInfo si = PackageParserEx.generateServiceInfo(service, mFlags, ps.readUserState(userId), userId);
            if (si == null) {
                return null;
            }
            final ResolveInfo res = new ResolveInfo();
            res.serviceInfo = si;
            if ((mFlags & PackageManager.GET_RESOLVED_FILTER) != 0) {
                res.filter = filter.filter;
            }
            res.priority = filter.filter.getPriority();
            res.preferredOrder = service.owner.mPreferredOrder;
            res.match = match;
            res.isDefault = filter.hasDefault;
            res.labelRes = filter.labelRes;
            res.nonLocalizedLabel = filter.nonLocalizedLabel;
            res.icon = filter.icon;
            return res;
        }

        @Override
        protected void sortResults(List<ResolveInfo> results) {
            Collections.sort(results, sResolvePrioritySorter);
        }

        @Override
        protected void dumpFilter(PrintWriter out, String prefix, VPackage.ServiceIntentInfo filter) {

        }

        @Override
        protected Object filterToLabel(VPackage.ServiceIntentInfo filter) {
            return filter.service;
        }

        protected void dumpFilterLabel(PrintWriter out, String prefix, Object label, int count) {

        }
    }

}
