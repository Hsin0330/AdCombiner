package com.symhung.adcombiner.adframework;

import android.app.Activity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.symhung.adcombiner.adframework.base.Position;
import com.symhung.adcombiner.adframework.base.TransferAd;
import com.symhung.adcombiner.adframework.base.TransferAdLoadedListener;
import com.symhung.adcombiner.adframework.base.TransferAdSource;
import com.symhung.adcombiner.adframework.base.VisibilityTracker;

import java.util.List;
import java.util.WeakHashMap;

import static com.symhung.adcombiner.adframework.TransferAdRecyclerAdapter.ContentChangeStrategy.INSERT_AT_END;
import static com.symhung.adcombiner.adframework.TransferAdRecyclerAdapter.ContentChangeStrategy.KEEP_ADS_FIXED;

/**
 * Created by HsinHung on 2017/1/9.
 */

public class TransferAdRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = TransferAdRecyclerAdapter.class.getSimpleName();

    // RecyclerView ad views will have negative types to avoid colliding with original view types.
    static final int NATIVE_AD_VIEW_TYPE_BASE = -56;

    enum ContentChangeStrategy {
        INSERT_AT_END, MOVE_ALL_ADS_WITH_CONTENT, KEEP_ADS_FIXED
    }

    private final RecyclerView.AdapterDataObserver adapterDataObserver;
    private RecyclerView recyclerView;
    private final TransferAdPlacer transferAdPlacer;
    private final RecyclerView.Adapter adapter;
    private final VisibilityTracker visibilityTracker;
    private final WeakHashMap<View, Integer> viewPositionMap;

    private ContentChangeStrategy mStrategy = INSERT_AT_END;
    private TransferAdLoadedListener adLoadedListener;

    public TransferAdRecyclerAdapter(Activity activity,
                                     RecyclerView.Adapter originalAdapter,
                                     TransferAdSource transferAdSource,
                                     Position adPositioning) {
        this(new TransferAdPlacer(activity, transferAdSource, adPositioning), originalAdapter,
                new VisibilityTracker(activity));
    }

    private TransferAdRecyclerAdapter(final TransferAdPlacer streamAdPlacer,
                                      final RecyclerView.Adapter originalAdapter,
                                      final VisibilityTracker visibilityTracker) {
        viewPositionMap = new WeakHashMap<>();
        adapter = originalAdapter;
        this.visibilityTracker = visibilityTracker;
        this.visibilityTracker.setVisibilityTrackerListener(new VisibilityTracker.VisibilityTrackerListener() {
            @Override
            public void onVisibilityChanged(final List<View> visibleViews,
                                            final List<View> invisibleViews) {
                handleVisibilityChanged(visibleViews, invisibleViews);
            }
        });



        setHasStableIdsInternal(adapter.hasStableIds());

        transferAdPlacer = streamAdPlacer;
        transferAdPlacer.setAdLoadedListener(new TransferAdLoadedListener() {
            @Override
            public void onAdLoaded(final int position) {
                handleAdLoaded(position);
            }

            @Override
            public void onAdRemoved(final int position) {
                handleAdRemoved(position);
            }
        });
        transferAdPlacer.setItemCount(adapter.getItemCount());

        adapterDataObserver = new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                transferAdPlacer.setItemCount(adapter.getItemCount());
                notifyDataSetChanged();
            }

            @Override
            public void onItemRangeChanged(final int positionStart, final int itemCount) {
                int adjustedEndPosition = transferAdPlacer.getAdjustedPosition(positionStart + itemCount - 1);
                int adjustedStartPosition = transferAdPlacer.getAdjustedPosition(positionStart);
                int adjustedCount = adjustedEndPosition - adjustedStartPosition + 1;
                notifyItemRangeChanged(adjustedStartPosition, adjustedCount);
            }

            @Override
            public void onItemRangeInserted(final int positionStart, final int itemCount) {
                final int adjustedStartPosition = transferAdPlacer.getAdjustedPosition(positionStart);
                final int newOriginalCount = adapter.getItemCount();
                transferAdPlacer.setItemCount(newOriginalCount);
                final boolean addingToEnd = positionStart + itemCount >= newOriginalCount;
                if (KEEP_ADS_FIXED == mStrategy
                        || (INSERT_AT_END == mStrategy
                        && addingToEnd)) {
                    notifyDataSetChanged();
                } else {
                    for (int i = 0; i < itemCount; i++) {
                        // We insert itemCount items at the original position, moving ads downstream.
                        transferAdPlacer.insertItem(positionStart);
                    }
                    notifyItemRangeInserted(adjustedStartPosition, itemCount);
                }
            }

            @Override
            public void onItemRangeRemoved(final int positionStart, final int itemsRemoved) {
                int adjustedStartPosition = transferAdPlacer.getAdjustedPosition(positionStart);
                final int newOriginalCount = adapter.getItemCount();
                transferAdPlacer.setItemCount(newOriginalCount);
                final boolean removingFromEnd = positionStart + itemsRemoved >= newOriginalCount;
                if (KEEP_ADS_FIXED == mStrategy
                        || (INSERT_AT_END == mStrategy
                        && removingFromEnd)) {
                    notifyDataSetChanged();
                } else {
                    final int oldAdjustedCount = transferAdPlacer.getAdjustedCount(newOriginalCount + itemsRemoved);
                    for (int i = 0; i < itemsRemoved; i++) {
                        // We remove itemsRemoved items at the original position.
                        transferAdPlacer.removeItem(positionStart);
                    }

                    final int itemsRemovedIncludingAds = oldAdjustedCount - transferAdPlacer.getAdjustedCount(newOriginalCount);
                    // Need to move the start position back by the # of ads removed.
                    adjustedStartPosition -= itemsRemovedIncludingAds - itemsRemoved;
                    notifyItemRangeRemoved(adjustedStartPosition, itemsRemovedIncludingAds);
                }
            }

            @Override
            public void onItemRangeMoved(final int fromPosition, final int toPosition,
                                         final int itemCount) {
                notifyDataSetChanged();
            }
        };

        adapter.registerAdapterDataObserver(adapterDataObserver);
    }

    @Override
    public void onAttachedToRecyclerView(final RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
    }

    @Override
    public void onDetachedFromRecyclerView(final RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        this.recyclerView = null;
    }

    public void setAdLoadedListener(final TransferAdLoadedListener listener) {
        adLoadedListener = listener;
    }

    public void loadAds(String adUnitId) {
        transferAdPlacer.loadAds(adUnitId);
    }

    private static int computeScrollOffset(final LinearLayoutManager linearLayoutManager,
                                           final RecyclerView.ViewHolder holder) {
        if (holder == null) {
            return 0;
        }
        final View view = holder.itemView;

        int offset = 0;
        if (linearLayoutManager.canScrollVertically()) {
            if (linearLayoutManager.getStackFromEnd()) {
                offset = view.getBottom();
            } else {
                offset = view.getTop();
            }
        } else if (linearLayoutManager.canScrollHorizontally()) {
            if (linearLayoutManager.getStackFromEnd()) {
                offset = view.getRight();
            } else {
                offset = view.getLeft();
            }
        }

        return offset;
    }

    public void refreshAds(String adUnitId) {
        if (recyclerView == null) {
            Log.w(TAG, "This adapter is not attached to a RecyclerView and cannot be refreshed.");
            return;
        }

        final RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        if (layoutManager == null) {
            Log.w(TAG, "Can't refresh ads when there is no layout manager on a RecyclerView.");
            return;
        }

        if (layoutManager instanceof LinearLayoutManager) {
            // Includes GridLayoutManager

            // Get the range & offset of scroll position.
            LinearLayoutManager linearLayoutManager = (LinearLayoutManager) layoutManager;
            final int firstPosition = linearLayoutManager.findFirstVisibleItemPosition();
            RecyclerView.ViewHolder holder = recyclerView.findViewHolderForLayoutPosition(firstPosition);
            final int scrollOffset = computeScrollOffset(linearLayoutManager, holder);

            // Calculate the range of ads not to remove ads from.
            int startOfRange = Math.max(0, firstPosition - 1);
            while (transferAdPlacer.isAd(startOfRange) && startOfRange > 0) {
                startOfRange--;
            }


            final int itemCount = getItemCount();
            int endOfRange = linearLayoutManager.findLastVisibleItemPosition();
            while (transferAdPlacer.isAd(endOfRange) && endOfRange < itemCount - 1) {
                endOfRange++;
            }

            final int originalStartOfRange = transferAdPlacer.getOriginalPosition(startOfRange);
            final int originalEndOfRange = transferAdPlacer.getOriginalPosition(endOfRange);
            final int endCount = adapter.getItemCount();

            transferAdPlacer.removeAdsInRange(originalEndOfRange, endCount);
            final int numAdsRemoved = transferAdPlacer.removeAdsInRange(0, originalStartOfRange);

            if (numAdsRemoved > 0) {
                linearLayoutManager.scrollToPositionWithOffset(firstPosition - numAdsRemoved, scrollOffset);
            }

            loadAds(adUnitId);
        } else {
            Log.w(TAG, "This LayoutManager can't be refreshed.");
            return;
        }
    }

    public void clearAds() {
        transferAdPlacer.clearAds();
    }

    public boolean isAd(final int position) {
        return transferAdPlacer.isAd(position);
    }

    public int getAdjustedPosition(final int originalPosition) {
        return transferAdPlacer.getAdjustedPosition(originalPosition);
    }

    public int getOriginalPosition(final int position) {
        return transferAdPlacer.getOriginalPosition(position);
    }

    public void setContentChangeStrategy(ContentChangeStrategy strategy) {
        mStrategy = strategy;
    }

    @Override
    public int getItemCount() {
        return transferAdPlacer.getAdjustedCount(adapter.getItemCount());
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        if (viewType >= NATIVE_AD_VIEW_TYPE_BASE && viewType <= NATIVE_AD_VIEW_TYPE_BASE + transferAdPlacer.getAdViewTypeCount()) {
            // Create the view and a view holder.
//            final MoPubAdRenderer adRenderer = transferAdPlacer.getAdRendererForViewType(viewType - NATIVE_AD_VIEW_TYPE_BASE);
//            if (adRenderer == null) {
//                Log.w(TAG, "No view binder was registered for ads in MoPubRecyclerAdapter.");
//                // This will cause a null pointer exception.
//                return null;
//            }
            return new TransferAdViewHolder(transferAdPlacer.createAdView(parent.getContext(), parent));
        }

        return adapter.onCreateViewHolder(parent, viewType);
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
        Object adResponse = transferAdPlacer.getAdData(position);
        if (adResponse != null) {
            transferAdPlacer.bindAdView((TransferAd) adResponse, holder.itemView);
            return;
        }

        viewPositionMap.put(holder.itemView, position);
        visibilityTracker.addView(holder.itemView, 0);

        //noinspection unchecked
        adapter.onBindViewHolder(holder, transferAdPlacer.getOriginalPosition(position));
    }

    @Override
    public int getItemViewType(final int position) {
        int type = transferAdPlacer.getAdViewType(position);
        if (type != TransferAdPlacer.CONTENT_VIEW_TYPE) {
            return NATIVE_AD_VIEW_TYPE_BASE + type;
        }

        return adapter.getItemViewType(transferAdPlacer.getOriginalPosition(position));
    }

    @Override
    public void setHasStableIds(final boolean hasStableIds) {
        setHasStableIdsInternal(hasStableIds);

        // We can only setHasStableIds when there are no observers on the adapter.
        adapter.unregisterAdapterDataObserver(adapterDataObserver);
        adapter.setHasStableIds(hasStableIds);
        adapter.registerAdapterDataObserver(adapterDataObserver);
    }

    public void destroy() {
        adapter.unregisterAdapterDataObserver(adapterDataObserver);
        transferAdPlacer.destroy();
        visibilityTracker.destroy();
    }

    @Override
    public long getItemId(final int position) {
        if (!adapter.hasStableIds()) {
            return RecyclerView.NO_ID;
        }

        final Object adData = transferAdPlacer.getAdData(position);
        if (adData != null) {
            return -System.identityHashCode(adData);
        }

        return adapter.getItemId(transferAdPlacer.getOriginalPosition(position));
    }

    // Notification methods to forward to the original adapter.
    @Override
    public boolean onFailedToRecycleView(final RecyclerView.ViewHolder holder) {
        if (holder instanceof TransferAdViewHolder) {
            return super.onFailedToRecycleView(holder);
        }

        // noinspection unchecked
        return adapter.onFailedToRecycleView(holder);
    }

    @Override
    public void onViewAttachedToWindow(final RecyclerView.ViewHolder holder) {
        if (holder instanceof TransferAdViewHolder) {
            super.onViewAttachedToWindow(holder);
            return;
        }

        // noinspection unchecked
        adapter.onViewAttachedToWindow(holder);
    }

    @Override
    public void onViewDetachedFromWindow(final RecyclerView.ViewHolder holder) {
        if (holder instanceof TransferAdViewHolder) {
            super.onViewDetachedFromWindow(holder);
            return;
        }

        // noinspection unchecked
        adapter.onViewDetachedFromWindow(holder);
    }

    @Override
    public void onViewRecycled(final RecyclerView.ViewHolder holder) {
        if (holder instanceof TransferAdViewHolder) {
            super.onViewRecycled(holder);
            return;
        }

        // noinspection unchecked
        adapter.onViewRecycled(holder);
    }
    // End forwarded methods.

    private void handleAdLoaded(final int position) {
        if (adLoadedListener != null) {
            adLoadedListener.onAdLoaded(position);
        }

        notifyItemInserted(position);
    }

    private void handleAdRemoved(final int position) {
        if (adLoadedListener != null) {
            adLoadedListener.onAdRemoved(position);
        }

        notifyItemRemoved(position);
    }

    private void handleVisibilityChanged(final List<View> visibleViews,
                                         final List<View> invisibleViews) {
        // Loop through all visible positions in order to build a max and min range, and then
        // place ads into that range.
        int min = Integer.MAX_VALUE;
        int max = 0;
        for (final View view : visibleViews) {
            final Integer pos = viewPositionMap.get(view);
            if (pos == null) {
                continue;
            }
            min = Math.min(pos, min);
            max = Math.max(pos, max);
        }
        transferAdPlacer.placeAdsInRange(min, max + 1);
    }

    /**
     * Sets the hasStableIds value on this adapter only, not also on the wrapped adapter.
     */
    private void setHasStableIdsInternal(final boolean hasStableIds) {
        super.setHasStableIds(hasStableIds);
    }
}
