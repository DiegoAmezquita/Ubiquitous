package com.example.android.sunshine.app.wear;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.Date;

public class WearSync implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {


    private static final String HIGH = "high";
    private static final String LOW = "low";
    private static final String ASSET = "asset";

    private static final String LOG_TAG = WearSync.class.getSimpleName();

    private static WearSync instance;

    private GoogleApiClient mGoogleApiClient;

    private WearSync(Context context) {
        initPlayServices(context);
    }

    public static WearSync getInstance(Context context) {
        if (instance == null) {
            instance = new WearSync(context);
        }
        return instance;
    }


    private void initPlayServices(Context context) {
        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();

        mGoogleApiClient.connect();
    }

    public void connect() {
        mGoogleApiClient.connect();
    }

    public void disconnect() {
        mGoogleApiClient.disconnect();
    }

    public void sendWeatherInfo(WearInfo wearInfo) {
        Log.e("DIEGO DEBUG", "TRYING TO SEND MESSAGE");

        new DataTask(wearInfo).execute();


        // the parameter of  node id can be empty string "".
        // the third parameter is message path.
//                Wearable.MessageApi.sendMessage(mGoogleApiClient, "", "/WeatherInfo", config.toByteArray())
//                .setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
//                    @Override
//                    public void onResult(MessageApi.SendMessageResult sendMessageResult) {
//                        Log.d(LOG_TAG, "SendMessageStatus: " + sendMessageResult.getStatus());
//                    }
//                });
    }


    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d(LOG_TAG, "onConnected: " + connectionHint);


    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.d(LOG_TAG, "onConnectionSuspended: " + cause);
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.d(LOG_TAG, "onConnectionFailed: " + result);
    }


    class DataTask extends AsyncTask<Node, Void, Void> {


        private WearInfo wearInfo;

        public DataTask(WearInfo wearInfo) {
            this.wearInfo = wearInfo;
        }

        @Override
        protected Void doInBackground(Node... nodes) {

            PutDataMapRequest request = PutDataMapRequest.create("/WeatherInfo");
            request.getDataMap().putString(HIGH, wearInfo.getHigh());
            request.getDataMap().putString(LOW, wearInfo.getLow());
            request.getDataMap().putAsset(ASSET, wearInfo.getAsset());
            request.getDataMap().putLong("time", new Date().getTime());
            PutDataRequest dataRequest = request.asPutDataRequest();


            Log.e(LOG_TAG, "getDataMap: " + request.getDataMap());

            Wearable.DataApi.putDataItem(mGoogleApiClient, dataRequest)
                    .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                        @Override
                        public void onResult(DataApi.DataItemResult dataItemResult) {
                            Log.e(LOG_TAG, "Sending image was successful: " + dataItemResult.getStatus()
                                    .isSuccess());
                        }
                    });


            return null;
        }
    }


}
