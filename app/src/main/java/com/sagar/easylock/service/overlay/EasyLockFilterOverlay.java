package com.sagar.easylock.service.overlay;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import com.sagar.easylock.PreferencesHelper;
import com.sagar.easylock.R;

import static com.sagar.easylock.PreferencesHelper.KEY_STATUS_BAR_HEIGHT;

/**
 * Created by aravind on 30/6/15.
 */
public class EasyLockFilterOverlay extends OverlayBase {
    View rootView;
    View innerView;
    long lastEventTime;

    public EasyLockFilterOverlay(Context context, WindowManager windowManager, OnDoubleTapListener listener) {
        super(context, windowManager, listener);
    }

    @SuppressLint("RtlHardcoded")
    @Override
    protected WindowManager.LayoutParams buildLayoutParams() {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);

        params.x = 0;
        params.y = 0;
        params.gravity = Gravity.TOP | Gravity.RIGHT;
        return params;
    }

    /**
     * Gets the layout to be shown by the window manager.
     * Recycling of already inflated view must be taken care of by the implementation. Else memory leaks can occur!
     *
     * @return layout to be shown by the window manager.
     */
    @Override
    protected View buildLayout() {
        if(rootView == null) {
            rootView = getInflater().inflate(R.layout.easy_shift_overlay, null, false);
            innerView = rootView.findViewById(R.id.view_overlay);
        }
        int sbHeight = PreferencesHelper.getIntPreference(getContext(), KEY_STATUS_BAR_HEIGHT, -1);
        if(sbHeight == -1) {
            sbHeight = (int) getContext().getResources().getDimension(R.dimen.status_bar_default_height);
        }
        innerView.setLayoutParams(new RelativeLayout.LayoutParams(2, sbHeight));
        rootView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                long currentEventTime = motionEvent.getEventTime();
                if(currentEventTime-lastEventTime <= 200){
                    onDoubleTap();
                } else {
                    lastEventTime = currentEventTime;
                }
                return false;
            }
        });
        return rootView;
    }
}
