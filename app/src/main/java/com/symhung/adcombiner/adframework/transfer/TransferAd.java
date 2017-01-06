package com.symhung.adcombiner.adframework.transfer;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by HsinHung on 2017/1/6.
 */

public interface TransferAd {

    View createView(Context context, ViewGroup viewGroup);

    void renderView(View view);
}
