package com.symhung.adcombiner.adframework.base;

import android.support.annotation.NonNull;

import com.mopub.common.Preconditions;
import com.mopub.nativeads.MoPubNativeAdPositioning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by HsinHung on 2017/1/9.
 */

public class Position {
    /**
     * Constant indicating that ad positions should not repeat.
     */
    public static final int NO_REPEAT = Integer.MAX_VALUE;

    @NonNull private final ArrayList<Integer> mFixedPositions = new ArrayList<Integer>();
    private int mRepeatInterval = NO_REPEAT;

    public Position() {
    }

    /**
     * Specifies a fixed ad position.
     *
     * @param position The ad position.
     * @return This object for easy use in chained setters.
     */
    @NonNull
    public Position addFixedPosition(final int position) {
        if (!Preconditions.NoThrow.checkArgument(position >= 0)) {
            return this;
        }

        // Add in sorted order if this does not exist.
        int index = Collections.binarySearch(mFixedPositions, position);
        if (index < 0) {
            mFixedPositions.add(~index, position);
        }
        return this;
    }

    /**
     * Returns an ordered array of fixed ad positions.
     *
     * @return Fixed ad positions.
     */
    @NonNull
    public List<Integer> getFixedPositions() {
        return mFixedPositions;
    }

    /**
     * Enables showing ads ad at a repeated interval.
     *
     * @param interval The frequency at which to show ads. Must be an integer greater than 1 or
     * the constant NO_REPEAT.
     * @return This object for easy use in chained setters.
     */
    @NonNull
    public Position enableRepeatingPositions(final int interval) {
        if (!Preconditions.NoThrow.checkArgument(
                interval > 1, "Repeating interval must be greater than 1")) {
            mRepeatInterval = NO_REPEAT;
            return this;
        }
        mRepeatInterval = interval;
        return this;
    }

    /**
     * Returns the repeating ad interval.
     *
     * Repeating ads start after the last fixed position. Returns {@link #NO_REPEAT} if there is
     * no repeating interval.
     *
     * @return The repeating ad interval.
     */
    public int getRepeatingInterval() {
        return mRepeatInterval;
    }
}
