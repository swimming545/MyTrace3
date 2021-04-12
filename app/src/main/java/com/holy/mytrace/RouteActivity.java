package com.holy.mytrace;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.holy.mytrace.helpers.GoogleDirectionApiHelper;

import java.util.List;

public class RouteActivity extends AppCompatActivity implements
        OnMapReadyCallback, GoogleDirectionApiHelper.OnDirectionDataReadyListener {

    public static final String EXTRA_ORIGIN_LATITUDE = "com.holy.mytrace.origin_latitude";
    public static final String EXTRA_ORIGIN_LONGITUDE = "com.holy.mytrace.origin_longitude";
    public static final String EXTRA_DESTINATION_LATITUDE = "com.holy.mytrace.destination_latitude";
    public static final String EXTRA_DESTINATION_LONGITUDE = "com.holy.mytrace.destination_longitude";

    private GoogleMap mGoogleMap;
    private FusedLocationProviderClient mFusedLocationClient;

    private List<LatLng> mEndPointList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // 구글맵 초기화
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // 경로 로딩 시작
        double latOrigin = getIntent().getDoubleExtra(EXTRA_ORIGIN_LATITUDE, 0);
        double lonOrigin = getIntent().getDoubleExtra(EXTRA_ORIGIN_LONGITUDE, 0);
        double latDest = getIntent().getDoubleExtra(EXTRA_DESTINATION_LATITUDE, 0);
        double lonDest = getIntent().getDoubleExtra(EXTRA_DESTINATION_LONGITUDE, 0);
        LatLng origin = new LatLng(latOrigin, lonOrigin);
        LatLng dest = new LatLng(latDest, lonDest);

        new GoogleDirectionApiHelper(this).loadDirectionData(this, origin, dest);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mGoogleMap = googleMap;

        if (mEndPointList != null) {
            updateRouteUIs();
        }
    }

    @Override
    public void onDirectionDataReady(double distance, List<LatLng> endPointList) {

        if (endPointList == null) {
            return;
        }

        mEndPointList = endPointList;

        if (mGoogleMap != null) {
            updateRouteUIs();
        }
    }

    private void updateRouteUIs() {

        if (mGoogleMap == null || mEndPointList == null) {
            return;
        }

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationClient.getLastLocation().addOnSuccessListener(location -> {

                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

                mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15.0f));
                mGoogleMap.addMarker(new MarkerOptions().position(latLng));
                mGoogleMap.addPolyline(new PolylineOptions().add(latLng));
                for (LatLng endPoint : mEndPointList) {
                    mGoogleMap.addPolyline(new PolylineOptions().add(endPoint));
                }
            });
        }
    }

}