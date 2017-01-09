package com.symhung.adcombiner.adframework.base;

import android.os.SystemClock;
import android.support.annotation.NonNull;

/**
 * Created by HsinHung on 2017/1/9.
 */

class TimestampWrapper<T> {
    @NonNull
    final T mInstance;
    long mCreatedTimestamp;

    TimestampWrapper(@NonNull final T instance) {
        mInstance = instance;
        mCreatedTimestamp = SystemClock.uptimeMillis();
    }
}