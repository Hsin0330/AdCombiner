package com.symhung.adcombiner.adframework.base;

import android.content.Context;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by HsinHung on 2017/1/9.
 */

public abstract class TransferAdSource {

    private static final int CACHE_LIMIT = 1;

    private static final int EXPIRATION_TIME_MILLISECONDS = 15 * 60 * 1000; // 15 minutes
    private static final int MAXIMUM_RETRY_TIME_MILLISECONDS = 5 * 60 * 1000; // 5 minutes.
    static final int[] RETRY_TIME_ARRAY_MILLISECONDS = new int[]{1000, 3000, 5000, 25000, 60000, MAXIMUM_RETRY_TIME_MILLISECONDS};

    @NonNull
    private final List<TimestampWrapper<TransferAd>> mNativeAdCache;
    @NonNull private final Handler mReplenishCacheHandler;
    @NonNull private final Runnable mReplenishCacheRunnable;
    @NonNull private final AdRequestListener adRequestListener;

    private boolean mRequestInFlight;
    private boolean mRetryInFlight;
    private int mCurrentRetries;

    @Nullable
    private AdSourceListener mAdSourceListener;

    @Nullable private AdLoader adLoader;

    public View createView(Context context, ViewGroup viewGroup) {
        return adLoader.createView(context, viewGroup);
    }

    /**
     * A listener for when ads are available for dequeueing.
     */
    public interface AdSourceListener {
        /**
         * Called when the number of items available for goes from 0 to more than 0.
         */
        void onAdsAvailable();
    }

    public TransferAdSource() {
        this(new ArrayList<TimestampWrapper<TransferAd>>(CACHE_LIMIT),
                new Handler());
    }

    public TransferAdSource(@NonNull final List<TimestampWrapper<TransferAd>> nativeAdCache,
                   @NonNull final Handler replenishCacheHandler) {
        mNativeAdCache = nativeAdCache;
        mReplenishCacheHandler = replenishCacheHandler;
        mReplenishCacheRunnable = new Runnable() {
            @Override
            public void run() {
                mRetryInFlight = false;
                replenishCache();
            }
        };

        // Construct native URL and start filling the cache
        adRequestListener = new AdRequestListener() {
            @Override
            public void onError() {
                // Reset the retry time for the next time we dequeue.
                mRequestInFlight = false;

                // Stopping requests after the max retry count prevents us from using battery when
                // the user is not interacting with the stream, eg. the app is backgrounded.
                if (mCurrentRetries >= RETRY_TIME_ARRAY_MILLISECONDS.length - 1) {
                    resetRetryTime();
                    return;
                }

                updateRetryTime();
                mRetryInFlight = true;
                mReplenishCacheHandler.postDelayed(mReplenishCacheRunnable, getRetryTime());
            }

            @Override
            public void onAdLoaded(TransferAd transferAd) {
                // This can be null if the ad source was cleared as the AsyncTask is posting
                // back to the UI handler. Drop this response.
                if (adLoader == null) {
                    return;
                }

                mRequestInFlight = false;
                resetRetryTime();

                mNativeAdCache.add(new TimestampWrapper<TransferAd>(transferAd));
                if (mNativeAdCache.size() == 1 && mAdSourceListener != null) {
                    mAdSourceListener.onAdsAvailable();
                }

                replenishCache();
            }
        };

        resetRetryTime();
    }

    /**
     * Sets a adSourceListener for determining when ads are available.
     * @param adSourceListener An AdSourceListener.
     */
    public void setAdSourceListener(@Nullable final AdSourceListener adSourceListener) {
        mAdSourceListener = adSourceListener;
    }

    public void loadAds() {
        loadAds(createAdLoader());
    }

    public abstract AdLoader createAdLoader();

    void loadAds(final AdLoader adLoader) {
        clear();
        adLoader.setAdRequestListener(adRequestListener);

        this.adLoader = adLoader;

        replenishCache();
    }

    /**
     * Clears the ad source, removing any currently queued ads.
     */
    public void clear() {
        // This will cleanup listeners to stop callbacks from handling old ad units
        if (adLoader != null) {
            adLoader.destroy();
            adLoader = null;
        }

        for (final TimestampWrapper<TransferAd> timestampWrapper : mNativeAdCache) {
            timestampWrapper.mInstance.destroy();
        }
        mNativeAdCache.clear();

        mReplenishCacheHandler.removeMessages(0);
        mRequestInFlight = false;
        resetRetryTime();
    }

    /**
     * Removes an ad from the front of the ad source cache.
     *
     * Dequeueing will automatically attempt to replenish the cache. Callers should dequeue ads as
     * late as possible, typically immediately before rendering them into a view.
     *
     * Set the listener to {@code null} to remove the listener.
     *
     * @return Ad ad item that should be rendered into a view.
     */
    @Nullable
    public TransferAd dequeueAd() {
        final long now = SystemClock.uptimeMillis();

        // Starting an ad request takes several millis. Post for performance reasons.
        if (!mRequestInFlight && !mRetryInFlight) {
            mReplenishCacheHandler.post(mReplenishCacheRunnable);
        }

        // Dequeue the first ad that hasn't expired.
        while (!mNativeAdCache.isEmpty()) {
            TimestampWrapper<TransferAd> responseWrapper = mNativeAdCache.remove(0);

            if (now - responseWrapper.mCreatedTimestamp < EXPIRATION_TIME_MILLISECONDS) {
                return responseWrapper.mInstance;
            }
        }
        return null;
    }

    void updateRetryTime() {
        if (mCurrentRetries < RETRY_TIME_ARRAY_MILLISECONDS.length - 1) {
            mCurrentRetries++;
        }
    }

    void resetRetryTime() {
        mCurrentRetries = 0;
    }

    int getRetryTime() {
        if (mCurrentRetries >= RETRY_TIME_ARRAY_MILLISECONDS.length) {
            mCurrentRetries = RETRY_TIME_ARRAY_MILLISECONDS.length - 1;
        }
        return RETRY_TIME_ARRAY_MILLISECONDS[mCurrentRetries];
    }

    /**
     * Replenish ads in the ad source cache.
     *
     * Calling this method is useful for warming the cache without dequeueing an ad.
     */
    void replenishCache() {
        if (!mRequestInFlight && adLoader != null && mNativeAdCache.size() < CACHE_LIMIT) {
            mRequestInFlight = true;
            adLoader.request();
        }
    }

    @NonNull
    @Deprecated
    AdRequestListener getAdRequestListener() {
        return adRequestListener;
    }
}
