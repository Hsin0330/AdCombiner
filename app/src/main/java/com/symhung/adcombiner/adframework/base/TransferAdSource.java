package com.symhung.adcombiner.adframework.base;

import android.content.Context;
import android.os.Handler;
import android.os.SystemClock;
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

    private final List<TimestampWrapper<TransferAd>> mNativeAdCache;
    private final Handler mReplenishCacheHandler;
    private final Runnable mReplenishCacheRunnable;
    private final AdRequestListener adRequestListener;

    private boolean mRequestInFlight;
    private boolean mRetryInFlight;
    private int mCurrentRetries;

    private AdSourceListener mAdSourceListener;

    private AdLoader adLoader;

    public View createView(Context context, ViewGroup viewGroup) {
        return adLoader.createView(context, viewGroup);
    }

    public interface AdSourceListener {
        void onAdsAvailable();
    }

    public TransferAdSource() {
        this(new ArrayList<TimestampWrapper<TransferAd>>(CACHE_LIMIT),
                new Handler());
    }

    public TransferAdSource(final List<TimestampWrapper<TransferAd>> nativeAdCache,
                   final Handler replenishCacheHandler) {
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

    public void setAdSourceListener(final AdSourceListener adSourceListener) {
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

    public void clear() {
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

    private void updateRetryTime() {
        if (mCurrentRetries < RETRY_TIME_ARRAY_MILLISECONDS.length - 1) {
            mCurrentRetries++;
        }
    }

    private void resetRetryTime() {
        mCurrentRetries = 0;
    }

    private int getRetryTime() {
        if (mCurrentRetries >= RETRY_TIME_ARRAY_MILLISECONDS.length) {
            mCurrentRetries = RETRY_TIME_ARRAY_MILLISECONDS.length - 1;
        }
        return RETRY_TIME_ARRAY_MILLISECONDS[mCurrentRetries];
    }

    private void replenishCache() {
        if (!mRequestInFlight && adLoader != null && mNativeAdCache.size() < CACHE_LIMIT) {
            mRequestInFlight = true;
            adLoader.request();
        }
    }
}
