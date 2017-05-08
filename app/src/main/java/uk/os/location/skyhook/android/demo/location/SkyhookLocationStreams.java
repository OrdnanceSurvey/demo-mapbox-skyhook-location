package uk.os.location.skyhook.android.demo.location;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

import com.skyhookwireless.wps.WPSCertifiedLocationCallback;
import com.skyhookwireless.wps.WPSContinuation;
import com.skyhookwireless.wps.WPSLocation;
import com.skyhookwireless.wps.WPSLocationCallback;
import com.skyhookwireless.wps.WPSPeriodicLocationCallback;
import com.skyhookwireless.wps.WPSReturnCode;
import com.skyhookwireless.wps.WPSStreetAddressLookup;
import com.skyhookwireless.wps.XPS;

import java.util.concurrent.CountDownLatch;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Action;

public class SkyhookLocationStreams {

    private enum Chosen {CERTIFIED, PERIODIC, OFFLINE}

    private static final String LOG_TAG = SkyhookLocationStreams.class.getSimpleName();
    private static final Chosen LOCATION_SOURCE = Chosen.CERTIFIED;

    private static XPS xps;
    private static Context appContext;

    public static void setXps(XPS xps) {
        SkyhookLocationStreams.xps = xps;
    }

    public static void setContext(Context context) {
        appContext = context.getApplicationContext();
    }

    public static Observable<Location> getLocationStream() {
        return getChosenStream().doOnDispose(new Action() {
            @Override
            public void run() throws Exception {
                xps.abort();
            }
        });
    }

    private static Observable<Location> getChosenStream() {
        switch (LOCATION_SOURCE) {
            case CERTIFIED:
                return getLocationStreamCertified();
            case PERIODIC:
                return getLocationStreamPeriodic();
            case OFFLINE:
                return getLocationStreamOffline();
            default:
                throw new IllegalStateException("unsupported stream type!");
        }
    }

    /**
     * @return a certified stream
     */
    private static Observable<Location> getLocationStreamCertified() {
        return Observable.create(new ObservableOnSubscribe<Location>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<Location> emitter) throws Exception {
                try {
                    foreverCertifiedLocation(xps, emitter);
                    emitter.onComplete();
                } catch (Exception e) {
                    emitter.onError(e);
                }
            }
        });
    }

    /**
     * @return a certified stream
     */
    private static Observable<Location> getLocationStreamOffline() {
        return Observable.create(new ObservableOnSubscribe<Location>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<Location> emitter) throws Exception {
                try {
                    foreverOfflineLocation(xps, emitter);
                    emitter.onComplete();
                } catch (Exception e) {
                    emitter.onError(e);
                }
            }
        });
    }

    /**
     * @return a sensible base stream
     */
    private static Observable<Location> getLocationStreamPeriodic() {
        return Observable.create(new ObservableOnSubscribe<Location>() {
            @Override
            public void subscribe(final ObservableEmitter<Location> emitter) throws Exception {
                try {
                    foreverPeriodicLocation(xps, emitter);
                    emitter.onComplete();
                } catch (Exception e) {
                    emitter.onError(e);
                }
            }
        });
    }

    private static void foreverCertifiedLocation(XPS mXps,
                                                 final ObservableEmitter<Location> emitter) {
        for (;;) {
            // if subscribers finished then we exit
            boolean isFinished = emitter.isDisposed();
            if (isFinished) {
                break;
            }

            final CountDownLatch countDownLatch = new CountDownLatch(1);
            mXps.getCertifiedLocation(null, WPSStreetAddressLookup.WPS_NO_STREET_ADDRESS_LOOKUP,
                    new WPSCertifiedLocationCallback() {
                @Override
                public WPSContinuation handleWPSCertifiedLocation(WPSLocation[] wpsLocations) {
                    emitter.onNext(toAndroidLocation(wpsLocations[0]));
                    countDownLatch.countDown();
                    return WPSContinuation.WPS_STOP; // TODO
                }

                @Override
                public void done() { }

                @Override
                public WPSContinuation handleError(WPSReturnCode wpsReturnCode) {
                    countDownLatch.countDown();
                    return WPSContinuation.WPS_STOP;
                }
            });

            // let us wait for a location
            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static void foreverOfflineLocation(XPS mXps, final ObservableEmitter<Location> emitter) {
        final byte[] key = "Skyhook".getBytes();
        byte[] token = mXps.getOfflineToken(null, key);
        Log.e(LOG_TAG, "token: " + token);
        if (token == null) {
            Log.e(LOG_TAG, "No token");
        } else {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
            prefs.edit().putString("skyhook-token", new String(token)).apply();
        }

        for (;;) {
            // if subscribers finished then we exit
            boolean isFinished = emitter.isDisposed();
            if (isFinished) {
                break;
            }

            final CountDownLatch countDownLatch = new CountDownLatch(1);
            mXps.getOfflineLocation(null, key, token, new WPSLocationCallback() {
                        @Override
                        public void handleWPSLocation(WPSLocation wpsLocation) {
                            emitter.onNext(toAndroidLocation(wpsLocation));
                            countDownLatch.countDown();
                        }

                        @Override
                        public void done() { }

                        @Override
                        public WPSContinuation handleError(WPSReturnCode wpsReturnCode) {
                            countDownLatch.countDown();
                            return WPSContinuation.WPS_STOP;
                        }
                    }

            );

            // let us wait for a location
            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // repeat
        }
    }

    private static void foreverPeriodicLocation(XPS mXps, final ObservableEmitter<Location> emitter) {

        final long updateInterval = 1000;
        final int unlimited = 0;

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        mXps.getPeriodicLocation(null, WPSStreetAddressLookup.WPS_NO_STREET_ADDRESS_LOOKUP,
                updateInterval, unlimited, new WPSPeriodicLocationCallback() {
            @Override
            public WPSContinuation handleWPSPeriodicLocation(WPSLocation wpsLocation) {
                emitter.onNext(toAndroidLocation(wpsLocation));

                if (emitter.isDisposed()) {
                    countDownLatch.countDown();
                    return WPSContinuation.WPS_STOP; // TODO
                } else {
                    return WPSContinuation.WPS_CONTINUE; // TODO
                }
            }

            @Override
            public void done() {

            }

            @Override
            public WPSContinuation handleError(WPSReturnCode wpsReturnCode) {
                if (emitter.isDisposed()) {
                    countDownLatch.countDown();
                    Log.e(LOG_TAG, "Error (stopping): " + wpsReturnCode);
                    return WPSContinuation.WPS_STOP; // TODO
                } else {
                    Log.e(LOG_TAG, "Error (continuing): " + wpsReturnCode);
                    return WPSContinuation.WPS_CONTINUE; // TODO
                }
            }
        });

        // let us wait for a location
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            Log.e(LOG_TAG, "Latch interrupted", e);
        }
    }

    private static Location toAndroidLocation(WPSLocation location) {
        final Location androidLocation = new Location("Skyhook");
        androidLocation.setLatitude(location.getLatitude());
        androidLocation.setLongitude(location.getLongitude());
        androidLocation.setAccuracy(location.getHPE());
        androidLocation.setBearing((float)location.getBearing());
        androidLocation.setAltitude(location.getAltitude());
        androidLocation.setSpeed(location.getScore());
        androidLocation.setTime(location.getTime());
        androidLocation.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        return androidLocation;
    }
}
