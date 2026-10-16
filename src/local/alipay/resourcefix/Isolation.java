package local.alipay.resourcefix;

import android.content.res.AssetManager;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

/** Removes the colliding platform resource from one private AssetManager. */
public final class Isolation {
    private Isolation() {}

    public static boolean repair(AssetManager manager, boolean requireAlipayBundle) throws Exception {
        synchronized (manager) {
            Field assetsField = AssetManager.class.getDeclaredField("mApkAssets");
            assetsField.setAccessible(true);
            Object[] original = (Object[]) assetsField.get(manager);
            boolean hasBundle = false;
            ArrayList<Object> keep = new ArrayList<Object>();
            for (Object asset : original) {
                Method getPath = asset.getClass().getDeclaredMethod("getAssetPath");
                getPath.setAccessible(true);
                String path = (String) getPath.invoke(asset);
                if (path != null && path.endsWith("/libmerged-slink-bundles-res.so")) {
                    hasBundle = true;
                }
                if (path == null || !path.endsWith("/org.lineageos.platform-res.apk")) {
                    keep.add(asset);
                }
            }
            if ((requireAlipayBundle && !hasBundle) || keep.size() == original.length) return false;
            Object filtered = Array.newInstance(original.getClass().getComponentType(), keep.size());
            for (int i = 0; i < keep.size(); i++) Array.set(filtered, i, keep.get(i));
            Field pointerField = AssetManager.class.getDeclaredField("mObject");
            pointerField.setAccessible(true);
            Method nativeSet = AssetManager.class.getDeclaredMethod("nativeSetApkAssets",
                    long.class, original.getClass(), boolean.class);
            nativeSet.setAccessible(true);
            Method invalidate = AssetManager.class.getDeclaredMethod("invalidateCachesLocked", int.class);
            invalidate.setAccessible(true);
            // Keep Java cookie indexing identical to native indexing. Do not call setApkAssets:
            // its Java implementation would automatically prepend Lineage's system asset again.
            assetsField.set(manager, filtered);
            try {
                nativeSet.invoke(null, pointerField.getLong(manager), filtered, true);
            } catch (Exception failure) {
                assetsField.set(manager, original);
                try { nativeSet.invoke(null, pointerField.getLong(manager), original, true); }
                catch (Exception rollback) { failure.addSuppressed(rollback); }
                throw failure;
            }
            invalidate.invoke(manager, -1);
            return true;
        }
    }
}
