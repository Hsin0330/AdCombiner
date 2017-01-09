package com.symhung.adcombiner.adframework.mopub;

import android.content.Context;

import com.mopub.nativeads.MoPubAdRenderer;
import com.mopub.nativeads.MoPubStaticNativeAdRenderer;
import com.mopub.nativeads.ViewBinder;
import com.symhung.adcombiner.R;
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
//        ViewBinder viewBinder = new ViewBinder.Builder(R.layout.native_ad_layout)
//                .mainImageId(R.id.native_ad_main_image)
//                .iconImageId(R.id.native_ad_icon_image)
//                .titleId(R.id.native_ad_title)
//                .textId(R.id.native_ad_text)
//                .privacyInformationIconImageId(R.id.native_ad_privacy_information_icon_image)
//                .build();
//        MoPubStaticNativeAdRenderer adRenderer = new MoPubStaticNativeAdRenderer(viewBinder);

        return new MopubAdLoader(context, unitId).registerAdRenderer(moPubAdRenderer);
    }

    public MopubNativeAdSource setMoPubAdRenderer(MoPubAdRenderer moPubAdRenderer) {
        this.moPubAdRenderer = moPubAdRenderer;
        return this;
    }
}