package com.symhung.adcombiner.network.resource;

import android.support.v4.util.ArrayMap;
import android.util.Log;

import com.symhung.adcombiner.models.TravelLocation;
import com.symhung.adcombiner.network.HttpClient;
import com.symhung.adcombiner.network.handlers.ResponseHandler;
import com.symhung.adcombiner.utilities.json.JSONHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

/**
 * Created by HsinHung on 2016/12/28.
 */

public class TravelResource {

    public static final String travelLocationResourceUrl = "http://data.taipei/opendata/datalist/apiAccess?scope=resourceAquire";

    //旅遊景點 中文 資料ID Key : rid=?
    public static final String travelLocationResourceId = "36847f3f-deff-4183-a5bb-800737591de5";

    public static void getTravelLocation(final ResponseHandler<List<TravelLocation>> handler) {

        Map<String, String> arguement = new ArrayMap<>();
        arguement.put("rid", travelLocationResourceId);

        HttpClient.newPostRequest(travelLocationResourceUrl)
                .arguements(arguement)
                .execute()
                .addResponseHandler(new ResponseHandler<String>() {
                    @Override
                    public void messageReceived(String msg) throws Exception {
                        JSONObject jsonObject = new JSONObject(msg);
                        JSONObject jsonResult = jsonObject.optJSONObject("result");
                        JSONArray data = jsonResult.optJSONArray("results");

                        List<TravelLocation> travelLocationList = JSONHelper.toList(data, TravelLocation.class);
                        writeNext(travelLocationList);
                    }
                })
                .addResponseHandler(handler);
    }
}
