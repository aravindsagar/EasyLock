package com.sagar.easylock.screenlock;

import android.app.Activity;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.sagar.easylock.R;

/**
 * Created by aravind on 30/6/15.
 */
public class AdminActions extends DeviceAdminReceiver {

    private static DevicePolicyManager mDPM;
    private static ComponentName mDeviceAdmin;

    @Override
    public void onEnabled(Context context, Intent intent) {
        super.onEnabled(context, intent);
        AdminActions.initializeAdminObjects(context);
    }

    public static void initAdmin(Context activityContext){
        if(!(activityContext instanceof Activity)){
            Log.e("AdminActions", "Context passed should be an activity context");
            return;
        }
        initializeAdminObjects(activityContext);
        if(!isAdminEnabled()){
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mDeviceAdmin);
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    R.string.device_admin_description);
            activityContext.startActivity(intent);
        }
    }

    public static void initializeAdminObjects(Context context){
        if(mDPM == null || mDeviceAdmin == null) {
            mDPM = (DevicePolicyManager) context.
                    getSystemService(Context.DEVICE_POLICY_SERVICE);
            mDeviceAdmin = new ComponentName(context, AdminActions.class);
        }
    }

    public static boolean isAdminEnabled(){
        return mDPM != null && mDeviceAdmin != null && mDPM.isAdminActive(mDeviceAdmin);
    }

    public static boolean turnScreenOff(Context context){
        initializeAdminObjects(context);
        if(isAdminEnabled()){
            mDPM.lockNow();
            return true;
        } else {
            Log.e("EasyLock", "No admin privileges, cannot change password.");
            return false;
        }
    }
}
