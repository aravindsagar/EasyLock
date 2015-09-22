package com.sagar.easylock;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.sagar.easylock.screenlock.AdminActions;
import com.sagar.easylock.service.EasyLockService;

import static com.sagar.easylock.service.EasyLockService.ACTION_START_OVERLAY;
import static com.sagar.easylock.service.EasyLockService.ACTION_STOP_OVERLAY;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AdminActions.initAdmin(this);
        setContentView(R.layout.activity_main);
        startService(new Intent(this, EasyLockService.class).setAction(ACTION_START_OVERLAY));
        saveStatusBarHeight();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startService(new Intent(this, EasyLockService.class).setAction(ACTION_STOP_OVERLAY));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    private void saveStatusBarHeight() {
        int height = getStatusBarHeight();
        PreferencesHelper.setPreference(this, PreferencesHelper.KEY_STATUS_BAR_HEIGHT, height);
    }
}
