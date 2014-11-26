package co.onemeter.oneapp.utils;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

/**
 * <p>Help to pass Theme resource ID between Activities.</p>
 * Created by pzy on 11/25/14.
 */
public class ThemeHelper {
    /** Theme resource ID. */
    private final static String EXTRA_THEME_RESID = "theme_helper_theme_23947593";

    public static int getThemeResId(Activity activity) {
        try {
            return activity.getPackageManager().getActivityInfo(activity.getComponentName(), 0).theme;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Pass theme to other Activity: Put Activity's theme resource ID into Intent extras.
     * @param intent
     * @param sourceActivity
     */
    public static void putExtraCurrThemeResId(Intent intent, Activity sourceActivity) {
        intent.putExtra(ThemeHelper.EXTRA_THEME_RESID,
                ThemeHelper.getThemeResId(sourceActivity));
    }

    /**
     * Accept theme passed by other Activity:
     * Set Acitivity theme as specified by {@link #EXTRA_THEME_RESID}.
     * <p>Call me before {@link Activity#setContentView}.</p>
     * @param activity
     * @param extras
     */
    public static void setTheme(Activity activity, Bundle extras) {
        if (extras != null) {
            int themeResId = extras.getInt(EXTRA_THEME_RESID);
            if (themeResId > 0)
                activity.setTheme(themeResId);
        }
    }
}
