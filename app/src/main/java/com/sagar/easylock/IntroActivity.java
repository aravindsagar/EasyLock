package com.sagar.easylock;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import static com.sagar.easylock.PreferencesHelper.KEY_HAS_VIEWED_INTRO;

public class IntroActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);
        Button getStartedButton = (Button) findViewById(R.id.button_get_started);
        getStartedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Set KEY_HAS_VIEWED_INTRO to true so that this activity won't be shown again
                // automatically when the app is opened.
                PreferencesHelper.setPreference(IntroActivity.this, KEY_HAS_VIEWED_INTRO, true);

                // Close this activity. Instead of usual activity transition, fade animations
                // will be better here.
                finish();
                overridePendingTransition(R.anim.abc_fade_in, R.anim.abc_fade_out);
            }
        });
    }
}
