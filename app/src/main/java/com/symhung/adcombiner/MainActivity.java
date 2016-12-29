package com.symhung.adcombiner;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import com.mopub.nativeads.MoPubNativeAdPositioning;
import com.mopub.nativeads.MoPubRecyclerAdapter;
import com.mopub.nativeads.MoPubStaticNativeAdRenderer;
import com.mopub.nativeads.RequestParameters;
import com.mopub.nativeads.ViewBinder;
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

    private MoPubRecyclerAdapter moPubRecyclerAdapter;
    private TravelLocationAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        queryData();
    }

    @Override
    protected void onResume() {
        // Request ads when the user returns to this activity.
        moPubRecyclerAdapter.loadAds(MY_AD_UNIT_ID);
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        moPubRecyclerAdapter.destroy();
        super.onDestroy();
    }

    private void initViews() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        adapter = new TravelLocationAdapter(this, new ArrayList<TravelLocation>());
        ViewBinder viewBinder = new ViewBinder.Builder(R.layout.native_ad_layout)
                .mainImageId(R.id.native_ad_main_image)
                .iconImageId(R.id.native_ad_icon_image)
                .titleId(R.id.native_ad_title)
                .textId(R.id.native_ad_text)
                .privacyInformationIconImageId(R.id.native_ad_privacy_information_icon_image)
                .build();
        MoPubNativeAdPositioning.MoPubServerPositioning adPositioning =
                MoPubNativeAdPositioning.serverPositioning();
        MoPubStaticNativeAdRenderer adRenderer = new MoPubStaticNativeAdRenderer(viewBinder);

        moPubRecyclerAdapter = new MoPubRecyclerAdapter(this, adapter, adPositioning);
        moPubRecyclerAdapter.registerAdRenderer(adRenderer);


        recyclerView.setAdapter(moPubRecyclerAdapter);
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
