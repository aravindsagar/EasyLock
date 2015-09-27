package com.sagar.easylock;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.sagar.easylock.screenlock.AdminActions;
import com.sagar.easylock.service.EasyLockService;

import static com.sagar.easylock.PreferencesHelper.KEY_DETECT_SOFT_KEY;
import static com.sagar.easylock.PreferencesHelper.KEY_MASTER_SWITCH_ON;
import static com.sagar.easylock.PreferencesHelper.KEY_SHOW_NOTIFICATION;
import static com.sagar.easylock.PreferencesHelper.KEY_START_ON_BOOT;
import static com.sagar.easylock.service.EasyLockService.ACTION_START_OVERLAY;
import static com.sagar.easylock.service.EasyLockService.ACTION_STOP_OVERLAY;

public class MainActivity extends AppCompatActivity {
    private static final String KEY_CHECK_USAGE_ACCESS_ON_RESUME = "check_usage_access_on_resume";

    private SwitchCompat masterSwitch;
    private CheckBox avoidSoftkeyCheckBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AdminActions.initAdmin(this);
        setContentView(R.layout.activity_main);
        saveStatusBarHeight();
        setUpToolbar();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpOptions();
        if(PreferencesHelper.getBoolPreference(this, KEY_CHECK_USAGE_ACCESS_ON_RESUME)) {
            if (PreferencesHelper.hasUsageAccess(this)) {
                avoidSoftkeyCheckBox.setChecked(true);
            } else {
                avoidSoftkeyCheckBox.setChecked(false);
                Toast.makeText(MainActivity.this,
                        R.string.message_enable_usage_access_for_per_app_profiles,
                        Toast.LENGTH_SHORT).show();
            }
            PreferencesHelper.setPreference(this, KEY_CHECK_USAGE_ACCESS_ON_RESUME, false);
        }
        IntentFilter filter = new IntentFilter(ACTION_START_OVERLAY);
        filter.addAction(EasyLockService.ACTION_STOP_OVERLAY);
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(receiver, filter);

        boolean masterSwitchOn = PreferencesHelper.getBoolPreference(this, KEY_MASTER_SWITCH_ON);
        if(masterSwitch.isChecked() == masterSwitchOn){
            if(masterSwitchOn) enableService();
            else disableService();
        } else {
            masterSwitch.setChecked(masterSwitchOn);
        }
    }

    private void setUpToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setElevation(getResources().getDimension(R.dimen.toolbar_elevation));
        }
        toolbar.setTitle(R.string.app_name);
        toolbar.setTitleTextColor(Color.WHITE);
        masterSwitch = new SwitchCompat(this);
        masterSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    enableService();
                } else {
                    disableService();
                }
            }
        });
        Toolbar.LayoutParams params = new Toolbar.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.END;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            params.setMarginEnd((int) getResources().getDimension(R.dimen.activity_vertical_margin));
        } else {
            params.setMargins(0,0,(int) getResources().getDimension(R.dimen.activity_vertical_margin),0);
        }
        toolbar.addView(masterSwitch, params);
        boolean masterSwitchOn = PreferencesHelper.getBoolPreference(this, KEY_MASTER_SWITCH_ON);
        if(masterSwitch.isChecked() == masterSwitchOn){
            if(masterSwitchOn) enableService();
            else disableService();
        } else {
            masterSwitch.setChecked(masterSwitchOn);
        }
    }

    private void setUpOptions() {
        // Binding the views to objects
        CheckBox startOnBootCheckBox      = (CheckBox) findViewById(R.id.checkBox_start_on_boot);
        avoidSoftkeyCheckBox = (CheckBox) findViewById(R.id.checkBox_avoid_soft_key);
        CheckBox showNotificationCheckBox = (CheckBox) findViewById(R.id.checkBox_show_notification);

        // Loading the saved preferences
        startOnBootCheckBox.setChecked(
                PreferencesHelper.getBoolPreference(this, KEY_START_ON_BOOT, true));
        avoidSoftkeyCheckBox.setChecked(
                PreferencesHelper.getBoolPreference(this, KEY_DETECT_SOFT_KEY));
        showNotificationCheckBox.setChecked(
                PreferencesHelper.getBoolPreference(this, KEY_SHOW_NOTIFICATION, true));

        // Listeners to validate and save data when preferences are changed.
        startOnBootCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                PreferencesHelper.setPreference(MainActivity.this, KEY_START_ON_BOOT, b);
            }
        });

        avoidSoftkeyCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(final CompoundButton compoundButton, boolean b) {
                if (b) {
                    new AlertDialog.Builder(MainActivity.this)
                            .setMessage(getString(R.string.warning_avoid_softkeys))
                            .setNegativeButton(getString(R.string.no),
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            compoundButton.setChecked(false);
                                            PreferencesHelper.setPreference(MainActivity.this,
                                                    KEY_DETECT_SOFT_KEY,
                                                    false);
                                        }
                                    })
                            .setPositiveButton(getString(R.string.yes),
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                                                    && !PreferencesHelper.hasUsageAccess(MainActivity.this)) {
                                                showEnableUsageAccess();
                                            } else {
                                                PreferencesHelper.setPreference(MainActivity.this,
                                                        KEY_DETECT_SOFT_KEY,
                                                        true);
                                            }
                                        }
                                    })
                            .setCancelable(false)
                            .show();
                } else {
                    PreferencesHelper.setPreference(MainActivity.this, KEY_DETECT_SOFT_KEY, false);
                }

            }
        });

        showNotificationCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(final CompoundButton compoundButton, boolean b) {
                if (!b) {
                    new AlertDialog.Builder(MainActivity.this)
                            .setMessage(getString(R.string.warning_notification))
                            .setTitle(getString(R.string.warning))
                            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    PreferencesHelper.setPreference(MainActivity.this,
                                            KEY_SHOW_NOTIFICATION,
                                            false);
                                }
                            })
                            .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    PreferencesHelper.setPreference(MainActivity.this,
                                            KEY_SHOW_NOTIFICATION,
                                            true);
                                    compoundButton.setChecked(true);
                                }
                            })
                            .setCancelable(false)
                            .show();
                } else {
                    PreferencesHelper.setPreference(MainActivity.this, KEY_SHOW_NOTIFICATION, true);
                }
            }
        });
    }

    private void showEnableUsageAccess() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP
                || PreferencesHelper.hasUsageAccess(this)) {
            return;
        }
        new AlertDialog.Builder(this).setTitle(getString(R.string.heading_enable_usage_access))
                .setMessage(getString(R.string.message_enable_usage_access))
                .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        if (intent.resolveActivity(getPackageManager()) != null) {
                            startActivity(intent);
                        } else {
                            intent = new Intent(Settings.ACTION_SECURITY_SETTINGS);
                            intent.setComponent(new ComponentName("com.android.settings",
                                    "com.android.settings.Settings$SecuritySettingsActivity"));
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        }
                        PreferencesHelper.setPreference(
                                MainActivity.this,
                                KEY_CHECK_USAGE_ACCESS_ON_RESUME,
                                true);
                    }
                })
                .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Toast.makeText(MainActivity.this,
                                R.string.message_enable_usage_access_for_per_app_profiles,
                                Toast.LENGTH_SHORT).show();
                        avoidSoftkeyCheckBox.setChecked(false);
                    }
                })
                .setCancelable(false)
                .show();
    }

    private void disableService() {
        startService(new Intent(this, EasyLockService.class).setAction(ACTION_STOP_OVERLAY));
    }

    private void enableService() {
        startService(new Intent(this, EasyLockService.class).setAction(ACTION_START_OVERLAY));
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

    private BroadcastReceiver receiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            if(ACTION_START_OVERLAY.equals(intent.getAction())) {
                masterSwitch.setChecked(true);
            } else if(ACTION_STOP_OVERLAY.equals(intent.getAction())) {
                masterSwitch.setChecked(false);
            }
        }
    };
}
