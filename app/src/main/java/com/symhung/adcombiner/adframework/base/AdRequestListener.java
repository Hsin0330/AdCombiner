package com.symhung.adcombiner.adframework.base;

import com.symhung.adcombiner.adframework.base.TransferAd;

/**
 * Created by HsinHung on 2017/1/6.
 */

public interface AdRequestListener {

    void onError();

    void onAdLoaded(TransferAd transferAd);
}
