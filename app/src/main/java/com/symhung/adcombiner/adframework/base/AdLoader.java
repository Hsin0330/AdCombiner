package com.symhung.adcombiner.adframework.base;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by HsinHung on 2017/1/6.
 */

public abstract class AdLoader {

    private Context context;
    private AdRequestListener adRequestListener;

    public AdLoader(Context context) {
        this.context = context;
    }

    public abstract void request();

    public abstract void destroy();

    public abstract View createView(Context context, ViewGroup viewGroup);

    public AdRequestListener getAdRequestListener() {
        return adRequestListener;
    }

    public void setAdRequestListener(AdRequestListener adRequestListener) {
        this.adRequestListener = adRequestListener;
    }
}
