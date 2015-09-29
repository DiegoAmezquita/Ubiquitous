package com.example.android.sunshine.app.wear;

import android.graphics.Bitmap;

import com.google.android.gms.wearable.Asset;

import java.io.ByteArrayOutputStream;

public class WearInfo {

    private String high;
    private String low;
    private Asset asset;

    public WearInfo(String high, String low, Asset asset) {
        this.high = high;
        this.low = low;
        this.asset = asset;
    }

    public String getHigh() {
        return high;
    }

    public String getLow() {
        return low;
    }

    public Asset getAsset() {
        return asset;
    }


}
