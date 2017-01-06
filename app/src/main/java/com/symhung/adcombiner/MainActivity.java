package com.symhung.adcombiner;

import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;

import com.mopub.nativeads.MoPubNative;
import com.mopub.nativeads.MoPubStaticNativeAdRenderer;
import com.mopub.nativeads.NativeAd;
import com.mopub.nativeads.NativeErrorCode;
import com.mopub.nativeads.ViewBinder;
import com.symhung.adcombiner.adframework.loader.AdLoader;
import com.symhung.adcombiner.adframework.AdRequestListener;
import com.symhung.adcombiner.adframework.loader.MopubAdLoader;
import com.symhung.adcombiner.adframework.transfer.TransferAd;
import com.symhung.adcombiner.base.BaseActivity;
import com.symhung.adcombiner.models.TravelLocation;
import com.symhung.adcombiner.network.handlers.ResponseHandler;
import com.symhung.adcombiner.network.resource.TravelResource;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseActivity {

    private static final String MY_AD_UNIT_ID = "e49a61c44e9a4bd0982dcaeb085759b2";

    private Toolbar toolbar;
    private RecyclerView recyclerView;
    private RelativeLayout adBanner;

//    private MoPubRecyclerAdapter moPubRecyclerAdapter;
    private TravelLocationAdapter adapter;

    //mopub
//    private MoPubNative moPubNative;
//    private NativeAd nativeAd;

    private AdLoader adLoader;
    private TransferAd transferAd;

    private final MoPubNative.MoPubNativeNetworkListener moPubNativeNetworkListener = new MoPubNative.MoPubNativeNetworkListener() {
        @Override
        public void onNativeLoad(NativeAd nativeAd) {
//            MainActivity.this.nativeAd = nativeAd;
//
//            View adView = nativeAd.createAdView(MainActivity.this, null);
//            nativeAd.prepare(adView);
//            nativeAd.renderAdView(adView);
//
//            adBanner.addView(adView);
        }

        @Override
        public void onNativeFail(NativeErrorCode errorCode) {

        }
    };

    private final AdRequestListener adRequestListener = new AdRequestListener() {
        @Override
        public void onError() {

        }

        @Override
        public void onAdLoaded(TransferAd transferAd) {
            View adView = transferAd.createView(MainActivity.this, null);
            transferAd.renderView(adView);

            adBanner.addView(adView);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initAds();
        initViews();
        queryData();

//        moPubNative.makeRequest();
        adLoader.request();
    }

    @Override
    protected void onResume() {
        // Request ads when the user returns to this activity.
//        moPubRecyclerAdapter.loadAds(MY_AD_UNIT_ID);
        super.onResume();
    }

    @Override
    protected void onDestroy() {
//        moPubRecyclerAdapter.destroy();
        super.onDestroy();
    }

    private void initAds() {
//        moPubNative = new MoPubNative(this, MY_AD_UNIT_ID, moPubNativeNetworkListener);
        ViewBinder viewBinder = new ViewBinder.Builder(R.layout.native_ad_layout)
                .mainImageId(R.id.native_ad_main_image)
                .iconImageId(R.id.native_ad_icon_image)
                .titleId(R.id.native_ad_title)
                .textId(R.id.native_ad_text)
                .privacyInformationIconImageId(R.id.native_ad_privacy_information_icon_image)
                .build();
        MoPubStaticNativeAdRenderer adRenderer = new MoPubStaticNativeAdRenderer(viewBinder);
//        moPubNative.registerAdRenderer(adRenderer);

        adLoader = new MopubAdLoader(this, MY_AD_UNIT_ID, adRequestListener).registerAdRenderer(adRenderer);
    }

    private void initViews() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        adBanner = (RelativeLayout) findViewById(R.id.ad_banner);

        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        adapter = new TravelLocationAdapter(this, new ArrayList<TravelLocation>());

//        MoPubNativeAdPositioning.MoPubServerPositioning adPositioning =
//                MoPubNativeAdPositioning.serverPositioning();

//
//        MoPubNativeAdPositioning.MoPubClientPositioning adPositioning = MoPubNativeAdPositioning.clientPositioning().addFixedPosition(0).enableRepeatingPositions(5);
//
//        moPubRecyclerAdapter = new MoPubRecyclerAdapter(this, adapter, adPositioning);
//        moPubRecyclerAdapter.registerAdRenderer(adRenderer);


//        recyclerView.setAdapter(moPubRecyclerAdapter);
        recyclerView.setAdapter(adapter);
    }

    private void queryData() {
        TravelResource.getTravelLocation(new ResponseHandler<List<TravelLocation>>() {
            @Override
            public void messageReceived(final List<TravelLocation> msg) throws Exception {
                Log.d("MainActivity", "size : " + msg.size());

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.addAll(msg);
                    }
                });
            }

            @Override
            public void exceptionCaught(Exception e) {
                e.printStackTrace();
            }
        });
    }
}
