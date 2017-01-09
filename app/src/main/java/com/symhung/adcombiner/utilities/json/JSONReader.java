package com.symhung.adcombiner.utilities.json;

import org.json.JSONObject;

/**
 * Created by HsinHung on 2016/12/29.
 */

public interface JSONReader<T> {
    public T read(JSONObject in);
}
