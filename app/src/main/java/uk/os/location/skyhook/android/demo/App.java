package uk.os.location.skyhook.android.demo;

import android.app.Application;

import com.mapbox.mapboxsdk.Mapbox;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Mapbox.getInstance(this, BuildConfig.MAPBOX_SDK_TOKEN);
    }
}
