package uk.os.location.skyhook.android.demo;

import android.app.Application;

import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.services.android.telemetry.MapboxTelemetry;
import com.skyhookwireless.wps.XPS;

import uk.os.location.skyhook.android.demo.location.SkyhookLocationStreams;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Mapbox customer config
        Mapbox.getInstance(this, BuildConfig.MAPBOX_SDK_TOKEN);
        MapboxTelemetry.getInstance().setTelemetryEnabled(false);

        // Skyhook customer config
        final XPS xps = new XPS(this);
        xps.setKey(BuildConfig.SKYHOOK_SDK_TOKEN);
        SkyhookLocationStreams.setXps(xps);
    }
}
