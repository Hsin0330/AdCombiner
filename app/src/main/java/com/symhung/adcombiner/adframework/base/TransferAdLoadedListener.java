package com.symhung.adcombiner.adframework.base;

/**
 * Created by HsinHung on 2017/1/9.
 */

public interface TransferAdLoadedListener {

    void onAdLoaded(int position);

    void onAdRemoved(int position);
}
