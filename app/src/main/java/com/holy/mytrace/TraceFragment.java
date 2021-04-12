package com.holy.mytrace;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
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
import android.widget.ImageButton;
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
import com.google.android.gms.maps.model.PolylineOptions;
import com.holy.mytrace.adapters.WaypointAdapter;
import com.holy.mytrace.helpers.SQLiteHelper;
import com.holy.mytrace.models.Waypoint;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class TraceFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mGoogleMap;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;
    private LocationRequest mLocationRequest;
    private Location mCurrentLocation;

    private Marker mCurrentMarker;
    private List<Marker> mMarkerList;
    private LocalDate mSelectedDate = LocalDate.now();


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
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

                if (mGoogleMap != null && mCurrentLocation == null) {
                    updateLocationUIs(latLng, false);
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

        return inflater.inflate(R.layout.fragment_trace, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 구글맵 초기화
        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        updateDateUI(mSelectedDate);

        // 버튼 리스너 설정
        ImageButton prevDayButton = view.findViewById(R.id.ibtnPrevDay);
        ImageButton nextDayButton = view.findViewById(R.id.ibtnNextDay);
        prevDayButton.setOnClickListener(v -> {
            // 어제 기록 보이기
            mSelectedDate = mSelectedDate.minusDays(1);
            updateDateUI(mSelectedDate);
            updateWaypointUIs(mSelectedDate);
        });
        nextDayButton.setOnClickListener(v -> {
            // 내일 기록 보이기
            mSelectedDate = mSelectedDate.plusDays(1);
            updateDateUI(mSelectedDate);
            updateWaypointUIs(mSelectedDate);
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        // Waypoint UI 업데이트
        updateWaypointUIs();

        // 위치 업데이트 시작
        if (getContext() != null &&
                ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
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

    private void updateLocationUIs(LatLng latLng, boolean moveCamera) {

        if (mCurrentMarker != null) {
            mCurrentMarker.remove();
        }

        // 현위치 마커 추가
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
                });
            }
        }
    }

    public void updateLocationUIs(boolean moveCamera) {

        if (mCurrentLocation != null) {
            LatLng latLng = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
            updateLocationUIs(latLng, moveCamera);
        }
    }

    // Waypoint UI 업데이트

    private void updateWaypointUIs(LocalDate date) {

        // 설정일자 Waypoint 모두 획득
        List<Waypoint> waypointList = SQLiteHelper.getInstance(getContext())
                .getWaypointsByDate(date.getYear(), date.getMonthValue(), date.getDayOfMonth());

        // Waypoint 리사이클러뷰 업데이트
        buildWaypointRecycler(waypointList);

        // Waypoint 마커 업데이트
        if (mGoogleMap != null) {
            buildWaypointMarkers(waypointList);
        }
    }

    public void updateWaypointUIs() {
        updateWaypointUIs(mSelectedDate);
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

        // 기존의 오버레이 모두 지우기
        mGoogleMap.clear();

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

        // 마커 연결하는 직선 그리기
        PolylineOptions po = new PolylineOptions().color(Color.RED);
        for (Marker marker : mMarkerList) {
            po.add(marker.getPosition());
        }
        mGoogleMap.addPolyline(po);
    }

    private void updateDateUI(LocalDate date) {

        View view = getView();
        if (view == null) {
            return;
        }

        // 주어진 날짜로부터 문자열 구성
        int year = date.getYear();
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        String strDate = String.format(Locale.getDefault(),
                "%d년 %02d월 %02d일", year, month, day);

        // 날짜 UI 업데이트
        TextView dateText = view.findViewById(R.id.txtDate);
        dateText.setText(strDate);
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {

        mGoogleMap = googleMap;
        googleMap.getUiSettings().setZoomControlsEnabled(true);

        if (mMarkerList == null) {
            updateWaypointUIs(mSelectedDate);
        }

        if (mCurrentLocation != null) {
            LatLng latLng = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
            updateLocationUIs(latLng, true);
        }

        // 경로의 첫머리로 카메라 이동
        if (!mMarkerList.isEmpty()) {
            LatLng latLng = mMarkerList.get(0).getPosition();
            updateLocationUIs(latLng, true);
        }

    }

}