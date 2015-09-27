package com.sagar.easylock.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.sagar.easylock.PreferencesHelper;

import static com.sagar.easylock.PreferencesHelper.KEY_MASTER_SWITCH_ON;
import static com.sagar.easylock.PreferencesHelper.KEY_START_ON_BOOT;
import static com.sagar.easylock.service.EasyLockService.ACTION_START_OVERLAY;
import static com.sagar.easylock.service.EasyLockService.ACTION_STOP_OVERLAY;

/**
 * Created by aravind on 27/9/15.
 */
public class BootCompleteReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("BootCompleteReceiver", "Boot complete. :)");
        if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())){

            if(PreferencesHelper.getBoolPreference(context, KEY_START_ON_BOOT, true)) {
                if (PreferencesHelper.getBoolPreference(context, KEY_MASTER_SWITCH_ON)) {
                    context.startService(new Intent(context, EasyLockService.class)
                            .setAction(ACTION_START_OVERLAY));
                } else {
                    context.startService(new Intent(context, EasyLockService.class)
                            .setAction(ACTION_STOP_OVERLAY));
                }
            }
        }
    }
}