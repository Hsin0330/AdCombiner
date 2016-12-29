package com.symhung.adcombiner;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.symhung.adcombiner.models.TravelLocation;

import java.util.List;

/**
 * Created by HsinHung on 2016/12/29.
 */

public class TravelLocationAdapter extends RecyclerView.Adapter<TravelLocationAdapter.ViewHolder> {

    private Context context;
    private List<TravelLocation> dataList;

    public TravelLocationAdapter(Context context, List<TravelLocation> dataList) {
        this.context = context;
        this.dataList = dataList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_travel, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        TravelLocation location = dataList.get(position);

        Glide.with(context).load(location.getFiles().get(0) + ".jpg").into(holder.scene);

        holder.title.setText(location.getTitle());
        holder.address.setText(location.getAddress());
        holder.info.setText(location.getTrafficInfo());
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    public void addAll(List<TravelLocation> list) {
        dataList.addAll(list);
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        ImageView scene;
        TextView title;
        TextView address;
        TextView info;

        public ViewHolder(View itemView) {
            super(itemView);

            scene = (ImageView) itemView.findViewById(R.id.item_travel_scene);
            title = (TextView) itemView.findViewById(R.id.item_travel_title);
            address = (TextView) itemView.findViewById(R.id.item_travel_address);
            info = (TextView) itemView.findViewById(R.id.item_travel_info);
        }
    }
}
