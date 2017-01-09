package com.symhung.adcombiner.adframework.base;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.view.ViewCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Created by HsinHung on 2017/1/9.
 */

public class VisibilityTracker {
    private static final String TAG = VisibilityTracker.class.getSimpleName();
    
    private static final int VISIBILITY_THROTTLE_MILLIS = 100;
    static final int NUM_ACCESSES_BEFORE_TRIMMING = 50;
    private final ArrayList<View> trimmedViews;
    private long accessCounter = 0;
    public interface VisibilityTrackerListener {
        void onVisibilityChanged(List<View> visibleViews, List<View> invisibleViews);
    }

    private final ViewTreeObserver.OnPreDrawListener onPreDrawListener;
    private WeakReference<ViewTreeObserver> weakViewTreeObserver;

    private static class TrackingInfo {
        int mMinViewablePercent;
        // Must be less than mMinVisiblePercent
        int mMaxInvisiblePercent;
        long mAccessOrder;
        View mRootView;
    }

    private final Map<View, TrackingInfo> trackedViews;

    private final VisibilityChecker mVisibilityChecker;

    private VisibilityTrackerListener mVisibilityTrackerListener;

    private final VisibilityRunnable mVisibilityRunnable;

    private final Handler mVisibilityHandler;

    private boolean mIsVisibilityScheduled;

    public VisibilityTracker(final Context context) {
        this(context,
                new WeakHashMap<View, TrackingInfo>(10),
                new VisibilityChecker(),
                new Handler());
    }

    private VisibilityTracker(final Context context,
                              final Map<View, TrackingInfo> trackedViews,
                              final VisibilityChecker visibilityChecker,
                              final Handler visibilityHandler) {
        this.trackedViews = trackedViews;
        mVisibilityChecker = visibilityChecker;
        mVisibilityHandler = visibilityHandler;
        mVisibilityRunnable = new VisibilityRunnable();
        trimmedViews = new ArrayList<>(NUM_ACCESSES_BEFORE_TRIMMING);

        onPreDrawListener = new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                scheduleVisibilityCheck();
                return true;
            }
        };

        weakViewTreeObserver = new WeakReference<>(null);
        setViewTreeObserver(context, null);
    }

    private void setViewTreeObserver(final Context context, final View view) {
        final ViewTreeObserver originalViewTreeObserver = weakViewTreeObserver.get();
        if (originalViewTreeObserver != null && originalViewTreeObserver.isAlive()) {
            return;
        }

        final View rootView = getTopmostView(context, view);
        if (rootView == null) {
            Log.d(TAG, "Unable to set Visibility Tracker due to no available root view.");
            return;
        }

        final ViewTreeObserver viewTreeObserver = rootView.getViewTreeObserver();
        if (!viewTreeObserver.isAlive()) {
            Log.w(TAG, "Visibility Tracker was unable to track views because the"
                    + " root view tree observer was not alive");
            return;
        }

        weakViewTreeObserver = new WeakReference<>(viewTreeObserver);
        viewTreeObserver.addOnPreDrawListener(onPreDrawListener);
    }

    public void setVisibilityTrackerListener(
            final VisibilityTrackerListener visibilityTrackerListener) {
        mVisibilityTrackerListener = visibilityTrackerListener;
    }

    public void addView(final View view, final int minPercentageViewed) {
        addView(view, view, minPercentageViewed);
    }

    public void addView(View rootView, final View view, final int minPercentageViewed) {
        addView(rootView, view, minPercentageViewed, minPercentageViewed);
    }

    public void addView(View rootView, final View view, final int minVisiblePercentageViewed, final int maxInvisiblePercentageViewed) {
        setViewTreeObserver(view.getContext(), view);

        // Find the view if already tracked
        TrackingInfo trackingInfo = trackedViews.get(view);
        if (trackingInfo == null) {
            trackingInfo = new TrackingInfo();
            trackedViews.put(view, trackingInfo);
            scheduleVisibilityCheck();
        }

        int maxInvisiblePercent = Math.min(maxInvisiblePercentageViewed, minVisiblePercentageViewed);

        trackingInfo.mRootView = rootView;
        trackingInfo.mMinViewablePercent = minVisiblePercentageViewed;
        trackingInfo.mMaxInvisiblePercent = maxInvisiblePercent;
        trackingInfo.mAccessOrder = accessCounter;

        // Trim the number of tracked views to a reasonable number
        accessCounter++;
        if (accessCounter % NUM_ACCESSES_BEFORE_TRIMMING == 0) {
            trimTrackedViews(accessCounter - NUM_ACCESSES_BEFORE_TRIMMING);
        }
    }

    private void trimTrackedViews(long minAccessOrder) {
        // Clear anything that is below minAccessOrder.
        for (final Map.Entry<View, TrackingInfo> entry : trackedViews.entrySet()) {
            if (entry.getValue().mAccessOrder <  minAccessOrder) {
                trimmedViews.add(entry.getKey());
            }
        }

        for (View view : trimmedViews) {
            removeView(view);
        }
        trimmedViews.clear();
    }

    private void removeView(final View view) {
        trackedViews.remove(view);
    }

    private void clear() {
        trackedViews.clear();
        mVisibilityHandler.removeMessages(0);
        mIsVisibilityScheduled = false;
    }

    public void destroy() {
        clear();
        final ViewTreeObserver viewTreeObserver = weakViewTreeObserver.get();
        if (viewTreeObserver != null && viewTreeObserver.isAlive()) {
            viewTreeObserver.removeOnPreDrawListener(onPreDrawListener);
        }
        weakViewTreeObserver.clear();
        mVisibilityTrackerListener = null;
    }

    private void scheduleVisibilityCheck() {
        // Tracking this directly instead of calling hasMessages directly because we measured that
        // this led to slightly better performance.
        if (mIsVisibilityScheduled) {
            return;
        }

        mIsVisibilityScheduled = true;
        mVisibilityHandler.postDelayed(mVisibilityRunnable, VISIBILITY_THROTTLE_MILLIS);
    }

    private class VisibilityRunnable implements Runnable {
        // Set of views that are visible or invisible. We create these once to avoid excessive
        // garbage collection observed when calculating these on each pass.
        private final ArrayList<View> visibleViews;
        private final ArrayList<View> invisibleViews;

        VisibilityRunnable() {
            invisibleViews = new ArrayList<>();
            visibleViews = new ArrayList<>();
        }

        @Override
        public void run() {
            mIsVisibilityScheduled = false;
            for (final Map.Entry<View, TrackingInfo> entry : trackedViews.entrySet()) {
                final View view = entry.getKey();
                final int minPercentageViewed = entry.getValue().mMinViewablePercent;
                final int maxInvisiblePercent = entry.getValue().mMaxInvisiblePercent;
                final View rootView = entry.getValue().mRootView;

                if (mVisibilityChecker.isVisible(rootView, view, minPercentageViewed)) {
                    visibleViews.add(view);
                } else if (!mVisibilityChecker.isVisible(rootView, view, maxInvisiblePercent)){
                    invisibleViews.add(view);
                }
            }

            if (mVisibilityTrackerListener != null) {
                mVisibilityTrackerListener.onVisibilityChanged(visibleViews, invisibleViews);
            }

            // Clear these immediately so that we don't leak memory
            visibleViews.clear();
            invisibleViews.clear();
        }
    }

    private static class VisibilityChecker {
        private final Rect clipRect = new Rect();

        boolean hasRequiredTimeElapsed(final long startTimeMillis, final int minTimeViewed) {
            return SystemClock.uptimeMillis() - startTimeMillis >= minTimeViewed;
        }

        boolean isVisible(final View rootView, final View view, final int minPercentageViewed) {
            // ListView & GridView both call detachFromParent() for views that can be recycled for
            // new data. This is one of the rare instances where a view will have a null parent for
            // an extended period of time and will not be the main window.
            // view.getGlobalVisibleRect() doesn't check that case, so if the view has visibility
            // of View.VISIBLE but it's group has no parent it is likely in the recycle bin of a
            // ListView / GridView and not on screen.
            if (view == null || view.getVisibility() != View.VISIBLE || rootView.getParent() == null) {
                return false;
            }

            if (!view.getGlobalVisibleRect(clipRect)) {
                // Not visible
                return false;
            }

            // % visible check - the cast is to avoid int overflow for large views.
            final long visibleViewArea = (long) clipRect.height() * clipRect.width();
            final long totalViewArea = (long) view.getHeight() * view.getWidth();

            if (totalViewArea <= 0) {
                return false;
            }

            return 100 * visibleViewArea >= minPercentageViewed * totalViewArea;
        }
    }

    public static View getTopmostView(final Context context, final View view) {
        final View rootViewFromActivity = getRootViewFromActivity(context);
        final View rootViewFromView = getRootViewFromView(view);

        // Prefer to use the rootView derived from the Activity's DecorView since it provides a
        // consistent value when the View is not attached to the Window. Fall back to the passed-in
        // View's hierarchy if necessary.
        return rootViewFromActivity != null
                ? rootViewFromActivity
                : rootViewFromView;
    }

    private static View getRootViewFromActivity(final Context context) {
        if (!(context instanceof Activity)) {
            return null;
        }

        return ((Activity) context).getWindow().getDecorView().findViewById(android.R.id.content);
    }

    private static View getRootViewFromView(final View view) {
        if (view == null) {
            return null;
        }

        if (!ViewCompat.isAttachedToWindow(view)) {
            Log.d(TAG, "Attempting to call View#getRootView() on an unattached View.");
        }

        final View rootView = view.getRootView();

        if (rootView == null) {
            return null;
        }

        final View rootContentView = rootView.findViewById(android.R.id.content);
        return rootContentView != null
                ? rootContentView
                : rootView;
    }
}
