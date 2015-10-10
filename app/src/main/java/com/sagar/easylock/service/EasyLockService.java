package com.sagar.easylock.service;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.sagar.easylock.MainActivity;
import com.sagar.easylock.PreferencesHelper;
import com.sagar.easylock.R;
import com.sagar.easylock.screenlock.AdminActions;
import com.sagar.easylock.screenlock.RootHelper;
import com.sagar.easylock.service.overlay.EasyLockFilterOverlay;
import com.sagar.easylock.service.overlay.EasyLockOverlay;
import com.sagar.easylock.service.overlay.OverlayBase;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import static com.sagar.easylock.PreferencesHelper.KEY_DETECT_SOFT_KEY;
import static com.sagar.easylock.PreferencesHelper.KEY_DOUBLE_TAP_TIMEOUT;
import static com.sagar.easylock.PreferencesHelper.KEY_MASTER_SWITCH_ON;
import static com.sagar.easylock.PreferencesHelper.KEY_SUPPORT_SMART_LOCK;

public class EasyLockService extends Service {

    public static final String ACTION_START_OVERLAY     = "action_start_overlay";
    public static final String ACTION_STOP_OVERLAY      = "action_stop_overlay";
    public static final String ACTION_TOGGLE            = "action_toggle";
    public static final String ACTION_POST_NOTIFICATION = "action_post_notification";

    public static final String EXTRA_SEND_BROADCAST     = "send_broadcast";

    OverlayBase lockOverlay, filterOverlay;

    private boolean filterTapped   = false;
    private String firstTapPackage = "";
    private ActivityManager mActivityManager;
    UsageStatsManager mUsageStatsManager;
    Handler handler;

    private boolean avoidSoftkeys = false, supportSmartLock = false;

    public EasyLockService() {}

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void postNotification() {
        if(PreferencesHelper.getBoolPreference(this, PreferencesHelper.KEY_MASTER_SWITCH_ON)) {
            postNotification(getString(R.string.service_enabled), R.drawable.ic_stat_service_running);
        } else {
            postNotification(getString(R.string.service_disabled), R.drawable.ic_stat_service_paused);
        }
    }

    private void postNotification(String text, int icon) {
        postNotification(text, null, icon);
    }
    private void postNotification(String text, Boolean override, int icon) {
        if(override == null) {
            if (PreferencesHelper.getBoolPreference(this, PreferencesHelper.KEY_SHOW_NOTIFICATION, true)) {
                startForeground(1, buildNotification(text, icon));
            } else {
                stopForeground(true);
            }
        } else {
            if(override) {
                startForeground(1, buildNotification(text, icon));
            } else {
                stopForeground(true);
            }
        }
    }

    private Notification buildNotification(String text, int icon){
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this);
        Intent intent = new Intent(this, EasyLockService.class);
        intent.setAction(ACTION_TOGGLE);
        intent.putExtra(EXTRA_SEND_BROADCAST, true);
        PendingIntent pi = PendingIntent.getService(this, 2, intent, PendingIntent.FLAG_UPDATE_CURRENT),
                settingsPi = PendingIntent.getActivity(this, 3, new Intent(this, MainActivity.class).
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP), PendingIntent.FLAG_UPDATE_CURRENT);
        return notificationBuilder.setContentTitle(getString(R.string.app_name)).
                setSmallIcon(icon).setContentText(text).setPriority(NotificationCompat.PRIORITY_MIN).
                addAction(R.drawable.ic_stat_notif_settings, getString(R.string.title_activity_settings), settingsPi).
                setContentIntent(pi).build();
    }

    private void postAdminEnableNotification() {
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this);
        PendingIntent settingsPi = PendingIntent.getActivity(this, 3, new Intent(this, MainActivity.class).
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP), PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = notificationBuilder
                .setContentTitle(getString(R.string.app_name))
                .setSmallIcon(R.drawable.ic_stat_notif_settings)
                .setContentText(getString(R.string.enable_admin))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(settingsPi).build();
        startForeground(1, notification);
    }

    private void postDrawOverEnableNotification() {
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this);
        PendingIntent settingsPi = PendingIntent.getActivity(this, 3, new Intent(this, MainActivity.class).
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP), PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = notificationBuilder
                .setContentTitle(getString(R.string.app_name))
                .setSmallIcon(R.drawable.ic_stat_notif_settings)
                .setContentText(getString(R.string.enable_draw_over))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(settingsPi).build();
        startForeground(1, notification);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        handler = new Handler();

        mActivityManager = (ActivityManager) getBaseContext().getSystemService(Context.ACTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            mUsageStatsManager = (UsageStatsManager) this.getSystemService(Context.USAGE_STATS_SERVICE);
        }

        final Runnable lockRunnable = new Runnable() {
            @Override
            public void run() {
                String secondTapPackage = "";
                if(avoidSoftkeys && !firstTapPackage.isEmpty()) {
                    int count = 0;
                    while (secondTapPackage.isEmpty()) {
                        secondTapPackage = getForegroundAppPackage();
                        count++;
                        if(count == 10 && !PreferencesHelper.hasUsageAccess(EasyLockService.this)) {
                            PreferencesHelper.setPreference(
                                    EasyLockService.this, KEY_DETECT_SOFT_KEY, false);
                        } else if(count > 100) {
                            secondTapPackage = firstTapPackage = "";
                            break;
                        }
                    }
                }
                if(!filterTapped && (!avoidSoftkeys || firstTapPackage.equals(secondTapPackage))) {
                    Log.d("EasyLockService", "Support smart lock: " + supportSmartLock);
                    if(!(supportSmartLock && RootHelper.lockNow())
                            && !AdminActions.turnScreenOff()) {
                        Toast.makeText(EasyLockService.this, R.string.enable_admin, Toast.LENGTH_SHORT).show();
                        postAdminEnableNotification();
                    }
                }
            }
        }, resetFilterRunnable = new Runnable() {
            @Override
            public void run() {
                filterTapped = false;
            }
        };

        OverlayBase.OnDoubleTapListener tapListener = new OverlayBase.OnDoubleTapListener() {

            @Override
            public void onDoubleTap(OverlayBase receiver) {
                if(receiver instanceof EasyLockOverlay){
                    handler.removeCallbacks(lockRunnable);
                    handler.postDelayed(lockRunnable, 10);
                } else if(receiver instanceof EasyLockFilterOverlay) {
                    filterTapped = true;
                    handler.removeCallbacks(resetFilterRunnable);
                    handler.postDelayed(resetFilterRunnable, 20);
                }
            }

            @Override
            public void onFirstTap(OverlayBase receiver) {
                if(receiver instanceof EasyLockOverlay && avoidSoftkeys) {
                    int count = 0;
                    do {
                        firstTapPackage = getForegroundAppPackage();
                        count++;
                        if(count == 10 && !PreferencesHelper.hasUsageAccess(EasyLockService.this)) {
                            PreferencesHelper.setPreference(
                                    EasyLockService.this, KEY_DETECT_SOFT_KEY, false);
                        } else if(count > 100) {
                            break;
                        }
                    } while (firstTapPackage.isEmpty());
                }
            }
        };
        lockOverlay = new EasyLockOverlay(this,
                (WindowManager) getSystemService(WINDOW_SERVICE), tapListener);
        filterOverlay = new EasyLockFilterOverlay(this,
                (WindowManager) getSystemService(WINDOW_SERVICE), tapListener);

        loadPreferences();

        PreferencesHelper.registerListener(new PreferencesHelper.PreferencesChangedListener() {
            @Override
            public void onPreferencesChanged() {
                postNotification();
                loadPreferences();
            }
        });
    }

    private void loadPreferences() {
        avoidSoftkeys = PreferencesHelper.getBoolPreference(this, KEY_DETECT_SOFT_KEY);
        supportSmartLock = PreferencesHelper.getBoolPreference(this, KEY_SUPPORT_SMART_LOCK);
        int timeout = PreferencesHelper.getIntPreference(this, KEY_DOUBLE_TAP_TIMEOUT, 200);
        filterOverlay.setDoubleTapTimeout(timeout);
        lockOverlay.setDoubleTapTimeout(timeout);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null) {
            String action = intent.getAction();
            if(ACTION_START_OVERLAY.equals(action)) {
                start(false);
            } else if(ACTION_STOP_OVERLAY.equals(action)) {
                stop(false);
            } else if(ACTION_TOGGLE.equals(action)) {
                toggle();
            } else if(ACTION_POST_NOTIFICATION.equals(action)) {
                postNotification();
            }
        }
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    private void toggle() {
        if(PreferencesHelper.getBoolPreference(this, PreferencesHelper.KEY_MASTER_SWITCH_ON)) {
            stop(true);
        } else {
            start(true);
        }
    }

    private void start(boolean sendBroadcast) {
        PreferencesHelper.setPreference(this, KEY_MASTER_SWITCH_ON, true);
        try {
            lockOverlay.execute();
            filterOverlay.execute();
        } catch (SecurityException | WindowManager.BadTokenException e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.enable_draw_over, Toast.LENGTH_SHORT).show();
            postDrawOverEnableNotification();
            PreferencesHelper.setPreference(this, KEY_MASTER_SWITCH_ON, false);
            return;
        }
        postNotification(getString(R.string.service_enabled), R.drawable.ic_stat_service_running);
        if(sendBroadcast) {
            LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(ACTION_START_OVERLAY));
        }
    }

    private void stop(boolean sendBroadcast) {
        PreferencesHelper.setPreference(this, KEY_MASTER_SWITCH_ON, false);
        lockOverlay.remove();
        filterOverlay.remove();
        postNotification(getString(R.string.service_disabled), R.drawable.ic_stat_service_paused);
        if(sendBroadcast) {
            LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(ACTION_STOP_OVERLAY));
        }
    }

    private String getForegroundAppPackage(){
        String foregroundTaskPackageName = "";
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            long time = System.currentTimeMillis();
            List<UsageStats> appList = mUsageStatsManager
                    .queryUsageStats(UsageStatsManager.INTERVAL_DAILY,  time - 1000*100, time);
            if (appList != null && appList.size() > 0) {
                SortedMap<Long, UsageStats> mySortedMap = new TreeMap<>();
                for (UsageStats usageStats : appList) {
                    mySortedMap.put(usageStats.getLastTimeUsed(), usageStats);
                }
                if (!mySortedMap.isEmpty()) {
                    foregroundTaskPackageName = mySortedMap
                            .get(mySortedMap.lastKey()).getPackageName();
                }
            }
        } else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            List<ActivityManager.RunningAppProcessInfo> tasks =
                    mActivityManager.getRunningAppProcesses();
            foregroundTaskPackageName = tasks.get(0).processName;
        } else {
            @SuppressWarnings("deprecation")
            ActivityManager.RunningTaskInfo foregroundTaskInfo = mActivityManager.getRunningTasks(1).get(0);
            foregroundTaskPackageName = foregroundTaskInfo.topActivity.getPackageName();
        }
        return foregroundTaskPackageName;
    }
}
