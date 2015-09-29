package com.example.android.sunshine.app;

import android.util.Log;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

public class DataLayerListenerService extends WearableListenerService {

    private static final String TAG = "DataLayerSample";
    private static final String START_ACTIVITY_PATH = "/start-activity";
    private static final String DATA_ITEM_RECEIVED_PATH = "/data-item-received";

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {

        for (DataEvent event : dataEvents) {

            Log.d("onDataChanged",
                    "Event received: " + event.getDataItem().getUri());

            String eventUri = event.getDataItem().getUri().toString();

            if (eventUri.contains("/myapp/myevent")) {

                DataMapItem dataItem = DataMapItem.fromDataItem(event.getDataItem());
                String[] data = dataItem.getDataMap().getStringArray("contents");

                Log.d("onDataChanged", "Sending timeline to the listener");

                Log.e("DIEGO DEBUG", "Data " + data);

//                myListener.onDataReceived(data);
            }
        }
    }

    // RECIVE a message
    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        // convert a byte array to DataMap
        byte[] rawData = messageEvent.getData();
        DataMap dataMap = DataMap.fromByteArray(rawData);


        Log.e("DIEGO DEBUG", "ALGO LLEGO");

        // we have different methods for different messages
        if (messageEvent.getPath().equals("/WeatherInfo")) {
//            fetchInfo(dataMap);
        }

        if (messageEvent.getPath().equals("/WeatherWatchFace/Config")) {
//            fetchConfig(dataMap);
        }
    }
}
