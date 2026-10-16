package local.alipay.resourcefix;

import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Build;
import android.util.TypedValue;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class Entry implements IXposedHookLoadPackage {
    private static final String TARGET = "com.eg.android.AlipayGphone";
    private static final String TAG = "[AlipayResourceFix] ";

    @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam load) throws Throwable {
        if (!TARGET.equals(load.packageName) || !TARGET.equals(load.processName)) return;
        if (Build.VERSION.SDK_INT != 33) {
            XposedBridge.log(TAG + "Skipped: this build supports Android 13 / API 33 only.");
            return;
        }
        Class<?> bundleResources = XposedHelpers.findClass(
                "com.alipay.mobile.quinox.resources.BundleResources", load.classLoader);
        XposedBridge.hookAllConstructors(bundleResources, new XC_MethodHook() {
            private boolean reported;
            @Override protected void beforeHookedMethod(MethodHookParam param) {
                try {
                    if (param.args.length > 0 && param.args[0] instanceof AssetManager) {
                        if (Isolation.repair((AssetManager) param.args[0], true)) {
                            XposedBridge.log(TAG + "Removed colliding Lineage asset from Alipay bundle AssetManager.");
                        }
                    }
                } catch (Throwable failure) {
                    XposedBridge.log(TAG + "Isolation failed: " + failure);
                }
            }
            @Override protected void afterHookedMethod(MethodHookParam param) {
                if (reported || param.hasThrowable()) return;
                try {
                    Resources resources = (Resources) param.thisObject;
                    TypedValue value = new TypedValue();
                    resources.getValue(0x3f090041, value, true);
                    String name = resources.getResourceName(0x3f090041);
                    if (name.contains("quick_menu_layout")) {
                        reported = true;
                        XposedBridge.log(TAG + "Verified 0x3f090041 -> " + name + " -> " + value.string);
                    }
                } catch (Throwable ignored) {
                    // Some BundleResources intentionally do not contain the chat bundle.
                }
            }
        });
        XposedBridge.log(TAG + "Hook installed for Alipay main process, Android 13.");
    }
}
