import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.Configuration;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import java.lang.reflect.Method;
import local.alipay.resourcefix.Isolation;

public class Probe {
    public static void main(String[] args) throws Exception {
        Class<?> runtime = Class.forName("dalvik.system.VMRuntime");
        Method getRuntime = runtime.getDeclaredMethod("getRuntime");
        Method exemptions = runtime.getDeclaredMethod("setHiddenApiExemptions", String[].class);
        exemptions.invoke(getRuntime.invoke(null), (Object) new String[]{"L"});
        AssetManager assets = AssetManager.class.getDeclaredConstructor().newInstance();
        Method add = AssetManager.class.getDeclaredMethod("addAssetPath", String.class);
        add.setAccessible(true);
        System.out.println("ADD_COOKIE=" + add.invoke(assets, args[0]));
        print(assets, "BEFORE");
        System.out.println("FILTERED=" + Isolation.repair(assets, true));
        print(assets, "AFTER");
    }
    private static void print(AssetManager assets, String prefix) throws Exception {
        DisplayMetrics metrics = new DisplayMetrics(); metrics.setToDefaults();
        Resources res = new Resources(assets, metrics, new Configuration());
        for (int id : new int[]{0x3f090041, 0x3f090019}) {
            TypedValue v = new TypedValue();res.getValue(id,v,true);
            System.out.println(prefix + " " + Integer.toHexString(id) + " " + res.getResourceName(id) + " -> " + v.string);
            try {
                Object parser = Resources.class.getMethod("getLayout", int.class).invoke(res, id);
                Class.forName("android.content.res.XmlResourceParser").getMethod("close").invoke(parser);
                System.out.println(prefix + " XML_OK");
            }
            catch (Exception e) { System.out.println(prefix + " XML_ERROR=" + e); }
        }
    }
}
