package com.holy.mytrace.helpers;

import android.content.Context;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GoogleDirectionApiHelper {

    // URL of API
    public static final String URL_FORMAT = "https://maps.googleapis.com/maps/api/directions/json?origin=[origin]&destination=[destination]&key=[key]";
    public static final String REQUEST_PLACEHOLDER_ORIGIN = "[origin]";
    public static final String REQUEST_PLACEHOLDER_DESTINATION ="[destination]";
    public static final String REQUEST_PLACEHOLDER_API_KEY = "[key]";
    public static final String API_KEY = "AIzaSyD8LVvug6itzvlonFm-ObXwiIwGRyCedWY";

    public static final String KEY_ROUTES = "routes";
    public static final String KEY_LEGS = "legs";
    public static final String KEY_DISTANCE = "distance";
    public static final String KEY_DISTANCE_VALUE = "value";
    public static final String KEY_STEPS = "steps";
    public static final String KEY_END_LOCATION = "end_location";
    public static final String KEY_LATITUDE = "lat";
    public static final String KEY_LONGITUDE = "lng";


    // Callback when called loading is finished
    public interface OnDirectionDataReadyListener {
        void onDirectionDataReady(double distance, List<LatLng> endPointList);
    }

    // Request queue
    private final RequestQueue requestQueue;


    public GoogleDirectionApiHelper(Context context) {
        requestQueue = Volley.newRequestQueue(context);
    }

    public void loadDirectionData(OnDirectionDataReadyListener callback,
                                  LatLng origin, LatLng destination) {

        String strOrigin = String.format(Locale.getDefault(),
                "%f,%f", origin.latitude, origin.longitude);
        String strDestination = String.format(Locale.getDefault(),
                "%f,%f", destination.latitude, destination.longitude);

        String url = URL_FORMAT
                .replace(REQUEST_PLACEHOLDER_ORIGIN, strOrigin)
                .replace(REQUEST_PLACEHOLDER_DESTINATION, strDestination)
                .replace(REQUEST_PLACEHOLDER_API_KEY, API_KEY);

        Log.d("TAG", "loadDirectionData: " + url);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET, url,
                null,
                response -> {
                    try {
                        int distanceInMeters;
                        List<LatLng> endPointList = new ArrayList<>();

                        // Get "routes" object
                        JSONArray routes = response.getJSONArray(KEY_ROUTES);

                        // Get route object (the first element of "routes" array)
                        JSONObject route = routes.getJSONObject(0);

                        // Get "legs" array
                        JSONArray legs = route.getJSONArray(KEY_LEGS);

                        // Get leg object (the first element of "legs" array)
                        JSONObject leg = legs.getJSONObject(0);

                        // Get distance value
                        JSONObject distance = leg.getJSONObject(KEY_DISTANCE);
                        distanceInMeters = distance.getInt(KEY_DISTANCE_VALUE);

                        // Get "steps" array
                        JSONArray steps = leg.getJSONArray(KEY_STEPS);

                        // Parse each step object and extract the end point
                        for (int i = 0; i < steps.length(); i++) {

                            JSONObject step = steps.getJSONObject(i);
                            JSONObject endLocation = step.getJSONObject(KEY_END_LOCATION);

                            // Get latitude and longitude of the end point
                            double latitude = endLocation.getDouble(KEY_LATITUDE);
                            double longitude = endLocation.getDouble(KEY_LONGITUDE);

                            // Add the end point into list
                            LatLng endPoint = new LatLng(latitude, longitude);
                            endPointList.add(endPoint);
                        }

                        // Loading is finished, invoke the callback
                        callback.onDirectionDataReady(distanceInMeters, endPointList);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                Throwable::printStackTrace);

        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                20000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        requestQueue.add(jsonObjectRequest);
    }

}
