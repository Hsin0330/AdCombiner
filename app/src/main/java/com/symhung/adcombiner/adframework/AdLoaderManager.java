package com.symhung.adcombiner.adframework;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by HsinHung on 2017/1/3.
 */

public class AdLoaderManager {

    private Activity activity;
    private List<RecyclerView.Adapter> adapterList = new ArrayList<>();

    private int startPosition;
    private int endPosition;
    private int repeatInterval;

    public AdLoaderManager(Activity activity, int startPosition, int endPosition, int repeatInterval) {
        this.activity = activity;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.repeatInterval = repeatInterval;
    }

    public void addAdAdapter(RecyclerView.Adapter loader) {
        this.adapterList.add(loader);
    }

    public void addAdAdapter(int index, RecyclerView.Adapter loader) {
        this.adapterList.add(index, loader);
    }

    public int getRepeatInterval() {
        return repeatInterval;
    }

    public void setRepeatInterval(int repeatInterval) {
        this.repeatInterval = repeatInterval;
    }

    public void setStartPosition(int startPosition) {
        this.startPosition = startPosition;
    }

    public void setEndPosition(int endPosition) {
        this.endPosition = endPosition;
    }

    public int getItemCount() {
        return 0;
    }

    public void onCreateViewHolder(final ViewGroup parent, final int viewType) {

    }

    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {

    }

    public int getItemViewType(final int position) {
        return 0;
    }
}
