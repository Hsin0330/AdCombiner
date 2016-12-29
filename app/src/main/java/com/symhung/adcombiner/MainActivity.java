package com.symhung.adcombiner;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import com.symhung.adcombiner.base.BaseActivity;
import com.symhung.adcombiner.models.TravelLocation;
import com.symhung.adcombiner.network.handlers.ResponseHandler;
import com.symhung.adcombiner.network.resource.TravelResource;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseActivity {

    private Toolbar toolbar;
    private RecyclerView recyclerView;

    private TravelLocationAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        queryData();
    }

    private void initViews() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        recyclerView.setAdapter(adapter = new TravelLocationAdapter(this, new ArrayList<TravelLocation>()));
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
