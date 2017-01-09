package com.symhung.adcombiner.adframework.mopub;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.mopub.nativeads.MoPubAdRenderer;
import com.mopub.nativeads.MoPubNative;
import com.mopub.nativeads.NativeAd;
import com.mopub.nativeads.NativeErrorCode;
import com.symhung.adcombiner.adframework.base.AdRequestListener;
import com.symhung.adcombiner.adframework.base.AdLoader;

/**
 * Created by HsinHung on 2017/1/6.
 */

public class MopubAdLoader extends AdLoader {

    private MoPubNative moPubNative;
    private MoPubAdRenderer moPubAdRenderer;

    public MopubAdLoader(Context context, String unitId) {
        super(context);

        this.moPubNative = new MoPubNative(context, unitId, new MoPubNative.MoPubNativeNetworkListener() {
            @Override
            public void onNativeLoad(NativeAd nativeAd) {
                if (getAdRequestListener() != null) {
                    getAdRequestListener().onAdLoaded(new MopubTransferAd(nativeAd));
                }
            }

            @Override
            public void onNativeFail(NativeErrorCode errorCode) {
                if (getAdRequestListener() != null) {
                    getAdRequestListener().onError();
                }
            }
        });
    }

    public MopubAdLoader registerAdRenderer(MoPubAdRenderer moPubAdRenderer) {
        this.moPubAdRenderer = moPubAdRenderer;
        moPubNative.registerAdRenderer(moPubAdRenderer);
        return this;
    }

    @Override
    public void request() {
        moPubNative.makeRequest();
    }

    @Override
    public void destroy() {
        moPubAdRenderer = null;
        moPubNative.destroy();
    }

    @Override
    public View createView(Context context, ViewGroup viewGroup) {
        return moPubAdRenderer.createAdView(context, viewGroup);
    }


}
