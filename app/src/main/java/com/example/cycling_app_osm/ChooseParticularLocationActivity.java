package com.example.cycling_app_osm;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.PreferenceManager;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChooseParticularLocationActivity extends AppCompatActivity {
    private MapView map;
    private Marker marker;
    private GeoPoint selectedPoint;
    public static final MediaType MEDIA_TYPE_MARKDOWN
            = MediaType.parse("text/plain");

    private final OkHttpClient client = new OkHttpClient();

    private final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context mContext = getApplicationContext();
        Configuration.getInstance().load(mContext,
                PreferenceManager.getDefaultSharedPreferences(mContext));
        setContentView(R.layout.particular_location);
        Button confirm_choice_btn = findViewById(R.id.confirm_choice_button);

        map = findViewById(R.id.mapLocation);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setBuiltInZoomControls(true);

        requestPermissionsIfNecessary(new String[]{
                // if you need to show the current location, uncomment the line below
                Manifest.permission.ACCESS_FINE_LOCATION,
                // WRITE_EXTERNAL_STORAGE is required in order to show the map
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        });
        MyLocationNewOverlay mMyLocationOverlay;
        IMapController mapController = map.getController();
        map.setMultiTouchControls(true);

        int tripChosen = getIntent().getIntExtra("tripChosen", -1);
        int chooseCurrentLocation = getIntent().getIntExtra("chooseCurrentLocation", -1);


        mMyLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(mContext), map);
        mMyLocationOverlay.enableMyLocation();

        map.getOverlays().add(mMyLocationOverlay);

        mMyLocationOverlay.runOnFirstFix(new Runnable() {
            @Override
            public void run() {
                Location lastKnownLocation = mMyLocationOverlay.getLastFix();
                if (lastKnownLocation != null) {
                    double latitude = lastKnownLocation.getLatitude();
                    double longitude = lastKnownLocation.getLongitude();
                    GeoPoint startPoint = new GeoPoint(latitude, longitude);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            map.getController().setCenter(startPoint);
                        }
                    });
                } else {
                    Toast.makeText(ChooseParticularLocationActivity.this, "Location not available", Toast.LENGTH_SHORT).show();
                }
            }
        });

        mapController.setZoom(18.0);

        MapEventsReceiver mapEventsReceiver = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                return false;
            }

            @Override
            public boolean longPressHelper(GeoPoint point) {
                if (marker != null) {
                    map.getOverlayManager().remove(marker);
                    map.invalidate();
                }
                selectedPoint = point;
                // Add marker to indicate the long-pressed location
                marker = new Marker(map);
                marker.setPosition(point);
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                map.getOverlayManager().add(marker);

                // Refresh the map to display the marker
                map.invalidate();

                return true;
            }
        };

        MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(mapEventsReceiver);
        map.getOverlays().add(0, mapEventsOverlay);


        confirm_choice_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (selectedPoint != null) {
                    double latitude = selectedPoint.getLatitude();
                    double longitude = selectedPoint.getLongitude();

                    startActivity(new Intent(ChooseParticularLocationActivity.this, MainActivity.class)
                            .putExtra("chooseCurrentLocation", chooseCurrentLocation)
                            .putExtra("tripChosen", tripChosen)
                            .putExtra("latitude",latitude )
                            .putExtra("longitude", longitude));
                }
            }
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (int i = 0; i < grantResults.length; i++) {
            permissionsToRequest.add(permissions[i]);
        }
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    private void requestPermissionsIfNecessary(String[] permissions) {
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted
                permissionsToRequest.add(permission);
            }
        }
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }
}
