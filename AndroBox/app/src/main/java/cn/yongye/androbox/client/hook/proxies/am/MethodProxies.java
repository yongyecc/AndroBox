package cn.yongye.androbox.client.hook.proxies.am;

import static cn.yongye.androbox.client.stub.VASettings.INTERCEPT_BACK_HOME;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;

import cn.yongye.androbox.FileUtils;
import cn.yongye.androbox.VirtualCore;
import cn.yongye.androbox.client.badger.BadgerManager;
import cn.yongye.androbox.client.env.Constants;
import cn.yongye.androbox.client.hook.base.MethodProxy;
import cn.yongye.androbox.client.ipc.ActivityClientRecord;
import cn.yongye.androbox.client.ipc.VActivityManager;
import cn.yongye.androbox.client.stub.ChooserActivity;
import cn.yongye.androbox.client.stub.StubPendingActivity;
import cn.yongye.androbox.client.stub.StubPendingReceiver;
import cn.yongye.androbox.client.stub.StubPendingService;
import cn.yongye.androbox.client.stub.VASettings;
import cn.yongye.androbox.helper.compat.ActivityManagerCompat;
import cn.yongye.androbox.helper.utils.ArrayUtils;
import cn.yongye.androbox.helper.utils.BitmapUtils;
import cn.yongye.androbox.helper.utils.ComponentUtils;
import cn.yongye.androbox.helper.utils.VLog;
import cn.yongye.androbox.os.VUserHandle;
import cn.yongye.androbox.virtual.server.interfaces.IAppRequestListener;

@SuppressWarnings("unused")
public class MethodProxies {

    static class StartActivity extends MethodProxy {

        private static final String SCHEME_FILE = "file";
        private static final String SCHEME_PACKAGE = "package";
        private static final String SCHEME_CONTENT = "content";

        @Override
        public String getMethodName() {
            return "startActivity";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {

            Log.d("Q_M", "---->StartActivity 类");

            int intentIndex = ArrayUtils.indexOfObject(args, Intent.class, 1);
            if (intentIndex < 0) {
                return ActivityManagerCompat.START_INTENT_NOT_RESOLVED;
            }
            int resultToIndex = ArrayUtils.indexOfObject(args, IBinder.class, 2);
            String resolvedType = (String) args[intentIndex + 1];
            Intent intent = (Intent) args[intentIndex];
            intent.setDataAndType(intent.getData(), resolvedType);
            IBinder resultTo = resultToIndex >= 0 ? (IBinder) args[resultToIndex] : null;
            int userId = VUserHandle.myUserId();

            if (ComponentUtils.isStubComponent(intent)) {
                return method.invoke(who, args);
            }

            if (Intent.ACTION_INSTALL_PACKAGE.equals(intent.getAction())
                    || (Intent.ACTION_VIEW.equals(intent.getAction())
                    && "application/vnd.android.package-archive".equals(intent.getType()))) {
                if (handleInstallRequest(intent)) {
                    return 0;
                }
            } else if ((Intent.ACTION_UNINSTALL_PACKAGE.equals(intent.getAction())
                    || Intent.ACTION_DELETE.equals(intent.getAction()))
                    && "package".equals(intent.getScheme())) {

                if (handleUninstallRequest(intent)) {
                    return 0;
                }
            }

            String resultWho = null;
            int requestCode = 0;
            Bundle options = ArrayUtils.getFirst(args, Bundle.class);
            if (resultTo != null) {
                resultWho = (String) args[resultToIndex + 1];
                requestCode = (int) args[resultToIndex + 2];
            }
            // chooser
            if (ChooserActivity.check(intent)) {
                intent.setComponent(new ComponentName(getHostContext(), ChooserActivity.class));
                intent.putExtra(Constants.EXTRA_USER_HANDLE, userId);
                intent.putExtra(ChooserActivity.EXTRA_DATA, options);
                intent.putExtra(ChooserActivity.EXTRA_WHO, resultWho);
                intent.putExtra(ChooserActivity.EXTRA_REQUEST_CODE, requestCode);
                return method.invoke(who, args);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                args[intentIndex - 1] = getHostPkg();
            }
            if (intent.getScheme() != null && intent.getScheme().equals(SCHEME_PACKAGE) && intent.getData() != null) {
                if (intent.getAction() != null && intent.getAction().startsWith("android.settings.")) {
                    intent.setData(Uri.parse("package:" + getHostPkg()));
                }
            }

            ActivityInfo activityInfo = VirtualCore.get().resolveActivityInfo(intent, userId);
            if (activityInfo == null) {
                VLog.e("VActivityManager", "Unable to resolve activityInfo : " + intent);

                Log.d("Q_M", "---->StartActivity who=" + who);
                Log.d("Q_M", "---->StartActivity intent=" + intent);
                Log.d("Q_M", "---->StartActivity resultTo=" + resultTo);

                if (intent.getPackage() != null && isAppPkg(intent.getPackage())) {
                    return ActivityManagerCompat.START_INTENT_NOT_RESOLVED;
                }

                if (INTERCEPT_BACK_HOME && Intent.ACTION_MAIN.equals(intent.getAction())
                        && intent.getCategories().contains("android.intent.category.HOME")
                        && resultTo != null) {
                    VActivityManager.get().finishActivity(resultTo);
                    return 0;
                }

                return method.invoke(who, args);
            }
            int res = VActivityManager.get().startActivity(intent, activityInfo, resultTo, options, resultWho, requestCode, VUserHandle.myUserId());
            if (res != 0 && resultTo != null && requestCode > 0) {
                VActivityManager.get().sendActivityResult(resultTo, resultWho, requestCode);
            }
            if (resultTo != null) {
                ActivityClientRecord r = VActivityManager.get().getActivityRecord(resultTo);
                if (r != null && r.activity != null) {
                    try {
                        TypedValue out = new TypedValue();
                        Resources.Theme theme = r.activity.getResources().newTheme();
                        theme.applyStyle(activityInfo.getThemeResource(), true);
                        if (theme.resolveAttribute(android.R.attr.windowAnimationStyle, out, true)) {

                            TypedArray array = theme.obtainStyledAttributes(out.data,
                                    new int[]{
                                            android.R.attr.activityOpenEnterAnimation,
                                            android.R.attr.activityOpenExitAnimation
                                    });

                            r.activity.overridePendingTransition(array.getResourceId(0, 0), array.getResourceId(1, 0));
                            array.recycle();
                        }
                    } catch (Throwable e) {
                        // Ignore
                    }
                }
            }
            return res;
        }


        private boolean handleInstallRequest(Intent intent) {
            IAppRequestListener listener = VirtualCore.get().getAppRequestListener();
            if (listener != null) {
                Uri packageUri = intent.getData();
                if (SCHEME_FILE.equals(packageUri.getScheme())) {
                    File sourceFile = new File(packageUri.getPath());
                    try {
                        listener.onRequestInstall(sourceFile.getPath());
                        return true;
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                } else if (SCHEME_CONTENT.equals(packageUri.getScheme())) {
                    InputStream inputStream = null;
                    OutputStream outputStream = null;
                    File sharedFileCopy = new File(getHostContext().getCacheDir(), packageUri.getLastPathSegment());
                    try {
                        inputStream = getHostContext().getContentResolver().openInputStream(packageUri);
                        outputStream = new FileOutputStream(sharedFileCopy);
                        byte[] buffer = new byte[1024];
                        int count;
                        while ((count = inputStream.read(buffer)) > 0) {
                            outputStream.write(buffer, 0, count);
                        }
                        outputStream.flush();

                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        FileUtils.closeQuietly(inputStream);
                        FileUtils.closeQuietly(outputStream);
                    }
                    try {
                        listener.onRequestInstall(sharedFileCopy.getPath());
                        return true;
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }

            }
            return false;
        }

        private boolean handleUninstallRequest(Intent intent) {
            IAppRequestListener listener = VirtualCore.get().getAppRequestListener();
            if (listener != null) {
                Uri packageUri = intent.getData();
                if (SCHEME_PACKAGE.equals(packageUri.getScheme())) {
                    String pkg = packageUri.getSchemeSpecificPart();
                    try {
                        listener.onRequestUninstall(pkg);
                        return true;
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }

            }
            return false;
        }

    }

    static class BroadcastIntent extends MethodProxy {

        @Override
        public String getMethodName() {
            return "broadcastIntent";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            Intent intent = (Intent) args[1];
            String type = (String) args[2];
            intent.setDataAndType(intent.getData(), type);
            if (VirtualCore.get().getComponentDelegate() != null) {
                VirtualCore.get().getComponentDelegate().onSendBroadcast(intent);
            }
            Intent newIntent = handleIntent(intent);
            if (newIntent != null) {
                args[1] = newIntent;
            } else {
                return 0;
            }

            if (args[7] instanceof String || args[7] instanceof String[]) {
                // clear the permission
                args[7] = null;
            }
//            ((Intent) args[1]).setComponent(new ComponentName("cn.yongye.androbox",
//                    "cn.yongye.androbox.client.stub.StubPendingReceiver"));
            return method.invoke(who, args);
        }


        private Intent handleIntent(final Intent intent) {
            final String action = intent.getAction();
            if ("android.intent.action.CREATE_SHORTCUT".equals(action)
                    || "com.android.launcher.action.INSTALL_SHORTCUT".equals(action)) {

                return VASettings.ENABLE_INNER_SHORTCUT ? handleInstallShortcutIntent(intent) : null;

            } else if ("com.android.launcher.action.UNINSTALL_SHORTCUT".equals(action)) {

                handleUninstallShortcutIntent(intent);

            } else if (BadgerManager.handleBadger(intent)) {
                return null;
            } else {
                return ComponentUtils.redirectBroadcastIntent(intent, VUserHandle.myUserId());
            }
            return intent;
        }

        private Intent handleInstallShortcutIntent(Intent intent) {
            Intent shortcut = intent.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
            if (shortcut != null) {
                ComponentName component = shortcut.resolveActivity(VirtualCore.getPM());
                if (component != null) {
                    String pkg = component.getPackageName();
                    Intent newShortcutIntent = new Intent();
                    newShortcutIntent.setClassName(getHostPkg(), Constants.SHORTCUT_PROXY_ACTIVITY_NAME);
                    newShortcutIntent.addCategory(Intent.CATEGORY_DEFAULT);
                    newShortcutIntent.putExtra("_VA_|_intent_", shortcut);
                    newShortcutIntent.putExtra("_VA_|_uri_", shortcut.toUri(0));
                    newShortcutIntent.putExtra("_VA_|_user_id_", VUserHandle.myUserId());
                    intent.removeExtra(Intent.EXTRA_SHORTCUT_INTENT);
                    intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, newShortcutIntent);

                    Intent.ShortcutIconResource icon = intent.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE);
                    if (icon != null && !TextUtils.equals(icon.packageName, getHostPkg())) {
                        try {
                            Resources resources = VirtualCore.get().getResources(pkg);
                            int resId = resources.getIdentifier(icon.resourceName, "drawable", pkg);
                            if (resId > 0) {
                                //noinspection deprecation
                                Drawable iconDrawable = resources.getDrawable(resId);
                                Bitmap newIcon = BitmapUtils.drawableToBitmap(iconDrawable);
                                if (newIcon != null) {
                                    intent.removeExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE);
                                    intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, newIcon);
                                }
                            }
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            return intent;
        }

        private void handleUninstallShortcutIntent(Intent intent) {
            Intent shortcut = intent.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
            if (shortcut != null) {
                ComponentName componentName = shortcut.resolveActivity(getPM());
                if (componentName != null) {
                    Intent newShortcutIntent = new Intent();
                    newShortcutIntent.putExtra("_VA_|_uri_", shortcut.toUri(0));
                    newShortcutIntent.setClassName(getHostPkg(), Constants.SHORTCUT_PROXY_ACTIVITY_NAME);
                    newShortcutIntent.removeExtra(Intent.EXTRA_SHORTCUT_INTENT);
                    intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, newShortcutIntent);
                }
            }
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }
    }

    static class GetIntentSender extends MethodProxy {

        @Override
        public String getMethodName() {
            return "getIntentSender";
        }

        @Override
        public Object call(Object who, Method method, Object... args) throws Throwable {
            String creator = (String) args[1];
            String[] resolvedTypes = (String[]) args[6];
            int type = (int) args[0];
            int flags = (int) args[7];
            if (args[5] instanceof Intent[]) {
                Intent[] intents = (Intent[]) args[5];
                for (int i = 0; i < intents.length; i++) {
                    Intent intent = intents[i];
                    if (resolvedTypes != null && i < resolvedTypes.length) {
                        intent.setDataAndType(intent.getData(), resolvedTypes[i]);
                    }
                    Intent targetIntent = redirectIntentSender(type, creator, intent);
                    if (targetIntent != null) {
                        intents[i] = targetIntent;
                    }
                }
            }
            args[7] = flags;
            args[1] = getHostPkg();
            // Force userId to 0
            if (args[args.length - 1] instanceof Integer) {
                args[args.length - 1] = 0;
            }
            IInterface sender = (IInterface) method.invoke(who, args);
            if (sender != null && creator != null) {
                VActivityManager.get().addPendingIntent(sender.asBinder(), creator);
            }
            return sender;
        }

        private Intent redirectIntentSender(int type, String creator, Intent intent) {
            Intent newIntent = intent.cloneFilter();
            switch (type) {
                case ActivityManagerCompat.INTENT_SENDER_ACTIVITY: {
                    ComponentInfo info = VirtualCore.get().resolveActivityInfo(intent, VUserHandle.myUserId());
                    if (info != null) {
                        newIntent.setClass(getHostContext(), StubPendingActivity.class);
                        newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    }
                }
                break;
                case ActivityManagerCompat.INTENT_SENDER_SERVICE: {
                    ComponentInfo info = VirtualCore.get().resolveServiceInfo(intent, VUserHandle.myUserId());
                    if (info != null) {
                        newIntent.setClass(getHostContext(), StubPendingService.class);
                    }
                }
                break;
                case ActivityManagerCompat.INTENT_SENDER_BROADCAST: {
                    newIntent.setClass(getHostContext(), StubPendingReceiver.class);
                }
                break;
                default:
                    return null;
            }
            newIntent.putExtra("_VA_|_user_id_", VUserHandle.myUserId());
            newIntent.putExtra("_VA_|_intent_", intent);
            newIntent.putExtra("_VA_|_creator_", creator);
            newIntent.putExtra("_VA_|_from_inner_", true);
            return newIntent;
        }

        @Override
        public boolean isEnable() {
            return isAppProcess();
        }

    }

    protected static boolean isAppProcess() {
        return VirtualCore.get().isVAppProcess();
    }

    protected static PackageManager getPM() {
        return VirtualCore.getPM();
    }
}
