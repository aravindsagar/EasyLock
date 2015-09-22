package com.sagar.easylock.service;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.view.WindowManager;
import android.widget.Toast;

import com.sagar.easylock.screenlock.AdminActions;
import com.sagar.easylock.screenlock.RootHelper;
import com.sagar.easylock.service.overlay.EasyLockFilterOverlay;
import com.sagar.easylock.service.overlay.EasyLockOverlay;
import com.sagar.easylock.service.overlay.OverlayBase;

import java.util.List;

public class EasyLockService extends Service {

    public static final String ACTION_START_OVERLAY = "action_start_overlay";
    public static final String ACTION_STOP_OVERLAY = "action_stop_overlay";

    OverlayBase lockOverlay, filterOverlay;

    private boolean filterTapped = false;
    private String firstTapPackage = "";
    private ActivityManager mActivityManager;

    public EasyLockService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mActivityManager = (ActivityManager) getBaseContext().getSystemService(Context.ACTIVITY_SERVICE);

        final Runnable lockRunnable = new Runnable() {
            @Override
            public void run() {
                if(!filterTapped && firstTapPackage.equals(getForegroundAppPackage())) {
                    if(!RootHelper.lockNow() && !AdminActions.turnScreenOff()) {
                        Toast.makeText(EasyLockService.this, "Please enable administrator privileges for Easy Lock.", Toast.LENGTH_SHORT).show();
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
                Handler handler = new Handler();
                if(receiver instanceof EasyLockOverlay){
//                    Log.d("EasyLockService", "Double tap main overlay");
                    handler.removeCallbacks(lockRunnable);
                    handler.postDelayed(lockRunnable, 10);
                } else if(receiver instanceof EasyLockFilterOverlay) {
//                    Log.d("EasyLockService", "Double tap filter overlay");
                    filterTapped = true;
                    handler.removeCallbacks(resetFilterRunnable);
                    handler.postDelayed(resetFilterRunnable, 20);
                }
            }

            @Override
            public void onFirstTap(OverlayBase receiver) {
                if(receiver instanceof EasyLockOverlay) {
                    firstTapPackage = getForegroundAppPackage();
                }
            }
        };
        lockOverlay = new EasyLockOverlay(this,
                (WindowManager) getSystemService(WINDOW_SERVICE), tapListener);
        filterOverlay = new EasyLockFilterOverlay(this,
                (WindowManager) getSystemService(WINDOW_SERVICE), tapListener);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null) {
            String action = intent.getAction();
            if(ACTION_START_OVERLAY.equals(action)) {
                lockOverlay.execute();
                filterOverlay.execute();
            } else if(ACTION_STOP_OVERLAY.equals(action)) {
                lockOverlay.remove();
                filterOverlay.remove();
            }
        }
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    private String getForegroundAppPackage() {
        String foregroundTaskPackageName;
        if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            ActivityManager.RunningTaskInfo foregroundTaskInfo = mActivityManager.getRunningTasks(1).get(0);

            foregroundTaskPackageName = foregroundTaskInfo.topActivity.getPackageName();
        } else {
            List<ActivityManager.RunningAppProcessInfo> tasks = mActivityManager.getRunningAppProcesses();
            foregroundTaskPackageName = tasks.get(0).processName;
        }
        if(foregroundTaskPackageName == null) {
            foregroundTaskPackageName = "";
        }
        return foregroundTaskPackageName;
    }
}
