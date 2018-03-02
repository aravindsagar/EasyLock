package com.sagar.easylock;

import android.annotation.TargetApi;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.preference.PreferenceManager;

import com.sagar.easylock.service.EasyLockService;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Created by aravind on 12/6/15.
 * Constants for SharedPrefernce and convenience methods for getting and setting them
 */
public class PreferencesHelper {

    private static Set<PreferencesChangedListener> mListeners;

    private PreferencesHelper() {

    }

    static {
        mListeners = Collections.newSetFromMap(new WeakHashMap<PreferencesChangedListener, Boolean>());
    }


    public static final String KEY_MASTER_SWITCH_ON   = "master_switch";
    public static final String KEY_STATUS_BAR_HEIGHT  = "status_bar_height";
    public static final String KEY_SHOW_NOTIFICATION  = "show_notification";
    public static final String KEY_START_ON_BOOT      = "start_on_boot";
    public static final String KEY_DETECT_SOFT_KEY    = "avoid_softkeys";
    public static final String KEY_AVOID_LOCKSCREEN   = "avoid_lockscreen";
    public static final String KEY_SUPPORT_SMART_LOCK = "support_smart_lock";
    public static final String KEY_DOUBLE_TAP_TIMEOUT = "double_tap_timeout";
    public static final String KEY_HAS_VIEWED_INTRO   = "has_viewed_intro";
    public static final String KEY_TOUCH_ANYWHERE     = "touch_anywhere";
    public static final String KEY_TOUCH_HOME         = "touch_home";

    public static void setPreference(Context context, final String KEY, boolean value){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().putBoolean(KEY, value).apply();
        callListeners(context);
    }

    public static void setPreference(Context context, final String KEY, int value){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().putInt(KEY, value).apply();
        callListeners(context);
    }

    public static void setPreference(Context context, final String KEY, long value){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().putLong(KEY, value).apply();
        callListeners(context);
    }

    public static boolean getBoolPreference(Context context, final String KEY){
        return getBoolPreference(context, KEY, false);
    }

    public static boolean getBoolPreference(Context context, final String KEY, boolean defaultValue){
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(KEY, defaultValue);
    }

    public static int getIntPreference(Context context, final String KEY, int defaultValue){
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(KEY, defaultValue);
    }

    public static long getLongPreference(Context context, final String KEY, long defaultValue){
        return PreferenceManager.getDefaultSharedPreferences(context).getLong(KEY, defaultValue);
    }

    public static String getStringPreference(Context context, final String KEY, String defaultValue){
        return PreferenceManager.getDefaultSharedPreferences(context).getString(KEY, defaultValue);
    }

    public static void registerListener(PreferencesChangedListener listener) {
        mListeners.add(listener);
    }

    private static void callListeners(Context c){
        c.startService(new Intent(c, EasyLockService.class).setAction(EasyLockService.ACTION_RELOAD_PREFS));
        for (PreferencesChangedListener listener : mListeners) {
            listener.onPreferencesChanged();
        }
    }

    public interface PreferencesChangedListener {
        void onPreferencesChanged();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static boolean hasUsageAccess(Context context) {
        try {
            PackageManager packageManager = context.getPackageManager();
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(context.getPackageName(), 0);
            AppOpsManager appOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            int mode = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                    applicationInfo.uid, applicationInfo.packageName);
            return (mode == AppOpsManager.MODE_ALLOWED);

        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
