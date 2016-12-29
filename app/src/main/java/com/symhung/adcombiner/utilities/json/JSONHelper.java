package com.symhung.adcombiner.utilities.json;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by HsinHung on 2016/12/29.
 */

public class JSONHelper {

    public static <T> List<T> toList(JSONArray jsonArray, Class<? extends JSONReader> classOfObject) throws Exception {
        List<T> list = new ArrayList<>();

        if (jsonArray != null) {
            for(int i = 0; i < jsonArray.length(); i ++) {
                list.add((T) classOfObject.newInstance().read(jsonArray.getJSONObject(i)));
            }
        }
        return list;
    }
}
