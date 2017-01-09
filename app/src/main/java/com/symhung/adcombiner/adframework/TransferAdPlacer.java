package com.symhung.adcombiner.adframework;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;

import com.symhung.adcombiner.adframework.base.Position;
import com.symhung.adcombiner.adframework.base.TransferAd;
import com.symhung.adcombiner.adframework.base.TransferAdLoadedListener;
import com.symhung.adcombiner.adframework.base.TransferAdSource;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.WeakHashMap;

/**
 * Created by HsinHung on 2017/1/9.
 */

public class TransferAdPlacer {

    public static final int CONTENT_VIEW_TYPE = 0;
    private static final int DEFAULT_AD_VIEW_TYPE = 1;
    private final static TransferAdLoadedListener EMPTY_NATIVE_AD_LOADED_LISTENER =
            new TransferAdLoadedListener() {
                @Override
                public void onAdLoaded(final int position) {
                }

                @Override
                public void onAdRemoved(final int position) {
                }
            };

    private final Activity activity;
    private final Handler placementHandler;
    private final Runnable placementRunnable;
    private final Position position;
    private final TransferAdSource adSource;

    private final HashMap<TransferAd, WeakReference<View>> viewMap;
    private final WeakHashMap<View, TransferAd> transferAdMap;

    private boolean hasReceivedPositions;
    private TransferPlacementData pendingPlacementData;
    private boolean hasReceivedAds;
    private boolean hasPlacedAds;
    private TransferPlacementData placementData;

    private TransferAdLoadedListener adLoadedListener = EMPTY_NATIVE_AD_LOADED_LISTENER;

    // The visible range is the range of items which we believe are visible, inclusive.
    // Placing ads near this range makes for a smoother user experience when scrolling up
    // or down.
    private static final int MAX_VISIBLE_RANGE = 100;
    private int visibleRangeStart;
    private int visibleRangeEnd;

    private int itemCount;
    // A buffer around the visible range where we'll place ads if possible.
    private static final int RANGE_BUFFER = 6;
    private boolean needsPlacement;

    TransferAdPlacer(final Activity activity,
                     final TransferAdSource adSource,
                     final Position position) {

        this.activity = activity;
        this.position = position;
        this.adSource = adSource;
        placementData = TransferPlacementData.empty();

        transferAdMap = new WeakHashMap<>();
        viewMap = new HashMap<>();

        placementHandler = new Handler();
        placementRunnable = new Runnable() {
            @Override
            public void run() {
                if (!needsPlacement) {
                    return;
                }
                placeAds();
                needsPlacement = false;
            }
        };

        visibleRangeStart = 0;
        visibleRangeEnd = 0;
    }

    void setAdLoadedListener(final TransferAdLoadedListener listener) {
        adLoadedListener = (listener == null) ? EMPTY_NATIVE_AD_LOADED_LISTENER : listener;
    }

    void loadAds(final String adUnitId) {

//        if (adSource.getAdRendererCount() == 0) {
//            MoPubLog.w("You must register at least 1 ad renderer by calling registerAdRenderer " +
//                    "before loading ads");
//            return;
//        }

        hasPlacedAds = false;
        hasReceivedPositions = false;
        hasReceivedAds = false;

        handlePositioningLoad(position);

        adSource.setAdSourceListener(new TransferAdSource.AdSourceListener() {
            @Override
            public void onAdsAvailable() {
                handleAdsAvailable();
            }
        });

        adSource.loadAds();
    }

    private void handlePositioningLoad(final Position positioning) {
        TransferPlacementData placementData = TransferPlacementData.fromAdPositioning(positioning);
        if (hasReceivedAds) {
            placeInitialAds(placementData);
        } else {
            pendingPlacementData = placementData;
        }
        hasReceivedPositions = true;
    }

    private void handleAdsAvailable() {
        // If we've already placed ads, just notify that we need placement.
        if (hasPlacedAds) {
            notifyNeedsPlacement();
            return;
        }

        // Otherwise, we may need to place initial ads.
        if (hasReceivedPositions) {
            placeInitialAds(pendingPlacementData);
        }
        hasReceivedAds = true;
    }

    private void placeInitialAds(TransferPlacementData placementData) {
        // Remove ads that may be present and immediately place ads again. This prevents the UI
        // from flashing grossly.
        removeAdsInRange(0, itemCount);

        this.placementData = placementData;
        placeAds();
        hasPlacedAds = true;
    }

    void placeAdsInRange(final int startPosition, final int endPosition) {
        visibleRangeStart = startPosition;
        visibleRangeEnd = Math.min(endPosition, startPosition + MAX_VISIBLE_RANGE);
        notifyNeedsPlacement();
    }

    boolean isAd(final int position) {
        return placementData.isPlacedAd(position);
    }

    void clearAds() {
        removeAdsInRange(0, itemCount);
        adSource.clear();
    }

    public void destroy() {
        placementHandler.removeMessages(0);
        adSource.clear();
        placementData.clearAds();
    }

    Object getAdData(final int position) {
        return placementData.getPlacedAd(position);
    }

    public View getAdView(final int position, final View convertView,
                          final ViewGroup parent) {
        final TransferAd transferAd = placementData.getPlacedAd(position);
        if (transferAd == null) {
            return null;
        }

        final View view = (convertView != null) ?
                convertView : transferAd.createView(activity, parent);
        bindAdView(transferAd, view);
        return view;
    }

    View createAdView(Context context, ViewGroup viewGroup) {
        return adSource.createView(context, viewGroup);
    }

    void bindAdView(TransferAd transferAd, View adView) {
        WeakReference<View> mappedViewRef = viewMap.get(transferAd);
        View mappedView = null;
        if (mappedViewRef != null) {
            mappedView = mappedViewRef.get();
        }
        if (!adView.equals(mappedView)) {
            clearNativeAd(mappedView);
            clearNativeAd(adView);
            prepareNativeAd(transferAd, adView);
            transferAd.renderView(adView);
        }
    }

    int removeAdsInRange(int originalStartPosition, int originalEndPosition) {
        int[] positions = placementData.getPlacedAdPositions();

        int adjustedStartRange = placementData.getAdjustedPosition(originalStartPosition);
        int adjustedEndRange = placementData.getAdjustedPosition(originalEndPosition);

        ArrayList<Integer> removedPositions = new ArrayList<>();
        // Traverse in reverse order to make this less error-prone for developers who are removing
        // views directly from their UI.
        for (int i = positions.length - 1; i >= 0; --i) {
            int position = positions[i];
            if (position < adjustedStartRange || position >= adjustedEndRange) {
                continue;
            }

            removedPositions.add(position);

            // Decrement the start range for any removed ads. We don't bother to decrement the end
            // range, as it is OK if it isn't 100% accurate.
            if (position < visibleRangeStart) {
                visibleRangeStart--;
            }
            itemCount--;
        }

        int clearedAdsCount = placementData.clearAdsInRange(adjustedStartRange, adjustedEndRange);
        for (int position : removedPositions) {
            adLoadedListener.onAdRemoved(position);
        }
        return clearedAdsCount;
    }

    int getAdViewTypeCount() {
//        return adSource.getAdRendererCount();
        return 1;
    }

    int getAdViewType(final int position) {
        TransferAd transferAd = placementData.getPlacedAd(position);
        if (transferAd == null) {
            return CONTENT_VIEW_TYPE;
        }

//        return adSource.getViewTypeForAd(transferAd);
        return DEFAULT_AD_VIEW_TYPE;
    }

    int getOriginalPosition(final int position) {
        return placementData.getOriginalPosition(position);
    }

    int getAdjustedPosition(final int originalPosition) {
        return placementData.getAdjustedPosition(originalPosition);
    }

    public int getOriginalCount(final int count) {
        return placementData.getOriginalCount(count);
    }

    int getAdjustedCount(final int originalCount) {
        return placementData.getAdjustedCount(originalCount);
    }

    void setItemCount(final int originalCount) {
        itemCount = placementData.getAdjustedCount(originalCount);

        // If we haven't already placed ads, we'll let ads get placed by the normal loadAds call
        if (hasPlacedAds) {
            notifyNeedsPlacement();
        }
    }

    void insertItem(final int originalPosition) {
        placementData.insertItem(originalPosition);
    }

    void removeItem(final int originalPosition) {
        placementData.removeItem(originalPosition);
    }

    public void moveItem(final int originalPosition, final int newPosition) {
        placementData.moveItem(originalPosition, newPosition);
    }

    private void notifyNeedsPlacement() {
        // Avoid posting if this method has already been called.
        if (needsPlacement) {
            return;
        }
        needsPlacement = true;

        // Post the placement to happen on the next UI render loop.
        placementHandler.post(placementRunnable);
    }

    private void placeAds() {
        // Place ads within the visible range
        if (!tryPlaceAdsInRange(visibleRangeStart, visibleRangeEnd)) {
            return;
        }

        // Place ads after the visible range so that user will see an ad if they scroll down. We
        // don't place an ad before the visible range, because we are trying to be mindful of
        // changes that will affect scrolling.
        tryPlaceAdsInRange(visibleRangeEnd, visibleRangeEnd + RANGE_BUFFER);
    }

    private boolean tryPlaceAdsInRange(final int start, final int end) {
        int position = start;
        int lastPosition = end - 1;
        while (position <= lastPosition && position != TransferPlacementData.NOT_FOUND) {
            if (position >= itemCount) {
                break;
            }
            if (placementData.shouldPlaceAd(position)) {
                if (!tryPlaceAd(position)) {
                    return false;
                }
                lastPosition++;
            }
            position = placementData.nextInsertionPosition(position);
        }
        return true;
    }

    private boolean tryPlaceAd(final int position) {
        final TransferAd transferAd = adSource.dequeueAd();
        if (transferAd == null) {
            return false;
        }

        placementData.placeAd(position, transferAd);
        itemCount++;

        adLoadedListener.onAdLoaded(position);
        return true;
    }

    private void clearNativeAd(final View view) {
        if (view == null) {
            return;
        }
        final TransferAd transferAd = transferAdMap.get(view);
        if (transferAd != null) {
            transferAd.clear(view);
            transferAdMap.remove(view);
            viewMap.remove(transferAd);
        }
    }

    private void prepareNativeAd(final TransferAd transferAd, final View view) {
        viewMap.put(transferAd, new WeakReference<>(view));
        transferAdMap.put(view, transferAd);
//        transferAd.prepare(view);
    }
}
