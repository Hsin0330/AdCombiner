package com.symhung.adcombiner.adframework.mopub;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.mopub.nativeads.NativeAd;
import com.symhung.adcombiner.adframework.base.TransferAd;

/**
 * Created by HsinHung on 2017/1/6.
 */

public class MopubTransferAd implements TransferAd {

    private NativeAd nativeAd;

    public MopubTransferAd(NativeAd nativeAd) {
        this.nativeAd = nativeAd;
    }

    @Override
    public View createView(Context context, ViewGroup viewGroup) {
        return nativeAd.createAdView(context, viewGroup);
    }

    @Override
    public void renderView(View view) {
        nativeAd.prepare(view);
        nativeAd.renderAdView(view);
    }

    @Override
    public void clear(View view) {
        nativeAd.clear(view);
    }

    @Override
    public void destroy() {
        nativeAd.destroy();
    }
}
