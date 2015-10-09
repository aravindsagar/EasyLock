package com.sagar.easylock.service.overlay;

import android.content.Context;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

/**
 * Created by aravind on 23/9/14.
 * A base class for managing overlay windows.
 */
public abstract class OverlayBase {
    protected static final String LOG_TAG = OverlayBase.class.getSimpleName();

    private LayoutInflater inflater;
    private WindowManager windowManager;
    private Context context;
    private View layout;
    private WindowManager.LayoutParams params;
    private OnDoubleTapListener mListener;
    private int mDoubleTapTimeout= 200;

    private boolean isAdded = false;

    public OverlayBase(Context context, WindowManager windowManager, OnDoubleTapListener listener){
        this.context = context;
        inflater = LayoutInflater.from(context);
        this.windowManager = windowManager;
        mListener = listener;
    }

    protected abstract WindowManager.LayoutParams buildLayoutParams();

    /**
     * Gets the layout to be shown by the window manager.
     * Recycling of already inflated view must be taken care of by the implementation. Else memory leaks can occur!
     * @return layout to be shown by the window manager.
     */
    protected abstract View buildLayout();

    /**
     * Adds the view specified by buildLayout() to the windowManager passed during initialization
     */
    public void execute(){
        layout = buildLayout();
        if(isAdded) return;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            layout.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }
        if(params == null){
            params = buildLayoutParams();
        }
        try {
            windowManager.addView(layout, params);
            isAdded = true;
            layout.invalidate();
        } catch (IllegalStateException e){
            isAdded = false;
        }
    }

    /**
     * Removes the view specified by buildLayout() from the windowManager passed during initialization
     */
    public void remove(){
        if(!isAdded) return;
        try {
            windowManager.removeView(layout);
            isAdded = false;
        } catch (Exception e){
            //Do nothing
        }
    }

    protected int getDisplayWidth(){
        DisplayMetrics displaymetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displaymetrics);
        return displaymetrics.widthPixels;
    }

    protected int getDisplayHeight(){
        DisplayMetrics displaymetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displaymetrics);
        return displaymetrics.heightPixels;
    }

    public LayoutInflater getInflater() {
        return inflater;
    }

    public Context getContext() {
        return context;
    }

    protected void onDoubleTap(){
        if(mListener != null){
            mListener.onDoubleTap(this);
        }
    }

    protected void onFirstTap() {
        if(mListener != null){
            mListener.onFirstTap(this);
        }
    }

    public void setDoubleTapTimeout(int timeout) {
        mDoubleTapTimeout = timeout;
    }

    protected int getDoubleTapTimeout() {
        return mDoubleTapTimeout;
    }

    public interface OnDoubleTapListener {
        /**
         * Called when a double tap is received by the overlay.
         * @param receiver The overlay that received the double tap.
         */
        void onDoubleTap(OverlayBase receiver);

        /**
         * Called on all other taps.
         * @param receiver The overlay that received the double tap.
         */
        void onFirstTap(OverlayBase receiver);
    }
}
