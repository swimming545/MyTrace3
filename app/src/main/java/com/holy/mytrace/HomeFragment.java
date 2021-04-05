package com.holy.mytrace;

import android.Manifest;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.holy.mytrace.adapters.WaypointAdapter;
import com.holy.mytrace.helpers.SQLiteHelper;
import com.holy.mytrace.models.Waypoint;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class HomeFragment extends Fragment implements OnMapReadyCallback {

    public static final int REQUEST_FINE_LOCATION = 100;
    public static final double THRESHOLD_WAYPOINT_METERS = 50;

    private GoogleMap googleMap;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    private boolean requestingLocationUpdates;

    private List<Waypoint> waypointList;
    private WaypointAdapter waypointAdapter;
    private RecyclerView waypointRecycler;
    private List<Marker> markerList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        if (getContext() == null) {
            return null;
        }

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        buildWaypointUI(view);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(getContext());
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }

                Location location = locationResult.getLastLocation();
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

                // 당일 waypoint 가 하나도 없으면 바로 추가
                if (waypointList.isEmpty()) {
                    addWaypoint(latLng);
                    return;
                }

                // 최초 카메라 이동
                if (locationResult.getLocations().size() == 1) {
                    CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 12.0f);
                    googleMap.moveCamera(cameraUpdate);
                }

                // TODO: 5분을 머물렀는지 판단해서 waypoint 추가
            }
        };

        locationRequest = LocationRequest.create();
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(2500);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // 날짜 UI 업데이트
        updateDateUI(view);

        return view;
    }

    private void addWaypoint(LatLng latLng) {

        Waypoint waypoint = new Waypoint(latLng.latitude, latLng.longitude,
                LocalDateTime.now(), LocalDateTime.now());

        SQLiteHelper.getInstance(getContext()).addWaypoint(waypoint);
        updateWaypointUI();

        if (googleMap != null) {
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 12.0f);
            googleMap.moveCamera(cameraUpdate);
        }
    }

    private void buildWaypointUI(View v) {

        waypointRecycler = v.findViewById(R.id.recyclerWaypoint);
        waypointRecycler.setHasFixedSize(true);
        waypointRecycler.setLayoutManager(new LinearLayoutManager(getContext()));

        waypointList = new ArrayList<>();
        waypointAdapter = new WaypointAdapter(waypointList);
        waypointRecycler.setAdapter(waypointAdapter);

        markerList = new ArrayList<>();
    }

    private void updateWaypointUI() {

        // 리사이클러뷰 업데이트
        LocalDate date = LocalDate.now();

        waypointList.clear();
        waypointList.addAll(SQLiteHelper.getInstance(getContext())
                .getWaypointsByDate(date.getYear(), date.getMonthValue(), date.getDayOfMonth())
        );

        waypointAdapter.notifyDataSetChanged();

        // 마커 업데이트
        for (Marker marker : markerList) {
            marker.remove();
        }
        markerList.clear();

        for (Waypoint waypoint : waypointList) {
            LatLng latLng = new LatLng(waypoint.getLatitude(), waypoint.getLongitude());
            BitmapDescriptor bd = BitmapDescriptorFactory.fromResource(R.drawable.pin);
            Marker marker = googleMap.addMarker(new MarkerOptions().position(latLng).icon(bd));
            markerList.add(marker);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (requestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_FINE_LOCATION) {
            // 위치 퍼미션 결과 확인
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 퍼미션 승인 시
                // 위치 업데이트 시작
                startLocationUpdates();
            } else {
                // 퍼미션 거부 시
                // 토스트 출력
                Toast.makeText(getContext(),
                        "위치 퍼미션이 없으면 현재 위치를 확인할 수 없습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        this.googleMap = googleMap;

        updateWaypointUI();

        // 현재 위치 확보

        // - 먼저 위치 퍼미션 확인
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // 퍼미션 없을 시, 퍼미션 요청하기
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_FINE_LOCATION);
            return;
        }

        // - 위치 업데이트 시작
        startLocationUpdates();
    }

    private void startLocationUpdates() {

        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        requestingLocationUpdates = true;
        fusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper());
    }

    private void stopLocationUpdates() {

        requestingLocationUpdates = false;
        fusedLocationClient.removeLocationUpdates(locationCallback);
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