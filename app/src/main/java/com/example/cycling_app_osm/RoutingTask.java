package com.example.cycling_app_osm;
import android.graphics.Color;
import android.os.AsyncTask;
import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Polyline;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class RoutingTask extends AsyncTask<Void, Void, List<GeoPoint>> {
    private GeoPoint startPoint;
    private GeoPoint endPoint;
    private MapView map;

    public RoutingTask(GeoPoint startPoint, GeoPoint endPoint, MapView map) {
        this.startPoint = startPoint;
        this.endPoint = endPoint;
        this.map = map;
    }

    @Override
    protected List<GeoPoint> doInBackground(Void... voids) {
        try {
            String urlString = String.format(
                    "https://graphhopper.com/api/1/route?point=%f,%f&point=%f,%f&vehicle=foot&key=9e459ab9-5a90-4e86-8343-20864e41e10f",
                    startPoint.getLatitude(), startPoint.getLongitude(),
                    endPoint.getLatitude(), endPoint.getLongitude());
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            JSONObject jsonResponse = new JSONObject(response.toString());
            JSONArray paths = jsonResponse.getJSONArray("paths");
            JSONObject path = paths.getJSONObject(0);
            JSONArray points = path.getJSONObject("points").getJSONArray("coordinates");

            List<GeoPoint> geoPoints = new ArrayList<>();
            for (int i = 0; i < points.length(); i++) {
                JSONArray point = points.getJSONArray(i);
                geoPoints.add(new GeoPoint(point.getDouble(1), point.getDouble(0)));
            }

            return geoPoints;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void onPostExecute(List<GeoPoint> geoPoints) {
        if (geoPoints != null) {
            Polyline polyline = new Polyline();
            polyline.setPoints(geoPoints);
            polyline.setWidth(5);
            polyline.setColor(Color.RED);

            map.getOverlayManager().add(polyline);
            map.invalidate();
        }
    }
}