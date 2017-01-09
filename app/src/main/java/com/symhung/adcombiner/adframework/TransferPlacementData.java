package com.symhung.adcombiner.adframework;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.mopub.nativeads.MoPubNativeAdPositioning;
import com.mopub.nativeads.NativeAd;
import com.symhung.adcombiner.adframework.base.Position;
import com.symhung.adcombiner.adframework.base.TransferAd;

import java.util.List;

/**
 * Created by HsinHung on 2017/1/9.
 */

class TransferPlacementData {

    private static final String TAG = TransferPlacementData.class.getSimpleName();
    /**
     * Returned when positions are not found.
     */
    public final static int NOT_FOUND = -1;

    // Cap the number of ads to avoid unrestrained memory usage. 200 allows the 5 positioning
    // arrays to fit in less than 4K.
    private final static int MAX_ADS = 200;

    // Initialize all of these to their max capacity. This prevents garbage collection when
    // reallocating the list, which causes noticeable stuttering when scrolling on some devices.
    @NonNull
    private final int[] mDesiredOriginalPositions = new int[MAX_ADS];
    @NonNull private final int[] mDesiredInsertionPositions = new int[MAX_ADS];
    private int mDesiredCount = 0;
    @NonNull private final int[] mOriginalAdPositions = new int[MAX_ADS];
    @NonNull private final int[] mAdjustedAdPositions = new int[MAX_ADS];
    @NonNull private final TransferAd[] mNativeAds = new TransferAd[MAX_ADS];
    private int mPlacedCount = 0;

    /**
     * @param desiredInsertionPositions Insertion positions, expressed as original positions
     */
    private TransferPlacementData(@NonNull final int[] desiredInsertionPositions) {
        mDesiredCount = Math.min(desiredInsertionPositions.length, MAX_ADS);
        System.arraycopy(desiredInsertionPositions, 0, mDesiredInsertionPositions, 0, mDesiredCount);
        System.arraycopy(desiredInsertionPositions, 0, mDesiredOriginalPositions, 0, mDesiredCount);
    }

    @NonNull
    static TransferPlacementData fromAdPositioning(@NonNull final Position adPositioning) {
        final List<Integer> fixed = adPositioning.getFixedPositions();
        final int interval = adPositioning.getRepeatingInterval();

        final int size = (interval == MoPubNativeAdPositioning.MoPubClientPositioning.NO_REPEAT ? fixed.size() : MAX_ADS);
        final int[] desiredInsertionPositions = new int[size];

        // Fixed positions are in terms of final positions. Calculate current insertion positions
        // by decrementing numAds at each index.
        int numAds = 0;
        int lastPos = 0;
        for (final Integer position : fixed) {
            lastPos = position - numAds;
            desiredInsertionPositions[numAds++] = lastPos;
        }

        // Expand the repeating positions, if there are any
        while (numAds < size) {
            lastPos = lastPos + interval - 1;
            desiredInsertionPositions[numAds++] = lastPos;
        }
        return new TransferPlacementData(desiredInsertionPositions);
    }

    @NonNull
    static TransferPlacementData empty() {
        return new TransferPlacementData(new int[] {});
    }

    /**
     * Whether the given position should be an ad.
     */
    boolean shouldPlaceAd(final int position) {
        final int index = binarySearch(mDesiredInsertionPositions, 0, mDesiredCount, position);
        return index >= 0;
    }

    /**
     * The next position after this position that should be an ad. Returns NOT_FOUND if there are no
     * more ads.
     */
    int nextInsertionPosition(final int position) {
        final int index = binarySearchGreaterThan(
                mDesiredInsertionPositions, mDesiredCount, position);
        if (index == mDesiredCount) {
            return NOT_FOUND;
        }
        return mDesiredInsertionPositions[index];
    }

    /**
     * The next position after this position that should be an ad. Returns NOT_FOUND if there
     * are no more ads.
     */
    int previousInsertionPosition(final int position) {
        final int index = binarySearchFirstEquals(
                mDesiredInsertionPositions,  mDesiredCount, position);
        if (index == 0) {
            return NOT_FOUND;
        }
        return mDesiredInsertionPositions[index - 1];
    }

    /**
     * Sets ad data at the given position.
     */
    void placeAd(final int adjustedPosition, final TransferAd nativeAd) {
        // See if this is a insertion ad
        final int desiredIndex = binarySearchFirstEquals(
                mDesiredInsertionPositions, mDesiredCount, adjustedPosition);
        if (desiredIndex == mDesiredCount
                || mDesiredInsertionPositions[desiredIndex] != adjustedPosition) {
            Log.w(TAG, "Attempted to insert an ad at an invalid position");
            return;
        }

        // Add to placed array
        final int originalPosition = mDesiredOriginalPositions[desiredIndex];
        int placeIndex = binarySearchGreaterThan(
                mOriginalAdPositions, mPlacedCount, originalPosition);
        if (placeIndex < mPlacedCount) {
            final int num = mPlacedCount - placeIndex;
            System.arraycopy(mOriginalAdPositions, placeIndex,
                    mOriginalAdPositions, placeIndex + 1, num);
            System.arraycopy(mAdjustedAdPositions, placeIndex,
                    mAdjustedAdPositions, placeIndex + 1, num);
            System.arraycopy(mNativeAds, placeIndex, mNativeAds, placeIndex + 1, num);
        }
        mOriginalAdPositions[placeIndex] = originalPosition;
        mAdjustedAdPositions[placeIndex] = adjustedPosition;
        mNativeAds[placeIndex] = nativeAd;
        mPlacedCount++;

        // Remove desired index
        final int num = mDesiredCount - desiredIndex - 1;
        System.arraycopy(mDesiredInsertionPositions, desiredIndex + 1,
                mDesiredInsertionPositions, desiredIndex, num);
        System.arraycopy(mDesiredOriginalPositions, desiredIndex + 1,
                mDesiredOriginalPositions, desiredIndex, num);
        mDesiredCount--;

        // Increment adjusted positions
        for (int i = desiredIndex; i < mDesiredCount; ++i) {
            mDesiredInsertionPositions[i]++;
        }
        for (int i = placeIndex + 1; i < mPlacedCount; ++i) {
            mAdjustedAdPositions[i]++;
        }
    }

    /**
     * @see {@link com.mopub.nativeads.MoPubStreamAdPlacer#isAd(int)}
     */
    boolean isPlacedAd(final int position) {
        final int index = binarySearch(mAdjustedAdPositions, 0, mPlacedCount, position);
        return index >= 0;
    }

    /**
     * Returns the ad data associated with the given ad position, or {@code null} if there is
     * no ad at this position.
     */
    @Nullable
    TransferAd getPlacedAd(final int position) {
        final int index = binarySearch(mAdjustedAdPositions, 0, mPlacedCount, position);
        if (index < 0) {
            return null;
        }
        return mNativeAds[index];
    }

    /**
     * Returns all placed ad positions. This method allocates new memory on every invocation. Do
     * not call it from performance critical code.
     */
    @NonNull
    int[] getPlacedAdPositions() {
        int[] positions = new int[mPlacedCount];
        System.arraycopy(mAdjustedAdPositions, 0, positions, 0, mPlacedCount);
        return positions;
    }

    /**
     * @see com.mopub.nativeads.MoPubStreamAdPlacer#getOriginalPosition(int)
     */
    int getOriginalPosition(final int position) {
        final int index = binarySearch(mAdjustedAdPositions, 0, mPlacedCount, position);

        // No match, ~index is the number of ads before this pos.
        if (index < 0) {
            return position - ~index;
        }

        // This is an ad - there is no original position
        return NOT_FOUND;
    }

    /**
     * @see com.mopub.nativeads.MoPubStreamAdPlacer#getAdjustedPosition(int)
     */
    int getAdjustedPosition(final int originalPosition) {
        // This is an ad. Since binary search doesn't properly handle dups, find the first non-ad.
        int index = binarySearchGreaterThan(mOriginalAdPositions, mPlacedCount, originalPosition);
        return originalPosition + index;
    }

    /**
     * @see com.mopub.nativeads.MoPubStreamAdPlacer#getOriginalCount(int)
     */
    int getOriginalCount(final int count) {
        if (count == 0) {
            return 0;
        }

        // The last item will never be an ad
        final int originalPos = getOriginalPosition(count - 1);
        return (originalPos == NOT_FOUND) ? NOT_FOUND : originalPos + 1;
    }

    /**
     * @see com.mopub.nativeads.MoPubStreamAdPlacer#getAdjustedCount(int)
     */
    int getAdjustedCount(final int originalCount) {
        if (originalCount == 0) {
            return 0;
        }
        return getAdjustedPosition(originalCount - 1) + 1;
    }

    /**
     * Clears the ads in the given range. After calling this method, the ad positions
     * will be removed from the placed ad positions and put back into the desired ad insertion
     * positions.
     */
    int clearAdsInRange(final int adjustedStartRange, final int adjustedEndRange) {
        // Temporary arrays to store the cleared positions. Using temporary arrays makes it
        // easy to debug what positions are being cleared.
        int[] clearOriginalPositions = new int[mPlacedCount];
        int[] clearAdjustedPositions = new int[mPlacedCount];
        int clearCount = 0;

        // Add to the clear position arrays any positions that fall inside
        // [adjustedRangeStart, adjustedRangeEnd).
        for (int i = 0; i < mPlacedCount; ++i) {
            int originalPosition = mOriginalAdPositions[i];
            int adjustedPosition = mAdjustedAdPositions[i];
            if (adjustedStartRange <= adjustedPosition && adjustedPosition < adjustedEndRange) {
                // When copying adjusted positions, subtract the current clear count because there
                // is no longer an ad incrementing the desired insertion position.
                clearOriginalPositions[clearCount] = originalPosition;
                clearAdjustedPositions[clearCount] = adjustedPosition - clearCount;

                // Destroying and nulling out the ad objects to avoids a memory leak.
                mNativeAds[i].destroy();
                mNativeAds[i] = null;
                clearCount++;
            } else if (clearCount > 0) {
                // The position is not in the range; shift it by the number of cleared ads.
                int newIndex = i - clearCount;
                mOriginalAdPositions[newIndex] = originalPosition;
                mAdjustedAdPositions[newIndex] = adjustedPosition - clearCount;
                mNativeAds[newIndex] = mNativeAds[i];
            }
        }

        // If we have cleared nothing, this method was a no-op.
        if (clearCount == 0) {
            return 0;
        }

        // Modify the desired positions arrays in order to make space to put back the
        // cleared ad positions. For example if the desired array was {1, 10,
        // 15} and we need to insert {3, 7} we'll shift the desired array to be {1, ?, ? , 10, 15}.
        int firstCleared = clearAdjustedPositions[0];
        int desiredIndex = binarySearchFirstEquals(
                mDesiredInsertionPositions, mDesiredCount, firstCleared);
        for (int i = mDesiredCount - 1; i >= desiredIndex; --i) {
            mDesiredOriginalPositions[i + clearCount] = mDesiredOriginalPositions[i];
            mDesiredInsertionPositions[i + clearCount] = mDesiredInsertionPositions[i] - clearCount;
        }

        // Copy the cleared ad positions into the desired arrays.
        for (int i = 0; i < clearCount; ++i) {
            mDesiredOriginalPositions[desiredIndex + i] = clearOriginalPositions[i];
            mDesiredInsertionPositions[desiredIndex + i] = clearAdjustedPositions[i];
        }

        // Update the array counts, and we're done.
        mDesiredCount = mDesiredCount + clearCount;
        mPlacedCount = mPlacedCount - clearCount;
        return clearCount;
    }

    /**
     * Clears the ads in the given range. After calling this method the ad's position
     * will be back to the desired insertion positions.
     */
    void clearAds() {
        if (mPlacedCount == 0) {
            return;
        }

        clearAdsInRange(0, mAdjustedAdPositions[mPlacedCount - 1] + 1);
    }

    /**
     * @see com.mopub.nativeads.MoPubStreamAdPlacer#insertItem(int)
     */
    void insertItem(final int originalPosition) {

        // Increment desired arrays.
        int indexToIncrement = binarySearchFirstEquals(
                mDesiredOriginalPositions, mDesiredCount, originalPosition);
        for (int i = indexToIncrement; i < mDesiredCount; ++i) {
            mDesiredOriginalPositions[i]++;
            mDesiredInsertionPositions[i]++;
        }

        // Increment placed arrays.
        indexToIncrement = binarySearchFirstEquals(
                mOriginalAdPositions, mPlacedCount, originalPosition);
        for (int i = indexToIncrement; i < mPlacedCount; ++i) {
            mOriginalAdPositions[i]++;
            mAdjustedAdPositions[i]++;
        }
    }

    /**
     * @see com.mopub.nativeads.MoPubStreamAdPlacer#removeItem(int)
     */
    void removeItem(final int originalPosition) {
        // When removing items, we only decrement ad position values *greater* than the original
        // position we're removing. The original position associated with an ad is the original
        // position of the first content item after the ad, so we shouldn't change the original
        // position of an ad that matches the original position removed.
        int indexToDecrement = binarySearchGreaterThan(
                mDesiredOriginalPositions, mDesiredCount, originalPosition);

        // Decrement desired arrays.
        for (int i = indexToDecrement; i < mDesiredCount; ++i) {
            mDesiredOriginalPositions[i]--;
            mDesiredInsertionPositions[i]--;
        }

        indexToDecrement = binarySearchGreaterThan(
                mOriginalAdPositions, mPlacedCount, originalPosition);

        for (int i = indexToDecrement; i < mPlacedCount; ++i) {
            mOriginalAdPositions[i]--;
            mAdjustedAdPositions[i]--;
        }
    }

    /**
     * @see com.mopub.nativeads.MoPubStreamAdPlacer#moveItem(int, int)
     */
    void moveItem(final int originalPosition, final int newPosition) {
        removeItem(originalPosition);
        insertItem(newPosition);
    }

    private static int binarySearchFirstEquals(int[] array, int count, int value) {
        int index = binarySearch(array, 0, count, value);

        // If not found, binarySearch returns the 2's complement of the index of the nearest
        // value higher than the target value, which is also the insertion index.
        if (index < 0) {
            return ~index;
        }

        int duplicateValue = array[index];
        while (index >= 0 && array[index] == duplicateValue) {
            index--;
        }

        return index + 1;
    }

    private static int binarySearchGreaterThan(int[] array, int count, int value) {
        int index = binarySearch(array, 0, count, value);

        // If not found, binarySearch returns the 2's complement of the index of the nearest
        // value higher than the target value, which is also the insertion index.
        if (index < 0) {
            return ~index;
        }

        int duplicateValue = array[index];
        while (index < count && array[index] == duplicateValue) {
            index++;
        }

        return index;
    }

    /**
     * Copied from Arrays.java, which isn't available until Gingerbread.
     */
    private static int binarySearch(int[] array, int startIndex, int endIndex, int value) {
        int lo = startIndex;
        int hi = endIndex - 1;

        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            int midVal = array[mid];

            if (midVal < value) {
                lo = mid + 1;
            } else if (midVal > value) {
                hi = mid - 1;
            } else {
                return mid;  // value found
            }
        }
        return ~lo;  // value not present
    }
}
