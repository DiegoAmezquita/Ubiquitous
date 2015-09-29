/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.sunshine.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Digital watch face with seconds. In ambient mode, the seconds aren't displayed. On devices with
 * low-bit ambient mode, the text is drawn without anti-aliasing in ambient mode.
 */
public class SunshineWatchFace extends CanvasWatchFaceService {
    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);

    private static final String HIGH = "high";
    private static final String LOW = "low";
    private static final String ASSET = "asset";
    private static final int TIMEOUT_MS = 5000;

    /**
     * Update rate in milliseconds for interactive mode. We update once a second since seconds are
     * displayed in interactive mode.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    GoogleApiClient mGoogleApiClient;
    private static final String TAG = "DIEGO DEBUG";

    String mLowTemp = "0";
    String mHighTemp = "0";

    Bitmap mBitmapWeather;
    int mWeatherImageSize;

    Engine engine;

    @Override
    public Engine onCreateEngine() {
        engine = new Engine();
        mGoogleApiClient = new GoogleApiClient.Builder(SunshineWatchFace.this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {
                        Log.d(TAG, "GoogleApiClient is Connected");

                        startListeners();
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                        Log.d(TAG, "GoogleApiClient is ConnectionSuspended");
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult connectionResult) {
                        Log.d(TAG, "The connection of GoogleApiClient is failed");
                    }
                })
                .addApi(Wearable.API) // tell Google API that we want to use Warable API
                .build();


        return engine;
    }

    private class Engine extends CanvasWatchFaceService.Engine implements DataApi.DataListener {
        final Handler mUpdateTimeHandler = new EngineHandler(this);

        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };

        boolean mRegisteredTimeZoneReceiver = false;

        Paint mBackgroundPaint;
        Paint mTextTimePaint;
        Paint mTextDatePaint;
        Paint mTextHighTempPaint;
        Paint mTextLowTempPaint;

        Rect mTextTimeBounds;
        Rect mTextDateBounds;

        Rect mTextHighTempBounds;
        Rect mTextLowTempBounds;

        boolean mAmbient;

        Time mTime;

        float mXOffset;
        float mYOffset;

        float mYSeparationDateTemp;
        float mYSeparationTimeDate;

        String mDateToShow;


        float mLineSeparatorWidth;
        float mSeparationTempItems;

        Resources mResources;

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);


            setWatchFaceStyle(new WatchFaceStyle.Builder(SunshineWatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());
            mResources = SunshineWatchFace.this.getResources();

            mYSeparationDateTemp = mResources.getDimension(R.dimen.digital_y_date_temp_separation);
            mYSeparationTimeDate = mResources.getDimension(R.dimen.digital_y_time_date_separation);
            mLineSeparatorWidth = mResources.getDimension(R.dimen.line_separator_width);

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(mResources.getColor(R.color.digital_background));

            mTextTimePaint = new Paint();
            mTextTimePaint = createTextPaint(mResources.getColor(R.color.digital_text));

            mTextDatePaint = new Paint();
            mTextDatePaint = createTextPaint(mResources.getColor(R.color.digital_text_date));

            mTextHighTempPaint = new Paint();
            mTextHighTempPaint = createTextPaint(mResources.getColor(R.color.digital_text));

            mTextLowTempPaint = new Paint();
            mTextLowTempPaint = createTextPaint(mResources.getColor(R.color.digital_text_date));

            mTextTimeBounds = new Rect();
            mTextDateBounds = new Rect();

            mTextHighTempBounds = new Rect();
            mTextLowTempBounds = new Rect();

            mBitmapWeather = BitmapFactory.decodeResource(getResources(), R.drawable.art_clear);

            mWeatherImageSize = (int) mResources.getDimension(R.dimen.weather_image_size);

            loadWeatherImage(mBitmapWeather);

            SimpleDateFormat format = new SimpleDateFormat("EEE, MMM d, yyyy", Locale.ENGLISH);
            mDateToShow = format.format(new Date());

            mTime = new Time();
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        private Paint createTextPaint(int textColor) {
            Paint paint = new Paint();
            paint.setColor(textColor);
            paint.setTypeface(NORMAL_TYPEFACE);
            paint.setAntiAlias(true);
            return paint;
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
            } else {
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();


            if (visible) {
                mGoogleApiClient.connect();
            } else {
                if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                    Wearable.DataApi.removeListener(mGoogleApiClient, this);
                    mGoogleApiClient.disconnect();
                }
            }
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            SunshineWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            SunshineWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            // Load resources that have alternate values for round watches.
            boolean isRound = insets.isRound();
            mYOffset = mResources.getDimension(isRound
                    ? R.dimen.digital_y_offset_round : R.dimen.digital_y_offset);

            mXOffset = mResources.getDimension(isRound
                    ? R.dimen.digital_x_offset_round : R.dimen.digital_x_offset);
            float textSize = mResources.getDimension(isRound
                    ? R.dimen.digital_text_size_round : R.dimen.digital_text_size);

            float textSizeDate = mResources.getDimension(isRound
                    ? R.dimen.digital_text_size_date_round : R.dimen.digital_text_size_date);

            float textSizeTemp = mResources.getDimension(isRound
                    ? R.dimen.digital_text_size_temp_round : R.dimen.digital_text_size_temp);

            mTextTimePaint.setTextSize(textSize);
            mTextDatePaint.setTextSize(textSizeDate);

            mTextHighTempPaint.setTextSize(textSizeTemp);
            mTextLowTempPaint.setTextSize(textSizeTemp);


            mSeparationTempItems = mResources.getDimension(isRound ? R.dimen.separation_temp_items : R.dimen.separation_temp_items_round);

        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                if (mLowBitAmbient) {
                    mTextTimePaint.setAntiAlias(!inAmbientMode);
                    mTextDatePaint.setAntiAlias(!inAmbientMode);
                }
                if (!mAmbient) {
                    mBackgroundPaint.setColor(mResources.getColor(R.color.black));
                } else {
                    mBackgroundPaint.setColor(mResources.getColor(R.color.digital_background));
                }


                invalidate();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            // Draw the background.
            canvas.drawRect(0, 0, bounds.width(), bounds.height(), mBackgroundPaint);

            // Draw H:MM in ambient mode or H:MM:SS in interactive mode.
            mTime.setToNow();
            String text = String.format("%d:%02d", mTime.hour, mTime.minute);
            String textDate = mDateToShow;

            mTextTimePaint.getTextBounds(text, 0, text.length(), mTextTimeBounds);
            mTextDatePaint.getTextBounds(textDate, 0, textDate.length(), mTextDateBounds);

            float widthTimeText = mTextTimeBounds.width();

            float heightDateText = mTextDateBounds.height();
            float widthDateText = mTextDateBounds.width();

            float textTimePositionX = bounds.width() / 2 - widthTimeText / 2;
            float textDatePositionX = bounds.width() / 2 - widthDateText / 2;

            canvas.drawText(text, textTimePositionX, mYOffset, mTextTimePaint);
            canvas.drawText(textDate, textDatePositionX, mYOffset + heightDateText + mYSeparationTimeDate, mTextDatePaint);

            float positionYTemp = mYOffset + heightDateText + mYSeparationTimeDate + mYSeparationDateTemp;
            float positionYLine = mYOffset + heightDateText + mYSeparationTimeDate + mYSeparationDateTemp / 2;

            drawTemperatureInfo(canvas, bounds, positionYTemp);
            drawLineSeparator(canvas, bounds, positionYLine);
        }

        private void drawTemperatureInfo(Canvas canvas, Rect bounds, float positionY) {
            mTextLowTempPaint.getTextBounds(mLowTemp, 0, mLowTemp.length(), mTextLowTempBounds);
            mTextHighTempPaint.getTextBounds(mHighTemp, 0, mHighTemp.length(), mTextHighTempBounds);

            float widthHighTempText = mTextHighTempBounds.width();

            float textHighTempPositionX = bounds.width() / 2 - widthHighTempText / 2;

            canvas.drawText(mLowTemp, bounds.width() / 2 + mSeparationTempItems, positionY + mTextLowTempBounds.height(), mTextLowTempPaint);
            canvas.drawText(mHighTemp, textHighTempPositionX, positionY + mTextHighTempBounds.height(), mTextHighTempPaint);


            drawWeatherImage(canvas, bounds, positionY + mTextHighTempBounds.height() / 2);

        }

        private void drawLineSeparator(Canvas canvas, Rect bounds, float positionY) {
            float centerX = bounds.width() / 2;
            canvas.drawLine(centerX - mLineSeparatorWidth / 2, positionY, centerX + mLineSeparatorWidth / 2, positionY, mTextLowTempPaint);
        }

        private void drawWeatherImage(Canvas canvas, Rect bounds, float positionY) {
            canvas.drawBitmap(mBitmapWeather, bounds.width() / 2 - mSeparationTempItems - mBitmapWeather.getWidth(), positionY - mBitmapWeather.getHeight() / 2, null);
        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }


        @Override
        public void onDataChanged(DataEventBuffer dataEventBuffer) {
            for (DataEvent event : dataEventBuffer) {
                Log.d(TAG, "Type: " + event.getType());
                if (event.getType() == DataEvent.TYPE_CHANGED) {
                    // DataItem changed
                    DataItem item = event.getDataItem();
                    Log.d(TAG, "PATH: " + item.getUri().getPath());
                    if (item.getUri().getPath().equals("/WeatherInfo")) {
                        DataMap dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                        if (dataMap.containsKey(LOW)) {
                            mLowTemp = dataMap.getString(LOW);
                        }
                        if (dataMap.containsKey(HIGH)) {
                            mHighTemp = dataMap.getString(HIGH);
                        }
                        if (dataMap.containsKey(ASSET)) {
                            Asset asset = dataMap.getAsset(ASSET);
                            Bitmap bitmap = loadBitmapFromAsset(asset);
                            loadWeatherImage(bitmap);
                        }
                    }
                }
            }
        }
    }

    private void startListeners() {
        Wearable.DataApi.addListener(mGoogleApiClient, engine);
    }

    public Bitmap loadBitmapFromAsset(Asset asset) {
        if (asset == null) {
            throw new IllegalArgumentException("Asset must be non-null");
        }
        ConnectionResult result =
                mGoogleApiClient.blockingConnect(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        if (!result.isSuccess()) {
            return null;
        }
        // convert asset into a file descriptor and block until it's ready
        InputStream assetInputStream = Wearable.DataApi.getFdForAsset(
                mGoogleApiClient, asset).await().getInputStream();

        if (assetInputStream == null) {
            return null;
        }
        // decode the stream into a bitmap
        return BitmapFactory.decodeStream(assetInputStream);
    }


    private void loadWeatherImage(Bitmap bitmap) {
        mBitmapWeather = Bitmap.createScaledBitmap(bitmap, mWeatherImageSize, mWeatherImageSize, false);
    }


    private static class EngineHandler extends Handler {
        private final WeakReference<SunshineWatchFace.Engine> mWeakReference;

        public EngineHandler(SunshineWatchFace.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            SunshineWatchFace.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }


}
