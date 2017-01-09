package com.symhung.adcombiner.adframework.base;

import com.mopub.common.Preconditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by HsinHung on 2017/1/9.
 */

public class Position {

    public static final int NO_REPEAT = Integer.MAX_VALUE;

    private final ArrayList<Integer> mFixedPositions = new ArrayList<Integer>();
    private int endPosition = -1;
    private int mRepeatInterval = NO_REPEAT;

    public Position() {
    }

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

    public List<Integer> getFixedPositions() {
        return mFixedPositions;
    }

    public int getEndPosition() {
        return endPosition;
    }

    public Position setEndPosition(int endPosition) {
        this.endPosition = endPosition;
        return this;
    }

    public Position enableRepeatingPositions(final int interval) {
        if (!Preconditions.NoThrow.checkArgument(
                interval > 1, "Repeating interval must be greater than 1")) {
            mRepeatInterval = NO_REPEAT;
            return this;
        }
        mRepeatInterval = interval;
        return this;
    }

    public int getRepeatingInterval() {
        return mRepeatInterval;
    }
}
