package com.symhung.adcombiner.adframework.loader;

import android.content.Context;

import com.mopub.nativeads.MoPubAdRenderer;
import com.mopub.nativeads.MoPubNative;
import com.mopub.nativeads.NativeAd;
import com.mopub.nativeads.NativeErrorCode;
import com.symhung.adcombiner.adframework.AdRequestListener;
import com.symhung.adcombiner.adframework.transfer.MopubTransferAd;

/**
 * Created by HsinHung on 2017/1/6.
 */

public class MopubAdLoader implements AdLoader {

    private MoPubNative moPubNative;

    public MopubAdLoader(Context context, String unitId,final AdRequestListener adRequestListener) {
        this.moPubNative = new MoPubNative(context, unitId, new MoPubNative.MoPubNativeNetworkListener() {
            @Override
            public void onNativeLoad(NativeAd nativeAd) {
                if (adRequestListener != null) {
                    adRequestListener.onAdLoaded(new MopubTransferAd(nativeAd));
                }
            }

            @Override
            public void onNativeFail(NativeErrorCode errorCode) {
                if (adRequestListener != null) {
                    adRequestListener.onError();
                }
            }
        });
    }

    public MopubAdLoader registerAdRenderer(MoPubAdRenderer moPubAdRenderer) {
        moPubNative.registerAdRenderer(moPubAdRenderer);
        return this;
    }

    @Override
    public void request() {
        moPubNative.makeRequest();
    }

    @Override
    public void destroy() {
        moPubNative.destroy();
    }
}
