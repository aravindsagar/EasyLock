package com.sagar.easylock;

import android.annotation.TargetApi;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.sagar.easylock.drawables.CheckMarkView;
import com.sagar.easylock.iab_util.IabHelper;
import com.sagar.easylock.iab_util.IabResult;
import com.sagar.easylock.iab_util.InAppBillingPublicKey;
import com.sagar.easylock.iab_util.Inventory;
import com.sagar.easylock.iab_util.Purchase;
import com.sagar.easylock.screenlock.AdminActions;
import com.sagar.easylock.service.EasyLockService;

import java.util.Date;

import eu.chainfire.libsuperuser.Shell;

import static android.widget.Toast.LENGTH_SHORT;
import static com.sagar.easylock.PreferencesHelper.KEY_AVOID_LOCKSCREEN;
import static com.sagar.easylock.PreferencesHelper.KEY_DETECT_SOFT_KEY;
import static com.sagar.easylock.PreferencesHelper.KEY_DOUBLE_TAP_TIMEOUT;
import static com.sagar.easylock.PreferencesHelper.KEY_HAS_VIEWED_INTRO;
import static com.sagar.easylock.PreferencesHelper.KEY_MASTER_SWITCH_ON;
import static com.sagar.easylock.PreferencesHelper.KEY_SHOW_NOTIFICATION;
import static com.sagar.easylock.PreferencesHelper.KEY_START_ON_BOOT;
import static com.sagar.easylock.PreferencesHelper.KEY_STATUS_BAR_HEIGHT;
import static com.sagar.easylock.PreferencesHelper.KEY_SUPPORT_SMART_LOCK;
import static com.sagar.easylock.PreferencesHelper.getBoolPreference;
import static com.sagar.easylock.PreferencesHelper.getIntPreference;
import static com.sagar.easylock.PreferencesHelper.setPreference;
import static com.sagar.easylock.service.EasyLockService.ACTION_START_OVERLAY;
import static com.sagar.easylock.service.EasyLockService.ACTION_STOP_OVERLAY;

public class MainActivity extends AppCompatActivity {
    private static final String KEY_CHECK_USAGE_ACCESS_ON_RESUME = "check_usage_access_on_resume";

    private SwitchCompat masterSwitch;
    private CheckBox avoidSoftkeyCheckBox;

    private long lastTapTime=0;

    // Globals related to IAB
    private IabHelper mHelper;
    boolean mHasDonated = false;
    static final String SKU_DONATE = "com.sagar.easylock.donate"; //"android.test.purchased"; //
    // (arbitrary) request code for the purchase flow
    static final int RC_REQUEST = 10001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Show intro if it has not been shown yet
        if (!getBoolPreference(this, KEY_HAS_VIEWED_INTRO)) {
            //Log.d("EasyLock", "Intro");
            setPreference(this, KEY_SUPPORT_SMART_LOCK, false);
            setPreference(this, KEY_AVOID_LOCKSCREEN, false);
            showIntro();
        }

        AdminActions.initAdmin(this);
        setContentView(R.layout.activity_main);
        saveStatusBarHeight();
        setUpToolbar();
        setUpMoreCard();
        setUpIAB();
    }

    private void setUpIAB() {
        String base64EncodedPublicKey = InAppBillingPublicKey.getPublicKey();

        // compute your public key and store it in base64EncodedPublicKey
        mHelper = new IabHelper(this, base64EncodedPublicKey);
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                if (!result.isSuccess()) {
                    // Oh noes, there was a problem.
                    Log.d("EasyLock", "Problem setting up In-app Billing: " + result);
                    return;
                }
                // Hooray, IAB is fully set up!
                // Have we been disposed of in the meantime? If so, quit.
                if (mHelper == null) return;

                // IAB is fully set up. Now, let's get an inventory of stuff we own.
                Log.d("EasyLock", "Setup successful. Querying inventory.");
                mHelper.queryInventoryAsync(mGotInventoryListener);
            }
        });
    }

    // Listener that's called when we finish querying the items and subscriptions we own
    IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {

            // Have we been disposed of in the meantime? If so, quit.
            if (mHelper == null) return;

            // Is it a failure?
            if (result.isFailure()) {
                complain("Failed to query inventory: " + result);
                return;
            }

            /*
             * Check for items we own. Notice that for each purchase, we check
             * the developer payload to see if it's correct! See
             * verifyDeveloperPayload().
             */

            Purchase donatePurchase = inventory.getPurchase(SKU_DONATE);
            mHasDonated = (donatePurchase != null);
            if(mHasDonated) {
                mHelper.consumeAsync(inventory.getPurchase(SKU_DONATE), mConsumeFinishedListener);
            }
        }
    };

    IabHelper.OnConsumeFinishedListener mConsumeFinishedListener = new IabHelper.OnConsumeFinishedListener() {
        public void onConsumeFinished(Purchase purchase, IabResult result) {
            if(result.isSuccess()) {
                Toast.makeText(MainActivity.this, "Thank you for your contribution. :)", LENGTH_SHORT).show();
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();

        if(Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
            new AlertDialog.Builder(this).setMessage(getString(R.string.enable_draw_over_dialog))
                    .setTitle(getString(R.string.permission_required))
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @TargetApi(Build.VERSION_CODES.M)
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:" + getPackageName()));
                            startActivityForResult(intent, 10);
                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Toast.makeText(MainActivity.this, R.string.enable_draw_over, LENGTH_SHORT)
                                    .show();
                            setPreference(MainActivity.this, KEY_MASTER_SWITCH_ON, false);
                            finish();
                        }
                    })
                    .show();

        }

        setUpOptions();
        if(getBoolPreference(this, KEY_CHECK_USAGE_ACCESS_ON_RESUME)) {
            if (PreferencesHelper.hasUsageAccess(this)) {
                avoidSoftkeyCheckBox.setChecked(true);
            } else {
                avoidSoftkeyCheckBox.setChecked(false);
                Toast.makeText(MainActivity.this,
                        R.string.message_enable_usage_access_for_per_app_profiles,
                        Toast.LENGTH_SHORT).show();
            }
            setPreference(this, KEY_CHECK_USAGE_ACCESS_ON_RESUME, false);
        }
        IntentFilter filter = new IntentFilter(ACTION_START_OVERLAY);
        filter.addAction(EasyLockService.ACTION_STOP_OVERLAY);
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(receiver, filter);

        boolean masterSwitchOn = getBoolPreference(this, KEY_MASTER_SWITCH_ON);
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
        boolean masterSwitchOn = getBoolPreference(this, KEY_MASTER_SWITCH_ON);
        if(masterSwitch.isChecked() == masterSwitchOn){
            if(masterSwitchOn) enableService();
            else disableService();
        } else {
            masterSwitch.setChecked(masterSwitchOn);
        }
    }

    private void setUpOptions() {
        setUpDoubleTapTimeoutControls();
        // Binding the views to objects
        final CheckBox startOnBootCheckBox = (CheckBox) findViewById(R.id.checkBox_start_on_boot),
                 showNotificationCheckBox  = (CheckBox) findViewById(R.id.checkBox_show_notification),
                 smartLockCheckBox         = (CheckBox) findViewById(R.id.checkBox_enable_smart_lock_support),
                 avoidLockscreenCheckBox   = (CheckBox) findViewById(R.id.checkBox_avoid_lockscreen)/*,
                 touchAnywhereCheckbox     = (CheckBox) findViewById(R.id.checkBox_touch_anywhere)*/;
        avoidSoftkeyCheckBox = (CheckBox) findViewById(R.id.checkBox_avoid_soft_key);

        // Loading the saved preferences
        startOnBootCheckBox.setChecked(getBoolPreference(this, KEY_START_ON_BOOT, true));
        avoidSoftkeyCheckBox.setChecked(getBoolPreference(this, KEY_DETECT_SOFT_KEY));
        showNotificationCheckBox.setChecked(getBoolPreference(this, KEY_SHOW_NOTIFICATION, true));
        smartLockCheckBox.setChecked(getBoolPreference(this, KEY_SUPPORT_SMART_LOCK));
        avoidLockscreenCheckBox.setChecked(getBoolPreference(this, KEY_AVOID_LOCKSCREEN));
        /*touchAnywhereCheckbox.setChecked(getBoolPreference(this, KEY_TOUCH_ANYWHERE));*/

        smartLockCheckBox.setVisibility(View.GONE);
        final View smartLockSeparator = findViewById(R.id.separator_smart_lock);
        smartLockSeparator.setVisibility(View.GONE);
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voids) {
                return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && Shell.SU.available();
            }

            @Override
            protected void onPostExecute(Boolean showSmartLock) {
                super.onPostExecute(showSmartLock);
                if(showSmartLock) {
                    smartLockCheckBox.setVisibility(View.VISIBLE);
                    smartLockSeparator.setVisibility(View.VISIBLE);
                    smartLockCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                            setPreference(MainActivity.this, KEY_SUPPORT_SMART_LOCK, b);
                        }
                    });
                }
            }
        }.execute();

        // Listeners to validate and save data when preferences are changed.
        startOnBootCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                setPreference(MainActivity.this, KEY_START_ON_BOOT, b);
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
                                            setPreference(MainActivity.this,
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
                                                setPreference(MainActivity.this,
                                                        KEY_DETECT_SOFT_KEY,
                                                        true);
                                            }
                                        }
                                    })
                            .setCancelable(false)
                            .show();
                } else {
                    setPreference(MainActivity.this, KEY_DETECT_SOFT_KEY, false);
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
                                    setPreference(MainActivity.this, KEY_SHOW_NOTIFICATION, false);
                                }
                            })
                            .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    setPreference(MainActivity.this, KEY_SHOW_NOTIFICATION, true);
                                    compoundButton.setChecked(true);
                                }
                            })
                            .setCancelable(false)
                            .show();
                } else {
                    setPreference(MainActivity.this, KEY_SHOW_NOTIFICATION, true);
                }
            }
        });

        View avoidLockscreenSeparator = findViewById(R.id.separator_avoid_lockscreen);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            avoidLockscreenCheckBox.setVisibility(View.VISIBLE);
            avoidLockscreenSeparator.setVisibility(View.VISIBLE);
            avoidLockscreenCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    setPreference(MainActivity.this, KEY_AVOID_LOCKSCREEN, b);
                }
            });
        } else {
            avoidLockscreenCheckBox.setVisibility(View.GONE);
            avoidLockscreenSeparator.setVisibility(View.GONE);
        }

        /*touchAnywhereCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(final CompoundButton compoundButton, boolean b) {
                if (b) {
                    new AlertDialog.Builder(MainActivity.this)
                            .setMessage(getString(R.string.warning_touch_anywhere))
                            .setNegativeButton(getString(R.string.no),
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            compoundButton.setChecked(false);
                                            setPreference(MainActivity.this,
                                                    KEY_TOUCH_ANYWHERE,
                                                    false);
                                        }
                                    })
                            .setPositiveButton(getString(R.string.yes),
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            setPreference(MainActivity.this,
                                                    KEY_TOUCH_ANYWHERE,
                                                    true);
                                        }
                                    })
                            .setCancelable(false)
                            .show();
                } else {
                    setPreference(MainActivity.this, KEY_TOUCH_ANYWHERE, false);
                }
            }
        });*/
    }

    private void setUpDoubleTapTimeoutControls() {

        // Setting up the button to test double tap timeout
        final CheckMarkView view = (CheckMarkView) findViewById(R.id.check_mark_test);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                long currentTime = new Date().getTime();
                int timeout = getIntPreference(MainActivity.this, KEY_DOUBLE_TAP_TIMEOUT, 200);
                if (currentTime - lastTapTime <= timeout) {
                    view.toggle();
                    lastTapTime = 0;
                } else {
                    lastTapTime = currentTime;
                }
            }
        });

        // Setting up timout selection seekbar
        // Minimum value is 100ms and maximum is 750ms

        final int min = 100, max = 750;
        SeekBar timeoutSeekBar = (SeekBar) findViewById(R.id.seek_bar_timeout);
        final TextView labelTextView = (TextView) findViewById(R.id.text_view_double_tap_timeout);
        timeoutSeekBar.setMax(max - min);
        int currentValue = getIntPreference(MainActivity.this,
                KEY_DOUBLE_TAP_TIMEOUT, 200);
        timeoutSeekBar.setProgress(currentValue - min);
        labelTextView.setText(String.format("%s : %dms",
                getString(R.string.double_tap_timeout), currentValue));
        timeoutSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    setPreference(MainActivity.this, KEY_DOUBLE_TAP_TIMEOUT, progress + min);
                }
                labelTextView.setText(String.format("%s : %dms",
                        getString(R.string.double_tap_timeout), progress + min));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private void setUpMoreCard() {
        findViewById(R.id.text_quick_intro).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showIntro();
            }
        });

        findViewById(R.id.text_about).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, AboutActivity.class));
            }
        });

        findViewById(R.id.text_donate).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mHelper != null) {
                    try {
                        mHelper.launchPurchaseFlow(MainActivity.this, SKU_DONATE, RC_REQUEST, mPurchaseFinishedListener);
                    } catch (IllegalStateException e) {
                        complain(getString(R.string.prev_in_progress_try_later));
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Cannot start in-app purchase.", LENGTH_SHORT).show();
                }
            }
        });

        findViewById(R.id.text_share_app).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_string) +
                        " http://aravindsagar.github.io/EasyLock/");
                sendIntent.setType("text/plain");
                startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.send_to)));
            }
        });

        findViewById(R.id.text_rate_review).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri uri = Uri.parse("market://details?id=" + MainActivity.this.getPackageName());
                Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
                // To count with Play market backstack, After pressing back button,
                // to taken back to our application, we need to add following flags to intent.
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    //noinspection deprecation
                    goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                            Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET |
                            Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                } else {
                    goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                            Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                            Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                }
                try {
                    startActivity(goToMarket);
                } catch (ActivityNotFoundException e) {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("http://play.google.com/store/apps/details?id=" +
                                    MainActivity.this.getPackageName())));
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
                        setPreference(MainActivity.this, KEY_CHECK_USAGE_ACCESS_ON_RESUME, true);
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

    private void showIntro() {
        startActivity(new Intent(this, IntroActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
        overridePendingTransition(R.anim.abc_fade_in, R.anim.abc_fade_out);
    }

    private void disableService() {
        startService(new Intent(this, EasyLockService.class).setAction(ACTION_STOP_OVERLAY));
    }

    private void enableService() {
        startService(new Intent(this, EasyLockService.class).setAction(ACTION_START_OVERLAY));
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
        setPreference(this, KEY_STATUS_BAR_HEIGHT, height);
    }
    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            // if we were disposed of in the meantime, quit.
            if (mHelper == null) return;

            if (result.isFailure()) {
                complain(getString(R.string.donate_failed_message));
                return;
            }
            if(purchase.getSku().equals(SKU_DONATE)) {
                mHelper.consumeAsync(purchase, mConsumeFinishedListener);
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mHelper != null) {
            try {
                mHelper.dispose();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
        mHelper = null;
    }

    private void complain(String message) {
        Log.e("EasyLock", "**** IAB Error: " + message);
        alert("Error: " + message);
    }

    private void alert(String message) {
        AlertDialog.Builder bld = new AlertDialog.Builder(this);
        bld.setMessage(message);
        bld.setNeutralButton("OK", null);
        Log.d("EasyLock", "Showing alert dialog: " + message);
        bld.create().show();
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
