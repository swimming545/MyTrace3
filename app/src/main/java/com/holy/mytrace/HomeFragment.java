package com.holy.mytrace;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.holy.mytrace.adapters.WaypointAdapter;
import com.holy.mytrace.helpers.SQLiteHelper;
import com.holy.mytrace.models.Waypoint;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class HomeFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mGoogleMap;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;
    private LocationRequest mLocationRequest;
    private Location mCurrentLocation;

    private List<Marker> mMarkerList;
    private Marker mCurrentMarker;


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(context);

        mLocationRequest = LocationRequest.create();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {

                Location location = locationResult.getLastLocation();

                if (mGoogleMap != null && mCurrentLocation == null) {
                    updateLocationUIs(location, false);
                }

                mCurrentLocation = location;
            }
        };
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Waypoint UI 업데이트
        updateWaypointUIs();

        // 날짜 UI 업데이트
        updateDateUI(view);

        return view;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getActivity() == null || getContext() == null) {
            return;
        }

        // 구글맵 초기화
        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // 현위치로 카메라 이동
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                mCurrentLocation = location;
                if (mGoogleMap != null) {
                   updateLocationUIs(location, true);
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Waypoint UI 업데이트
        updateWaypointUIs();

        // 위치 업데이트 시작
        if (getContext() != null &&
                ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.getMainLooper());
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        // 위치 업데이트 종료
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }

    // 현위치 UI 업데이트

    private void updateLocationUIs(Location location, boolean moveCamera) {

        if (mCurrentMarker != null) {
            mCurrentMarker.remove();
        }

        // 현위치 마커 추가
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        mCurrentMarker = mGoogleMap.addMarker(new MarkerOptions().position(latLng));

        if (mGoogleMap != null && moveCamera) {
            if (getContext() != null &&
                    ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {
                mFusedLocationClient.getLastLocation().addOnSuccessListener(lastLocation -> {
                    LatLng lastLatLng = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                    mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastLatLng, 15.0f));
                    Log.d("TAG", "moveCamera: ");
                });
            }
        }
    }

    public void updateLocationUIs(boolean moveCamera) {

        if (mCurrentLocation != null) {
            updateLocationUIs(mCurrentLocation, moveCamera);
        }
    }

    // Waypoint UI 업데이트

    public void updateWaypointUIs() {

        // 오늘 Waypoint 모두 획득
        LocalDate today = LocalDate.now();
        List<Waypoint> waypointList = SQLiteHelper.getInstance(getContext())
                .getWaypointsByDate(today.getYear(), today.getMonthValue(), today.getDayOfMonth());

        // Waypoint 리사이클러뷰 업데이트
        buildWaypointRecycler(waypointList);

        if (mGoogleMap != null) {
            // Waypoint 마커 업데이트
            buildWaypointMarkers(waypointList);
        }
    }

    // Waypoint 리사이클러뷰 업데이트

    private void buildWaypointRecycler(List<Waypoint> waypointList) {

        View view = getView();
        if (view == null) {
            return;
        }

        RecyclerView recyclerView = view.findViewById(R.id.recyclerWaypoint);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(new WaypointAdapter(waypointList));
    }

    // Waypoint 마커 업데이트

    private void buildWaypointMarkers(List<Waypoint> waypointList) {

        // 기존의 마커 모두 지우기
        if (mMarkerList != null) {
            for (Marker marker : mMarkerList) {
                marker.remove();
            }
        }

        mMarkerList = new ArrayList<>();

        // 새로운 마커 생성하기
        for (Waypoint wayPoint : waypointList) {
            MarkerOptions options = new MarkerOptions()
                    .position(new LatLng(wayPoint.getLatitude(), wayPoint.getLongitude()))
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.pin))
                    .title(wayPoint.getName());
            Marker newMarker = mGoogleMap.addMarker(options);
            mMarkerList.add(newMarker);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mGoogleMap = googleMap;
        googleMap.getUiSettings().setZoomControlsEnabled(true);

        if (mCurrentLocation != null) {
            updateLocationUIs(mCurrentLocation, true);
        }

        if (mMarkerList == null) {
            updateWaypointUIs();
        }
    }

    private void updateDateUI(View view) {

        // 현재 날짜로부터 문자열 구성
        LocalDate date = LocalDate.now();
        int year = date.getYear();
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        String strDate = String.format(Locale.getDefault(),
                "%d년 %02d월 %02d일", year, month, day);

        // 날짜 UI 업데이트
        TextView dateText = view.findViewById(R.id.txtDate);
        dateText.setText(strDate);
    }



}