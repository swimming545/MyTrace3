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

        // ????????? ?????????
        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        updateDateUI(mSelectedDate);

        // ?????? ????????? ??????
        ImageButton prevDayButton = view.findViewById(R.id.ibtnPrevDay);
        ImageButton nextDayButton = view.findViewById(R.id.ibtnNextDay);
        prevDayButton.setOnClickListener(v -> {
            // ?????? ?????? ?????????
            mSelectedDate = mSelectedDate.minusDays(1);
            updateDateUI(mSelectedDate);
            updateWaypointUIs(mSelectedDate);
        });
        nextDayButton.setOnClickListener(v -> {
            // ?????? ?????? ?????????
            mSelectedDate = mSelectedDate.plusDays(1);
            updateDateUI(mSelectedDate);
            updateWaypointUIs(mSelectedDate);
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        // Waypoint UI ????????????
        updateWaypointUIs();

        // ?????? ???????????? ??????
        if (getContext() != null &&
                ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.getMainLooper());
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        // ?????? ???????????? ??????
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }

    // ????????? UI ????????????

    private void updateLocationUIs(LatLng latLng, boolean moveCamera) {

        if (mCurrentMarker != null) {
            mCurrentMarker.remove();
        }

        // ????????? ?????? ??????
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

    // Waypoint UI ????????????

    private void updateWaypointUIs(LocalDate date) {

        // ???????????? Waypoint ?????? ??????
        List<Waypoint> waypointList = SQLiteHelper.getInstance(getContext())
                .getWaypointsByDate(date.getYear(), date.getMonthValue(), date.getDayOfMonth());

        // Waypoint ?????????????????? ????????????
        buildWaypointRecycler(waypointList);

        // Waypoint ?????? ????????????
        if (mGoogleMap != null) {
            buildWaypointMarkers(waypointList);
        }
    }

    public void updateWaypointUIs() {
        updateWaypointUIs(mSelectedDate);
    }

    // Waypoint ?????????????????? ????????????

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

    // Waypoint ?????? ????????????

    private void buildWaypointMarkers(List<Waypoint> waypointList) {

        // ????????? ???????????? ?????? ?????????
        mGoogleMap.clear();

        mMarkerList = new ArrayList<>();

        // ????????? ?????? ????????????
        for (Waypoint wayPoint : waypointList) {
            MarkerOptions options = new MarkerOptions()
                    .position(new LatLng(wayPoint.getLatitude(), wayPoint.getLongitude()))
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.pin))
                    .title(wayPoint.getName());
            Marker newMarker = mGoogleMap.addMarker(options);
            mMarkerList.add(newMarker);
        }

        // ?????? ???????????? ?????? ?????????
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

        // ????????? ??????????????? ????????? ??????
        int year = date.getYear();
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();

        String strDate = String.format(Locale.getDefault(),
                "%d??? %02d??? %02d???", year, month, day);

        // ?????? UI ????????????
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

        // ????????? ???????????? ????????? ??????
        if (!mMarkerList.isEmpty()) {
            LatLng latLng = mMarkerList.get(0).getPosition();
            updateLocationUIs(latLng, true);
        }

    }

}