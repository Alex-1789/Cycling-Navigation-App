package com.example.cycling_app_osm;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.Manifest;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.ItemizedOverlay;
import org.osmdroid.views.overlay.ItemizedOverlayWithFocus;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private MapView map;
    public static final MediaType MEDIA_TYPE_MARKDOWN
            = MediaType.parse("text/plain");

    private final OkHttpClient client = new OkHttpClient();

    private final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private List<Marker> markers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context mContext = getApplicationContext();
        Configuration.getInstance().load(mContext,
                PreferenceManager.getDefaultSharedPreferences(mContext));
        setContentView(R.layout.activity_main);

        map = findViewById(R.id.map);


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
        //mMyLocationOverlay.enableFollowLocation();
        //mMyLocationOverlay.disableFollowLocation();
        map.getOverlays().add(mMyLocationOverlay);

        mMyLocationOverlay.runOnFirstFix(new Runnable() {
            @Override
            public void run() {
                double latitude = -1;
                double longitude = -1;
                if (chooseCurrentLocation == 0) {
                    Location lastKnownLocation = mMyLocationOverlay.getLastFix();
                    latitude = lastKnownLocation.getLatitude();
                    longitude = lastKnownLocation.getLongitude();
                } else if (chooseCurrentLocation == 1) {
                    latitude = getIntent().getDoubleExtra("latitude", -1);
                    longitude = getIntent().getDoubleExtra("longitude", -1);
                }
                else {
                    return;
                }

                GeoPoint startPoint = new GeoPoint(latitude, longitude);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        map.getController().setCenter(startPoint);
                    }
                });

                // Run heavy task in a separate thread
                double finalLatitude = latitude;
                double finalLongitude = longitude;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // Call fetchData method in a separate thread
                            fetchData(finalLatitude, finalLongitude, tripChosen);
                            makeWayAlgorithm_New(finalLatitude, finalLongitude);
                        } catch (Exception e) {
                            e.printStackTrace();
                            // Handle exceptions if necessary
                        }
                    }
                }).start();
            }
        });

        mapController.setZoom(18.0);


    }

    @Override
    public void onPause() {

        super.onPause();
        map.onPause();
    }

    @Override
    public void onResume() {

        super.onResume();
        map.onResume();
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

    public void fetchData(double latitude, double longitude, int tripChosen) throws Exception {
        String postBody = "";

        if (tripChosen == 0) {
            postBody =
                    "(" +
                            "  node[\"tourism\"=\"viewpoint\"](around:10000, " + latitude + ", " + longitude + ");\n" +
                           /* "  way[\"tourism\"=\"viewpoint\"](around:10000, " + latitude + ", " + longitude + ");\n" +
                            "  relation[\"tourism\"=\"viewpoint\"](around:10000, " + latitude + ", " + longitude + ");\n" +*/

                            "  node[\"leisure\"=\"park\"](around:10000, " + latitude + ", " + longitude + ");\n" +
                           /* "  way[\"leisure\"=\"park\"](around:10000, " + latitude + ", " + longitude + ");\n" +
                            "  relation[\"leisure\"=\"park\"](around:10000, " + latitude + ", " + longitude + ");\n" +*/

                            "  node[\"historic\"=\"ruins\"](around:10000, " + latitude + ", " + longitude + ");\n" +
                          /*  "  way[\"historic\"=\"ruins\"](around:10000, " + latitude + ", " + longitude + ");\n" +
                            "  relation[\"historic\"=\"ruins\"](around:10000, " + latitude + ", " + longitude + ");\n" +*/
                            ");\n" +
                            "out center;";

        } else if (tripChosen == 1) {
            postBody =
                    "(" +
                            "  node[\"tourism\"=\"museum\"](around:10000, " + latitude + ", " + longitude + ");\n" +
                           /* "  way[\"tourism\"=\"museum\"](around:10000, " + latitude + ", " + longitude + ");\n" +
                            "  relation[\"tourism\"=\"museum\"](around:10000, " + latitude + ", " + longitude + ");\n" +*/

                            "  node[\"tourism\"=\"artwork\"](around:10000, " + latitude + ", " + longitude + ");\n" +
                           /* "  way[\"tourism\"=\"artwork\"](around:10000, " + latitude + ", " + longitude + ");\n" +
                            "  relation[\"tourism\"=\"artwork\"](around:10000, " + latitude + ", " + longitude + ");\n" +*/

                            "  node[\"amenity\"=\"gallery\"](around:10000, " + latitude + ", " + longitude + ");\n" +
                          /*  "  way[\"amenity\"=\"gallery\"](around:10000, " + latitude + ", " + longitude + ");\n" +
                            "  relation[\"amenity\"=\"gallery\"](around:10000, " + latitude + ", " + longitude + ");\n" +*/
                            ");\n" +
                            "out center;";

        } else {
            throw new Exception("Wrong type of trip");
        }

        Request request = new Request.Builder()
                .url("https://overpass-api.de/api/interpreter")
                .post(RequestBody.create(MEDIA_TYPE_MARKDOWN, postBody))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

           // System.out.println(response.body().string());

            String responseBody = response.body().string();
            // Parse the response and extract coordinates of points (restaurants)
            List<Marker> restaurantMarkers = parseResponse(responseBody);
            markers = restaurantMarkers;
            // Add markers for each restaurant point on the map
            for (Marker marker : restaurantMarkers) {
                map.getOverlays().add(marker);
            }
        }
    }

    private List<Marker> parseResponse(String responseBody) {
        List<Marker> markers = new ArrayList<>();
        GeoPoint point = null;
        // Split the response by lines
        String[] lines = responseBody.split("\n");

        // Iterate over each line
        for (String line : lines) {
            // Check if the line contains a <node> element
            if (line.contains("<node")) {
                // Extract latitude and longitude from the line
                String[] parts = line.split("\"");
                double latitude = Double.parseDouble(parts[3]);
                double longitude = Double.parseDouble(parts[5]);

                // Create a GeoPoint object with extracted coordinates
                point = new GeoPoint(latitude, longitude);

            }
            else if(line.contains("<tag k=\"name\"")){
                Marker marker = new Marker(map);

                int startIndex = line.indexOf("v=\"") + 3;
                int endIndex = line.lastIndexOf("\"");

                marker.setTitle(line.substring(startIndex, endIndex));
                marker.setPosition(point);
                markers.add(marker);
            }
            /*else if (line.contains("<center")) {
                // Extract latitude and longitude from the <center> tag within <way>
                String[] parts = line.split("\"");
                double latitude = Double.parseDouble(parts[3]);
                double longitude = Double.parseDouble(parts[5]);

                // Create a GeoPoint object with extracted coordinates
                GeoPoint point = new GeoPoint(latitude, longitude);
                points.add(point);
            }*/
        }

        return markers;
    }

    public static Object[][] quickSort(Object[][] matrix, int low, int high) {
        if (low < high) {
            Object[] result = partition(matrix, low, high);
            int pi = (int) result[0];
            matrix = (Object[][]) result[1];

            matrix = quickSort(matrix, low, pi - 1);
            matrix = quickSort(matrix, pi + 1, high);
        }
        return matrix;
    }

    public static Object[] partition(Object[][] matrix, int low, int high) {
        double pivot = (double) matrix[1][high];
        int i = low - 1;

        for (int j = low; j < high; j++) {
            if ((double) matrix[1][j] < pivot) {
                i++;
                matrix = swap(matrix, i, j);
            }
        }

        matrix = swap(matrix, i + 1, high);
        return new Object[]{i + 1, matrix};
    }

    public static Object[][] swap(Object[][] matrix, int i, int j) {
        Marker temp_0 = (Marker) matrix[0][i];
        double temp_1 = (double) matrix[1][i];
        boolean temp_2 = (boolean) matrix[2][i];
        matrix[0][i] = matrix[0][j];
        matrix[1][i] = matrix[1][j];
        matrix[2][i] = matrix[2][j];
        matrix[0][j] = temp_0;
        matrix[1][j] = temp_1;
        matrix[2][j] = temp_2;

        return matrix;
    }

    private void makeWayAlgorithm(double latitude, double longitude){
        Object [][] matrix = new Object[3][markers.size()];

        double wholeWay = 0;
        int howManyPoints = 0;
        double original_latitude = latitude;
        double original_longitude = longitude;


        for(int i = 0; i < markers.size(); i++){
            matrix[0][i] = markers.get(i);
            matrix[2][i] = false;
        }

        Marker[] temp = new Marker [12];
        if(markers.isEmpty()){
            return;
        }

        while(howManyPoints < 12 && wholeWay <= 12) {
            for (int i = 0; i < markers.size(); i++) {
                Marker point = (Marker) matrix[0][i];
                matrix[1][i] = calculateDistance(latitude, longitude,
                        point.getPosition().getLatitude(),
                        point.getPosition().getLongitude());
            }
            int x = 0;

            matrix = quickSort(matrix, 0, matrix[0].length - 1);

            while((boolean) matrix[2][x] == true){
                x++;
            }

            temp[howManyPoints] = (Marker) matrix[0][x];
            howManyPoints++;
            wholeWay += (double) matrix[1][x];
            matrix[2][x] = true;
            Marker point = (Marker) matrix[0][x];

            latitude = point.getPosition().getLatitude();
            longitude = point.getPosition().getLongitude();
        }

        if(howManyPoints < 3 || wholeWay > 15){
            return;
        }


        Marker [] points = new Marker [howManyPoints];

        for(int  i = 0; i < howManyPoints; i++) {
            points[i] = temp[i];
        }

        List<GeoPoint> geoPoints = new ArrayList<>();
        geoPoints.add(new GeoPoint(original_latitude,original_longitude));
        for(int i = 0; i < howManyPoints; i++) {
            geoPoints.add(points[i].getPosition());
        }

        Polyline polyline = new Polyline();
        polyline.setPoints(geoPoints);
        polyline.setWidth(5); // Ustawienie szerokości linii
        polyline.setColor(Color.RED); // Ustawienie koloru linii

        map.getOverlayManager().add(polyline);
        map.invalidate(); // Odświeżenie mapy, aby wyświetlić zmiany

    }

    private List<GeoPoint> parseResponseMap(String responseBody) {
        List<GeoPoint> geoPoints = new ArrayList<>();

        try {
            JSONObject response = new JSONObject(responseBody);
            JSONArray paths = response.getJSONArray("paths");
            if (paths.length() > 0) {
                JSONObject path = paths.getJSONObject(0);
                JSONObject points = path.getJSONObject("points");
                JSONArray coordinates = points.getJSONArray("coordinates");

                for (int i = 0; i < coordinates.length(); i++) {
                    JSONArray point = coordinates.getJSONArray(i);
                    double longitude = point.getDouble(0);
                    double latitude = point.getDouble(1);
                    geoPoints.add(new GeoPoint(latitude, longitude));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return geoPoints;
    }

    private void makeWayAlgorithm_New(double latitude, double longitude) throws Exception {
        Object [][] matrix = new Object[3][markers.size()];

        double wholeWay = 0;
        int howManyPoints = 0;
        double original_latitude = latitude;
        double original_longitude = longitude;


        for(int i = 0; i < markers.size(); i++){
            matrix[0][i] = markers.get(i);
            matrix[2][i] = false;
        }

        Marker[] temp = new Marker [12];
        if(markers.isEmpty()){
            return;
        }

        while(howManyPoints < 12 && wholeWay <= 12) {
            for (int i = 0; i < markers.size(); i++) {
                Marker point = (Marker) matrix[0][i];
                matrix[1][i] = calculateDistance(latitude, longitude,
                        point.getPosition().getLatitude(),
                        point.getPosition().getLongitude());
            }
            int x = 0;

            matrix = quickSort(matrix, 0, matrix[0].length - 1);

            while((boolean) matrix[2][x] == true){
                x++;
            }

            temp[howManyPoints] = (Marker) matrix[0][x];
            howManyPoints++;
            wholeWay += (double) matrix[1][x];
            matrix[2][x] = true;
            Marker point = (Marker) matrix[0][x];

            latitude = point.getPosition().getLatitude();
            longitude = point.getPosition().getLongitude();
        }

        if(howManyPoints < 3 || wholeWay > 15){
            return;
        }


        Marker [] points = new Marker [howManyPoints];

        for(int  i = 0; i < howManyPoints; i++) {
            points[i] = temp[i];
        }

        List<GeoPoint> geoPoints = new ArrayList<>();
        geoPoints.add(new GeoPoint(original_latitude,original_longitude));
        for(int i = 0; i < howManyPoints; i++) {
            geoPoints.add(points[i].getPosition());
        }

        geoPoints = fetchRoad(geoPoints);
        Polyline polyline = new Polyline();
        polyline.setPoints(geoPoints);
        polyline.setWidth(10); // Ustawienie szerokości linii
        polyline.setColor(Color.BLUE); // Ustawienie koloru linii

        map.getOverlayManager().add(polyline);
        map.invalidate(); // Odświeżenie mapy, aby wyświetlić zmiany

    }

    public List<GeoPoint> fetchRoad(List<GeoPoint> geoPoints) throws Exception {
        StringBuilder url = new StringBuilder();
        url.append("https://graphhopper.com/api/1/route?");
        for (GeoPoint point : geoPoints) {
            url.append("point=").append(point.getLatitude()).append(",")
                    .append(point.getLongitude()).append("&");
        }
        url.append("profile=bike&locale=en&calc_points=true&points_encoded=false&key=9e459ab9-5a90-4e86-8343-20864e41e10f");

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url.toString())
                .get()
                .build();

        List<GeoPoint> roadWay = null;
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            String responseBody = response.body().string();
            // Parse the response and extract coordinates of points (restaurants)
             roadWay = parseResponseMap(responseBody);

        }
        return roadWay;
    }



    static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // Promień Ziemi w kilometrach
        final double R = 6371.0;

        // Konwersja stopni na radiany
        double lat1Rad = Math.toRadians(lat1);
        double lon1Rad = Math.toRadians(lon1);
        double lat2Rad = Math.toRadians(lat2);
        double lon2Rad = Math.toRadians(lon2);

        // Różnice długości i szerokości geograficznych
        double dLat = lat2Rad - lat1Rad;
        double dLon = lon2Rad - lon1Rad;

        // Obliczenia odległości przy użyciu wzoru Haversine
        double a = Math.pow(Math.sin(dLat / 2), 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                        Math.pow(Math.sin(dLon / 2), 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c;

        return distance;
    }
}




  /*public void fetchMuseums(double latitude, double longitude) {
        String query = constructOverpassQuery(latitude, longitude);
        String urlString = "https://overpass-api.de/api/interpreter?data=" + query;

        try {
            URL url = new URL(urlString);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            try {
                InputStream in = urlConnection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                String jsonResponse = response.toString();
                // Process the JSON response here
                processJsonResponse(jsonResponse);
            } finally {
                urlConnection.disconnect();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }*/

//ArrayList<OverlayItem> items = new ArrayList<>();
//OverlayItem home = new OverlayItem("Rallo's office", "my office", new GeoPoint(52.2296, 21.0120));
//Drawable m = home.getMarker(0);
// items.add(home);
//items.add(new OverlayItem("Resto", "cher babar", new GeoPoint(43.65950, 7.00517)));

       /* ItemizedOverlayWithFocus<OverlayItem> mOverlay = new ItemizedOverlayWithFocus<OverlayItem>(getApplicationContext(),
                items, new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
            @Override
            public boolean onItemSingleTapUp(int index, OverlayItem item) {
                return true;
            }

            @Override
            public boolean onItemLongPress(int index, OverlayItem item) {
                return false;
            }
        });*/

//  mOverlay.setFocusItemsOnTap(true);
//  map.getOverlays().add(mOverlay);
// map.getOverlays().add(mMyLocationOverlay);
// EdgeToEdge.enable(this);
//
       /* ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });*/

/*
private void makeWay(double latitude, double longitude){
        Object [][] matrix = new Object[2][markers.size()];

        double wholeWay = 0;
        int howManyPoints = 0;

        for(int i = 0; i < markers.size(); i++){
            matrix[0][i] = markers.get(i);
            matrix[1][i] = calculateDistance(latitude, longitude,
                    markers.get(i).getPosition().getLatitude(),
                    markers.get(i).getPosition().getLongitude());
        }

        matrix = quickSort(matrix, 0, matrix[0].length - 1);

        for(int i = 0; i < markers.size(); i++) {
            if(wholeWay < 12 || howManyPoints < 12) {
                wholeWay += (double) matrix[1][i];
                howManyPoints++;
                if(wholeWay > 15 || howManyPoints > 12) {
                    wholeWay -= (double) matrix[1][i];
                    howManyPoints--;
                }
            }
        }

        Marker [] points = new Marker [howManyPoints];

        for(int i = 0; i < howManyPoints; i++) {
            points[i] = (Marker) matrix[0][i];
        }

        List<GeoPoint> geoPoints = new ArrayList<>();
        geoPoints.add(new GeoPoint(latitude,longitude));
        for(int i = 0; i < howManyPoints; i++) {
            geoPoints.add(points[i].getPosition());
        }

        Polyline polyline = new Polyline();
        polyline.setPoints(geoPoints);
        polyline.setWidth(5); // Ustawienie szerokości linii
        polyline.setColor(Color.RED); // Ustawienie koloru linii

        map.getOverlayManager().add(polyline);
        map.invalidate(); // Odświeżenie mapy, aby wyświetlić zmiany
    }
 */