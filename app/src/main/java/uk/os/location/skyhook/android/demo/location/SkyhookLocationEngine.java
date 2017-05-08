package uk.os.location.skyhook.android.demo.location;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.mapbox.services.android.telemetry.location.LocationEngine;
import com.mapbox.services.android.telemetry.location.LocationEngineListener;
import com.mapbox.services.android.telemetry.permissions.PermissionsManager;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class SkyhookLocationEngine extends LocationEngine {

    private static final String LOG_TAG = SkyhookLocationEngine.class.getSimpleName();
    private static LocationEngine instance;

    private final Context context;
    private boolean isConnected;
    private Location mLastLocation = null;

    // previously called Subscription in RxJava
    private Disposable locationSubscription;

    /**
     *
     * @param context context used to establish the application context
     * @return a LocationEngine backed by the Skyhook Android SDK
     */
    public static synchronized LocationEngine getLocationEngine(Context context) {
        Log.d(LOG_TAG, "ZXC getLocationEngine");
        if (instance == null) {
            instance = new SkyhookLocationEngine(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * @param context context used to establish the application context
     */
    private SkyhookLocationEngine(Context context) {
        super();
        this.context = context;
    }

    @Override
    public void activate() {
        Log.d(LOG_TAG, "activate");
        isConnected = true;

        for (LocationEngineListener listener : locationListeners) {
            listener.onConnected();
        }
    }

    @Override
    public void deactivate() {
        Log.d(LOG_TAG, "deactivate");
        isConnected = false;
    }

    @Override
    public boolean isConnected() {
        Log.d(LOG_TAG, "isConnected");
        return isConnected;
    }

    @Override
    public Location getLastLocation() {
        Log.d(LOG_TAG, "getLastLocation");
        if (isConnected && PermissionsManager.areLocationPermissionsGranted(context)) {
            return mLastLocation;
        }
        return null;
    }

    @Override
    public void requestLocationUpdates() {
        Log.d(LOG_TAG, "requestLocationUpdates");

        boolean isExistingSubscription = locationSubscription != null;
        if (isExistingSubscription) {
            Log.d(LOG_TAG, "removeLocationUpdates: ignore request as already subscription");
            return;
        }

        locationSubscription =
                SkyhookLocationStreams.getLocationStream()
                        .subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnNext(new Consumer<Location>() {
                            @Override
                            public void accept(@NonNull Location location) throws Exception {
                                Log.d(LOG_TAG, "emitting location");
                            }
                        })
                        .subscribe(new Consumer<Location>() {
                            @Override
                            public void accept(@NonNull Location location) throws Exception {
                                if (isConnected) {
                                    Log.d(LOG_TAG, "isConnected - notifying listeners");
                                } else {
                                    Log.d(LOG_TAG, "is NOT connected - I probably should not be " +
                                            "notifying listeners...but I do at the moment!");
                                }

                                for (LocationEngineListener listener : locationListeners) {
                                    listener.onLocationChanged(location);
                                }
                            }
                        }, new Consumer<Throwable>() {
                            @Override
                            public void accept(@NonNull Throwable throwable) throws Exception {
                                Log.e(LOG_TAG, "yikes - " + throwable);
                            }
                        });
    }

    @Override
    public void removeLocationUpdates() {
        Log.d(LOG_TAG, "removeLocationUpdates");
        if (locationSubscription == null) {
            Log.d(LOG_TAG, "removeLocationUpdates: no subscription so ignoring");
            return;
        }
        locationSubscription.dispose();
        locationSubscription = null;
    }
}
