package com.symhung.adcombiner.adframework;

import android.util.Log;

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
    private final int[] desiredOriginalPositions = new int[MAX_ADS];
    private final int[] desiredInsertionPositions = new int[MAX_ADS];
    private int desiredCount = 0;
    private final int[] driginalAdPositions = new int[MAX_ADS];
    private final int[] adjustedAdPositions = new int[MAX_ADS];
    private final TransferAd[] transferAds = new TransferAd[MAX_ADS];
    private int placedCount = 0;

    /**
     * @param desiredInsertionPositions Insertion positions, expressed as original positions
     */
    private TransferPlacementData(final int[] desiredInsertionPositions) {
        desiredCount = Math.min(desiredInsertionPositions.length, MAX_ADS);
        System.arraycopy(desiredInsertionPositions, 0, this.desiredInsertionPositions, 0, desiredCount);
        System.arraycopy(desiredInsertionPositions, 0, desiredOriginalPositions, 0, desiredCount);
    }

    static TransferPlacementData fromAdPositioning(final Position adPositioning) {
        final List<Integer> fixed = adPositioning.getFixedPositions();
        final int end = adPositioning.getEndPosition();
        final int interval = adPositioning.getRepeatingInterval();

//        final int size = (interval == MoPubNativeAdPositioning.MoPubClientPositioning.NO_REPEAT ? fixed.size() : MAX_ADS);
        final int size = end != -1 ? fixed.size() + (end - fixed.get(fixed.size() - 1)) / interval : MAX_ADS;
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

    static TransferPlacementData empty() {
        return new TransferPlacementData(new int[] {});
    }

    /**
     * Whether the given position should be an ad.
     */
    boolean shouldPlaceAd(final int position) {
        final int index = binarySearch(desiredInsertionPositions, 0, desiredCount, position);
        return index >= 0;
    }

    /**
     * The next position after this position that should be an ad. Returns NOT_FOUND if there are no
     * more ads.
     */
    int nextInsertionPosition(final int position) {
        final int index = binarySearchGreaterThan(
                desiredInsertionPositions, desiredCount, position);
        if (index == desiredCount) {
            return NOT_FOUND;
        }
        return desiredInsertionPositions[index];
    }

    /**
     * The next position after this position that should be an ad. Returns NOT_FOUND if there
     * are no more ads.
     */
    int previousInsertionPosition(final int position) {
        final int index = binarySearchFirstEquals(
                desiredInsertionPositions, desiredCount, position);
        if (index == 0) {
            return NOT_FOUND;
        }
        return desiredInsertionPositions[index - 1];
    }

    /**
     * Sets ad data at the given position.
     */
    void placeAd(final int adjustedPosition, final TransferAd nativeAd) {
        // See if this is a insertion ad
        final int desiredIndex = binarySearchFirstEquals(
                desiredInsertionPositions, desiredCount, adjustedPosition);
        if (desiredIndex == desiredCount
                || desiredInsertionPositions[desiredIndex] != adjustedPosition) {
            Log.w(TAG, "Attempted to insert an ad at an invalid position");
            return;
        }

        // Add to placed array
        final int originalPosition = desiredOriginalPositions[desiredIndex];
        int placeIndex = binarySearchGreaterThan(
                driginalAdPositions, placedCount, originalPosition);
        if (placeIndex < placedCount) {
            final int num = placedCount - placeIndex;
            System.arraycopy(driginalAdPositions, placeIndex,
                    driginalAdPositions, placeIndex + 1, num);
            System.arraycopy(adjustedAdPositions, placeIndex,
                    adjustedAdPositions, placeIndex + 1, num);
            System.arraycopy(transferAds, placeIndex, transferAds, placeIndex + 1, num);
        }
        driginalAdPositions[placeIndex] = originalPosition;
        adjustedAdPositions[placeIndex] = adjustedPosition;
        transferAds[placeIndex] = nativeAd;
        placedCount++;

        // Remove desired index
        final int num = desiredCount - desiredIndex - 1;
        System.arraycopy(desiredInsertionPositions, desiredIndex + 1,
                desiredInsertionPositions, desiredIndex, num);
        System.arraycopy(desiredOriginalPositions, desiredIndex + 1,
                desiredOriginalPositions, desiredIndex, num);
        desiredCount--;

        // Increment adjusted positions
        for (int i = desiredIndex; i < desiredCount; ++i) {
            desiredInsertionPositions[i]++;
        }
        for (int i = placeIndex + 1; i < placedCount; ++i) {
            adjustedAdPositions[i]++;
        }
    }

    /**
     * @see {@link com.mopub.nativeads.MoPubStreamAdPlacer#isAd(int)}
     */
    boolean isPlacedAd(final int position) {
        final int index = binarySearch(adjustedAdPositions, 0, placedCount, position);
        return index >= 0;
    }

    /**
     * Returns the ad data associated with the given ad position, or {@code null} if there is
     * no ad at this position.
     */
    TransferAd getPlacedAd(final int position) {
        final int index = binarySearch(adjustedAdPositions, 0, placedCount, position);
        if (index < 0) {
            return null;
        }
        return transferAds[index];
    }

    /**
     * Returns all placed ad positions. This method allocates new memory on every invocation. Do
     * not call it from performance critical code.
     */
    int[] getPlacedAdPositions() {
        int[] positions = new int[placedCount];
        System.arraycopy(adjustedAdPositions, 0, positions, 0, placedCount);
        return positions;
    }

    /**
     * @see com.mopub.nativeads.MoPubStreamAdPlacer#getOriginalPosition(int)
     */
    int getOriginalPosition(final int position) {
        final int index = binarySearch(adjustedAdPositions, 0, placedCount, position);

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
        int index = binarySearchGreaterThan(driginalAdPositions, placedCount, originalPosition);
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
        int[] clearOriginalPositions = new int[placedCount];
        int[] clearAdjustedPositions = new int[placedCount];
        int clearCount = 0;

        // Add to the clear position arrays any positions that fall inside
        // [adjustedRangeStart, adjustedRangeEnd).
        for (int i = 0; i < placedCount; ++i) {
            int originalPosition = driginalAdPositions[i];
            int adjustedPosition = adjustedAdPositions[i];
            if (adjustedStartRange <= adjustedPosition && adjustedPosition < adjustedEndRange) {
                // When copying adjusted positions, subtract the current clear count because there
                // is no longer an ad incrementing the desired insertion position.
                clearOriginalPositions[clearCount] = originalPosition;
                clearAdjustedPositions[clearCount] = adjustedPosition - clearCount;

                // Destroying and nulling out the ad objects to avoids a memory leak.
                transferAds[i].destroy();
                transferAds[i] = null;
                clearCount++;
            } else if (clearCount > 0) {
                // The position is not in the range; shift it by the number of cleared ads.
                int newIndex = i - clearCount;
                driginalAdPositions[newIndex] = originalPosition;
                adjustedAdPositions[newIndex] = adjustedPosition - clearCount;
                transferAds[newIndex] = transferAds[i];
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
                desiredInsertionPositions, desiredCount, firstCleared);
        for (int i = desiredCount - 1; i >= desiredIndex; --i) {
            desiredOriginalPositions[i + clearCount] = desiredOriginalPositions[i];
            desiredInsertionPositions[i + clearCount] = desiredInsertionPositions[i] - clearCount;
        }

        // Copy the cleared ad positions into the desired arrays.
        for (int i = 0; i < clearCount; ++i) {
            desiredOriginalPositions[desiredIndex + i] = clearOriginalPositions[i];
            desiredInsertionPositions[desiredIndex + i] = clearAdjustedPositions[i];
        }

        // Update the array counts, and we're done.
        desiredCount = desiredCount + clearCount;
        placedCount = placedCount - clearCount;
        return clearCount;
    }

    /**
     * Clears the ads in the given range. After calling this method the ad's position
     * will be back to the desired insertion positions.
     */
    void clearAds() {
        if (placedCount == 0) {
            return;
        }

        clearAdsInRange(0, adjustedAdPositions[placedCount - 1] + 1);
    }

    /**
     * @see com.mopub.nativeads.MoPubStreamAdPlacer#insertItem(int)
     */
    void insertItem(final int originalPosition) {

        // Increment desired arrays.
        int indexToIncrement = binarySearchFirstEquals(
                desiredOriginalPositions, desiredCount, originalPosition);
        for (int i = indexToIncrement; i < desiredCount; ++i) {
            desiredOriginalPositions[i]++;
            desiredInsertionPositions[i]++;
        }

        // Increment placed arrays.
        indexToIncrement = binarySearchFirstEquals(
                driginalAdPositions, placedCount, originalPosition);
        for (int i = indexToIncrement; i < placedCount; ++i) {
            driginalAdPositions[i]++;
            adjustedAdPositions[i]++;
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
                desiredOriginalPositions, desiredCount, originalPosition);

        // Decrement desired arrays.
        for (int i = indexToDecrement; i < desiredCount; ++i) {
            desiredOriginalPositions[i]--;
            desiredInsertionPositions[i]--;
        }

        indexToDecrement = binarySearchGreaterThan(
                driginalAdPositions, placedCount, originalPosition);

        for (int i = indexToDecrement; i < placedCount; ++i) {
            driginalAdPositions[i]--;
            adjustedAdPositions[i]--;
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
