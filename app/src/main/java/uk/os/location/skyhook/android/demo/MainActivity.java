package uk.os.location.skyhook.android.demo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.services.android.telemetry.location.LocationEngine;
import com.mapbox.services.android.telemetry.location.LocationEngineListener;

import uk.os.location.skyhook.android.demo.location.SkyhookLocationEngine;

public class MainActivity extends AppCompatActivity {

    // Mapbox SDK
    private MapView mapView;
    private MapboxMap mapboxMap;

    // Permission callback (e.g. for location)
    private static final int PERMISSIONS_LOCATION = 0;

    // Custom location source
    private LocationEngine skyhookLocationEngine;

    // Log view
    TextView logView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        logView = (TextView) findViewById(R.id.log_messages);

        skyhookLocationEngine = SkyhookLocationEngine.getLocationEngine(this);

        mapView = (MapView) findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                if (isGrantedLocationPermission()) {
                    mapboxMap.setLocationSource(skyhookLocationEngine);
                    mapboxMap.setMyLocationEnabled(true);
                    MainActivity.this.mapboxMap = mapboxMap;
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
    public void onResume() {
        super.onResume();
        mapView.onResume();

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_LOCATION);
            return;
        }
        skyhookLocationEngine.addLocationEngineListener(locationListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
        skyhookLocationEngine.removeLocationEngineListener(locationListener);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                doLocationLookups();
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
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

    @SuppressWarnings("MissingPermission")
    public void onRequestLocation(View view) {
        if (mapboxMap != null) {
            mapboxMap.setMyLocationEnabled(!mapboxMap.isMyLocationEnabled());

            boolean isMyLocationEnabled = mapboxMap.isMyLocationEnabled();
            if (isMyLocationEnabled && isGrantedLocationPermission()) {
                Toast.makeText(this, "Location enabled", Toast.LENGTH_SHORT).show();
                skyhookLocationEngine.requestLocationUpdates();
            } else {
                Toast.makeText(this, "Location disabled", Toast.LENGTH_SHORT).show();
                skyhookLocationEngine.removeLocationUpdates();
            }
        }
    }

    private boolean isGrantedLocationPermission() {
        return ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressWarnings("MissingPermission")
    private void doLocationLookups() {
        skyhookLocationEngine.requestLocationUpdates();
    }

    LocationEngineListener locationListener = new LocationEngineListener() {
        int counter = 0;
        @Override
        public void onConnected() {

        }

        @Override
        public void onLocationChanged(Location location) {
            logView.setText(String.format("%d) %f %f - %d", counter++, location.getLatitude(), location.getLongitude(), location.getTime()));
        }
    };
}
