package com.sagar.easylock;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

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


    public static final String KEY_MASTER_SWITCH_ON = "master_switch";
    public static final String KEY_STATUS_BAR_HEIGHT = "status_bar_height";

    public static void setPreference(Context context, final String KEY, boolean value){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().putBoolean(KEY, value).apply();
        callListeners();
    }

    public static void setPreference(Context context, final String KEY, int value){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().putInt(KEY, value).apply();
    }

    public static void setPreference(Context context, final String KEY, long value){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().putLong(KEY, value).apply();
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

    private static void callListeners(){
        for (PreferencesChangedListener listener : mListeners) {
            listener.onPreferencesChanged();
        }
    }

    public interface PreferencesChangedListener {
        void onPreferencesChanged();
    }
}
