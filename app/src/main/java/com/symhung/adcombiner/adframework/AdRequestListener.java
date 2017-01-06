package com.symhung.adcombiner.adframework;

import com.symhung.adcombiner.adframework.transfer.TransferAd;

/**
 * Created by HsinHung on 2017/1/6.
 */

public interface AdRequestListener {

    void onError();

    void onAdLoaded(TransferAd transferAd);
}
