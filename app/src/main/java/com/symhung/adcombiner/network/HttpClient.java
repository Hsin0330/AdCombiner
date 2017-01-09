package com.symhung.adcombiner.network;

import android.net.Uri;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;
import android.util.Log;

import com.symhung.adcombiner.network.handlers.ResponseHandleQueue;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by HsinHung on 2016/12/28.
 */

public class HttpClient {

    private static final String TAG = HttpClient.class.getSimpleName();
    private static final OkHttpClient okHttpClient = new OkHttpClient();

//    public static HttpRequest newGetRequest() {
//
//    }

    public static HttpRequest newPostRequest(String resource) {
        return new HttpRequest(resource);
    }


    public static class HttpRequest {

        private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

        private final Request.Builder builder = new Request.Builder();
        private String body;

        private String resourceUrl;

        private ResponseHandleQueue responseHandleQueue;
        private long requestedAt;

        public HttpRequest(String resource) {
            resourceUrl = resource;
        }

        public HttpRequest arguements(Map<String, String> arguements) {
            if (arguements != null) {
                for(String key : arguements.keySet()) {
                    resourceUrl = Uri.parse(resourceUrl).buildUpon().appendQueryParameter(key, arguements.get(key)).build().toString();
                }
            }
            return this;
        }

        public HttpRequest post(JSONObject jsonObject) {
            body = jsonObject.toString();
            builder.post(RequestBody.create(JSON, body));

            return this;
        }

        public ResponseHandleQueue execute() {
            Request request = builder.url(resourceUrl).build();

            responseHandleQueue = new ResponseHandleQueue(this);
            execute(request, responseHandleQueue);
            return responseHandleQueue;
        }

        void execute(Request request, final ResponseHandleQueue handleQueue) {
            requestedAt = System.currentTimeMillis();
            okHttpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                    handleQueue.sendExceptionCaught(handleQueue.head(), e);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        return;
                    }

                    String result = response.body().string();
//                    if (!TextUtils.isEmpty(result)) {
//
//                    }

                    long latency = System.currentTimeMillis() - requestedAt;
                    Log.d(TAG, "latency : " + latency + " (ms)");

                    handleQueue.sendReadEvent(handleQueue.head(), result);
                }
            });
        }
    }
}
