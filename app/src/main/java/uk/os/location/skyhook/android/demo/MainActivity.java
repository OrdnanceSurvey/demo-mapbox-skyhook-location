package uk.os.location.skyhook.android.demo;

import android.Manifest;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.services.android.telemetry.location.LocationEngine;
import com.mapbox.services.android.telemetry.location.LocationEngineListener;
import com.mapbox.services.android.telemetry.permissions.PermissionsManager;

import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;
import uk.os.location.skyhook.android.demo.location.SkyhookLocationEngine;
import uk.os.location.skyhook.android.demo.widget.MyLocationFab;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {

    private static String TAG = MainActivity.class.getName();

    // Permission callback (e.g. for location)
    private static final int RC_LOCATION_PERM = 0;
    private static final int RC_STORAGE_PERM = 1;

    private static final String[] LOCATION_PERMS = new String[] {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION };
    private static final String[] STORAGE_PERMS = new String[] {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE};

    // Mapbox SDK
    private MapView mapView;
    private MapboxMap mapboxMap;
    private LocationEngineListener locationListenerForPan;

    // Custom location source
    private LocationEngine skyhookLocationEngine;

    // Log view
    private TextView logView = null;
    private MyLocationFab locationToggleFab;
    private LocationEngineListener locationListenerForTextView = getLocationListenerForTextView();

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!EasyPermissions.hasPermissions(this, STORAGE_PERMS)) {
            getMenuInflater().inflate(R.menu.main_options_menu, menu);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.main_sdcard_permission: {
                EasyPermissions.requestPermissions(this,
                        getString(R.string.main_options_menu_logging_rationale),
                        RC_STORAGE_PERM, STORAGE_PERMS);
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // EasyPermissions handles the request result.
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        Log.d(TAG, "onPermissionsGranted:" + requestCode + ":" + perms.size());
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        Log.d(TAG, "onPermissionsDenied:" + requestCode + ":" + perms.size());

        // (Optional) Check whether the user denied any permissions and checked "NEVER ASK AGAIN."
        // This will display a dialog directing them to enable the permission in app settings.
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this).build().show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        logView = (TextView) findViewById(R.id.log_messages);

        locationToggleFab = (MyLocationFab) findViewById(R.id.fabLocationToggle);
        locationToggleFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mapboxMap != null) {
                    enableLocation(!mapboxMap.isMyLocationEnabled());
                }
            }
        });

        skyhookLocationEngine = SkyhookLocationEngine.getLocationEngine(this);

        mapView = (MapView) findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                MainActivity.this.mapboxMap = mapboxMap;
                if (PermissionsManager.areLocationPermissionsGranted(MainActivity.this)) {
                    Log.d(TAG, "Setting Skyhook location source");
                    mapboxMap.setLocationSource(skyhookLocationEngine);
                    mapboxMap.setMyLocationEnabled(locationToggleFab.isEnabled());
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
        skyhookLocationEngine.addLocationEngineListener(locationListenerForTextView);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();

        // remove listeners for UI controls
        skyhookLocationEngine.removeLocationEngineListener(locationListenerForTextView);
        if (locationListenerForPan != null) {
            skyhookLocationEngine.removeLocationEngineListener(locationListenerForPan);
        }

        // shutdown location engine (for demo purposes)
        mapboxMap.setMyLocationEnabled(false);
        skyhookLocationEngine.removeLocationUpdates();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @AfterPermissionGranted(RC_STORAGE_PERM)
    private void enableLogging() {
        invalidateOptionsMenu();
    }

    @AfterPermissionGranted(RC_LOCATION_PERM)
    private void enableLocation() {
        enableLocation(true);
    }

    @SuppressWarnings("MissingPermission")
    @UiThread
    private void enableLocation(boolean enabled) {
        if (enabled) {
            if (EasyPermissions.hasPermissions(this, LOCATION_PERMS)) {
                Location lastLocation = skyhookLocationEngine.getLastLocation();
                moveMapTo(lastLocation);
                locationToggleFab.enabled();
            } else {
                EasyPermissions.requestPermissions(this, getString(R.string.rationale_location),
                        RC_LOCATION_PERM, LOCATION_PERMS);
            }
        } else {
            locationToggleFab.disabled();
            skyhookLocationEngine.removeLocationUpdates();
        }
        mapboxMap.setMyLocationEnabled(enabled);
    }

    private LocationEngineListener getLocationListenerForTextView() {
        return new LocationEngineListener() {
            int counter = 0;
            @Override
            public void onConnected() { }

            @Override
            public void onLocationChanged(Location location) {
                logView.setText(String.format(getResources().getString(R.string.log_message),
                        counter++,
                        location.getLatitude(),
                        location.getLongitude(),
                        location.getTime()));
            }
        };
    }

    private void moveMapTo(Location lastLocation) {
        if (lastLocation != null) {
            if (mapboxMap.getCameraPosition().zoom > 15.99) {
                mapboxMap.easeCamera(CameraUpdateFactory.newLatLng(new LatLng(lastLocation)), 1000);
            } else {
                mapboxMap.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(new LatLng(lastLocation), 16), 1000);
            }
        } else {
            locationListenerForPan = new LocationEngineListener() {
                @Override
                public void onConnected() {
                    // Nothing
                }

                @Override
                public void onLocationChanged(Location location) {
                    if (location != null) {
                        mapboxMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                new LatLng(location), 16));
                        skyhookLocationEngine.removeLocationEngineListener(this);
                        locationListenerForPan = null;
                    }
                }
            };
            skyhookLocationEngine.addLocationEngineListener(locationListenerForPan);
        }
    }
}
