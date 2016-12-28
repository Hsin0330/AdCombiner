package com.symhung.adcombiner.models;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by HsinHung on 2016/12/27.
 */

public class TravelLocation implements Parcelable{

    private String id;
    private String rowNumber;
    private String refWp;
    private String cat1;
    private String cat2;
    private String serialNo;
    private String memoTime;
    private String title;
    private String body;
    private String begin;
    private String end;
    private String idpt;
    private String address;
    private String postDate;
    private List<String> files;
    private String langInfo;
    private String poi;
    private String trafficInfo;
    private String longitude;
    private String latitude;
    private String mrt;

    public TravelLocation(JSONObject jsonObject) {

        id = jsonObject.optString("_id");
        rowNumber = jsonObject.optString("RowNumber");
        refWp = jsonObject.optString("REF_WP");
        cat1 = jsonObject.optString("CAT1");
        cat2 = jsonObject.optString("CAT2");
        serialNo = jsonObject.optString("SERIAL_NO");
        memoTime = jsonObject.optString("MEMO_TIME");
        title = jsonObject.optString("stitle");
        body = jsonObject.optString("xbody");
        begin = jsonObject.optString("avBegin");
        end = jsonObject.optString("abEnd");
        idpt = jsonObject.optString("idpt");
        address = jsonObject.optString("address");
        postDate = jsonObject.optString("xpostDate");
        files = new ArrayList<>(Arrays.asList(jsonObject.optString("file").toLowerCase().split(".jpg")));
        langInfo = jsonObject.optString("langinfo");
        poi = jsonObject.optString("POI");
        trafficInfo = jsonObject.optString("info");
        longitude = jsonObject.optString("longitude");
        latitude = jsonObject.optString("latitude");
        mrt = jsonObject.optString("MRT");
    }

    protected TravelLocation(Parcel in) {
        id = in.readString();
        rowNumber = in.readString();
        refWp = in.readString();
        cat1 = in.readString();
        cat2 = in.readString();
        serialNo = in.readString();
        memoTime = in.readString();
        title = in.readString();
        body = in.readString();
        begin = in.readString();
        end = in.readString();
        idpt = in.readString();
        address = in.readString();
        postDate = in.readString();
        files = in.createStringArrayList();
        langInfo = in.readString();
        poi = in.readString();
        trafficInfo = in.readString();
        longitude = in.readString();
        latitude = in.readString();
        mrt = in.readString();
    }

    public static final Creator<TravelLocation> CREATOR = new Creator<TravelLocation>() {
        @Override
        public TravelLocation createFromParcel(Parcel in) {
            return new TravelLocation(in);
        }

        @Override
        public TravelLocation[] newArray(int size) {
            return new TravelLocation[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(rowNumber);
        dest.writeString(refWp);
        dest.writeString(cat1);
        dest.writeString(cat2);
        dest.writeString(serialNo);
        dest.writeString(memoTime);
        dest.writeString(title);
        dest.writeString(body);
        dest.writeString(begin);
        dest.writeString(end);
        dest.writeString(idpt);
        dest.writeString(address);
        dest.writeString(postDate);
        dest.writeStringList(files);
        dest.writeString(langInfo);
        dest.writeString(poi);
        dest.writeString(trafficInfo);
        dest.writeString(longitude);
        dest.writeString(latitude);
        dest.writeString(mrt);
    }

    public String getId() {
        return id;
    }

    public String getRowNumber() {
        return rowNumber;
    }

    public String getRefWp() {
        return refWp;
    }

    public String getCat1() {
        return cat1;
    }

    public String getCat2() {
        return cat2;
    }

    public String getSerialNo() {
        return serialNo;
    }

    public String getMemoTime() {
        return memoTime;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public String getBegin() {
        return begin;
    }

    public String getEnd() {
        return end;
    }

    public String getIdpt() {
        return idpt;
    }

    public String getAddress() {
        return address;
    }

    public String getPostDate() {
        return postDate;
    }

    public List<String> getFiles() {
        return files;
    }

    public String getLangInfo() {
        return langInfo;
    }

    public String getPoi() {
        return poi;
    }

    public String getTrafficInfo() {
        return trafficInfo;
    }

    public String getLongitude() {
        return longitude;
    }

    public String getLatitude() {
        return latitude;
    }

    public String getMrt() {
        return mrt;
    }
}
