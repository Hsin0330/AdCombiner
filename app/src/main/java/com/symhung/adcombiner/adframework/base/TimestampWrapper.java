package com.symhung.adcombiner.adframework.base;

import android.os.SystemClock;

/**
 * Created by HsinHung on 2017/1/9.
 */

class TimestampWrapper<T> {
    final T mInstance;
    long mCreatedTimestamp;

    TimestampWrapper(final T instance) {
        mInstance = instance;
        mCreatedTimestamp = SystemClock.uptimeMillis();
    }
}