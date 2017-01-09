package com.symhung.adcombiner.adframework.mopub;

import android.content.Context;

import com.mopub.nativeads.MoPubAdRenderer;
import com.symhung.adcombiner.adframework.base.AdLoader;
import com.symhung.adcombiner.adframework.base.TransferAdSource;

/**
 * Created by HsinHung on 2017/1/9.
 */
public class MopubNativeAdSource extends TransferAdSource{

    private Context context;
    private String unitId;
    private MoPubAdRenderer moPubAdRenderer;

    public MopubNativeAdSource(Context context, String unitId) {
        this.context = context;
        this.unitId = unitId;
    }

    @Override
    public AdLoader createAdLoader() {
        return new MopubAdLoader(context, unitId).registerAdRenderer(moPubAdRenderer);
    }

    public MopubNativeAdSource setMoPubAdRenderer(MoPubAdRenderer moPubAdRenderer) {
        this.moPubAdRenderer = moPubAdRenderer;
        return this;
    }
}