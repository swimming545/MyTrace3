package com.holy.mytrace;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.tabs.TabLayout;
import com.holy.mytrace.adapters.HospitalAdapter;
import com.holy.mytrace.helpers.HospitalXmlParser;
import com.holy.mytrace.helpers.SQLiteHelper;
import com.holy.mytrace.models.Hospital;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class HospitalFragment extends Fragment implements OnMapReadyCallback, TabLayout.OnTabSelectedListener {

    // API URL
    public static final String URL_HOSPITAL = "http://apis.data.go.kr/B551182/hospInfoService/getHospBasisList?serviceKey=[KEY]&numOfRows=[NUM]&xPos=[XPOS]&yPos=[YPOS]&radius=[RAD]";
    public static final String URL_PHARMACY = "http://apis.data.go.kr/B551182/pharmacyInfoService/getParmacyBasisList?serviceKey=[KEY]&numOfRows=[NUM]&xPos=[XPOS]&yPos=[YPOS]&radius=[RAD]";
    public static final String PLACEHOLDER_KEY = "[KEY]";
    public static final String PLACEHOLDER_NUM = "[NUM]";
    public static final String PLACEHOLDER_XPOS = "[XPOS]";
    public static final String PLACEHOLDER_YPOS = "[YPOS]";
    public static final String PLACEHOLDER_RADIUS = "[RAD]";
    public static final String KEY = "3U5FAOrbKwtTBXYzH54eZ0jeVY0FeCjK4xcoXMOBgH%2BOJq2omZYKSRYMqTjQTg5ytsAbkcmF3gDycB2zjPMprA%3D%3D";
    public static final int HOSPITAL_CENTER = 0;
    public static final int PHARMACY = 1;
    public static final int CORONA_EXAM = 2;

    private GoogleMap mGoogleMap;
    private FusedLocationProviderClient mFusedLocationClient;
    private Location mCurrentLocation;

    private List<Hospital> mHospitalList;
    private List<Marker> mMarkerList;

    private int mHospitalSort = HOSPITAL_CENTER;


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_hospital, container, false);

        TabLayout tabLayout = view.findViewById(R.id.tabLayoutHospital);
        tabLayout.addOnTabSelectedListener(this);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getContext() == null || getActivity() == null) {
            return;
        }

        // ????????? ?????????
        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // ?????? ?????? ??????
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(getActivity(), location -> {
                        if (location != null) {
                            // ?????? ???????????? ?????? ?????? ??????
                            mCurrentLocation = location;
                            loadHospitalData(URL_HOSPITAL, location.getLatitude(), location.getLongitude());

                            // ????????? ????????? ????????? ??????
                            if (mGoogleMap != null) {
                                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                                mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15.0f));
                                mGoogleMap.addMarker(new MarkerOptions().position(latLng));
                            }
                        }
                    });
        }
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onTabSelected(TabLayout.Tab tab) {

        if (mCurrentLocation == null || tab.getText() == null) {
            return;
        }

        String strTab = tab.getText().toString();
        Resources res = getResources();
        double latitude = mCurrentLocation.getLatitude();
        double longitude = mCurrentLocation.getLongitude();

        if (strTab.equals(res.getString(R.string.hospital_center))) {
            loadHospitalData(URL_HOSPITAL, latitude, longitude);
            mHospitalSort = HOSPITAL_CENTER;
        } else if (strTab.equals(res.getString(R.string.pharmacy))) {
            loadHospitalData(URL_PHARMACY, latitude, longitude);
            mHospitalSort = PHARMACY;
        }
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) { }

    @Override
    public void onTabReselected(TabLayout.Tab tab) { }

    // ?????? ?????? ??????

    private void loadHospitalData(String url, double latitude, double longitude) {

        String urlHospital = url
                .replace(PLACEHOLDER_KEY, KEY)
                .replace(PLACEHOLDER_NUM, String.valueOf(100))
                .replace(PLACEHOLDER_XPOS, String.valueOf(longitude))
                .replace(PLACEHOLDER_YPOS, String.valueOf(latitude))
                .replace(PLACEHOLDER_RADIUS, String.valueOf(500));

        new DownloadXmlTask(this).execute(urlHospital);
    }

    // ?????? ?????? UI ????????????

    private void updateHospitalUIs(List<Hospital> hospitalList) {

        mHospitalList = hospitalList;

        // ????????? ???????????? ??????
        mHospitalList.sort((o1, o2) -> (int)(o1.getDistance() - o2.getDistance()));

        // ?????? ?????????????????? ????????????
        buildHospitalRecycler(hospitalList);

        // ?????? ?????? ????????????
        if (mGoogleMap != null) {
            buildHospitalMarkers(hospitalList);
        }
    }

    private void buildHospitalRecycler(List<Hospital> hospitalList) {

        View view = getView();
        if (view == null || mCurrentLocation == null) {
            return;
        }

        RecyclerView recycler = view.findViewById(R.id.recyclerHospital);
        recycler.setHasFixedSize(true);
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));

        HospitalAdapter adapter = new HospitalAdapter(hospitalList);
        recycler.setAdapter(adapter);
    }

    private void buildHospitalMarkers(List<Hospital> hospitalList) {

        // ????????? ?????? ?????? ?????????
        if (mMarkerList != null) {
            for (Marker marker : mMarkerList) {
                marker.remove();
            }
        }

        mMarkerList = new ArrayList<>();

        // ????????? ?????? ????????????
        for (Hospital hospital : hospitalList) {
            MarkerOptions options = new MarkerOptions()
                    .position(new LatLng(hospital.getLatitude(), hospital.getLongitude()))
                    .title(hospital.getName());
            switch (mHospitalSort) {
                case HOSPITAL_CENTER:
                    options.icon(BitmapDescriptorFactory.fromResource(R.drawable.vaccine));
                    break;
                case PHARMACY:
                    options.icon(BitmapDescriptorFactory.fromResource(R.drawable.pill));
                    break;
            }
            Marker newMarker = mGoogleMap.addMarker(options);
            mMarkerList.add(newMarker);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mGoogleMap = googleMap;
        googleMap.getUiSettings().setZoomControlsEnabled(true);

        // API ????????? ?????? ?????? : updateHospitalUI() ?????? ?????? ??????????????? ?????? ??????. ??? ??????????????? ?????? ???????????? ??????
        // ????????? ????????? ?????? ?????? : updateHospitalUI() ?????? ?????? ??????????????? ???. ?????? ?????? ?????????.

        if (mHospitalList != null) {
            buildHospitalMarkers(mHospitalList);
        }

        // ?????? ????????? ????????? ??????

        if (mCurrentLocation != null) {
            LatLng latLng = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15.0f));
            mGoogleMap.addMarker(new MarkerOptions().position(latLng));
        }

    }

    // XML ????????? ????????? ???????????? ?????? AsyncTask

    static class DownloadXmlTask extends AsyncTask<String, Void, List<Hospital>> {

        // ???????????? ???????????? (????????? ?????? ??????)
        private final WeakReference<HospitalFragment> reference;

        // ?????????
        public DownloadXmlTask(HospitalFragment fragment) {

            reference = new WeakReference<>(fragment);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            View view = reference.get().getView();
            if (view != null) {
                ProgressBar progressBar = view.findViewById(R.id.progress);
                progressBar.setVisibility(View.VISIBLE);
            }
        }

        // ??????????????? ????????? (?????? ?????? ????????????)
        @Override
        protected List<Hospital> doInBackground(String... strings) {

            // ????????? URL ?????? ??????
            String strUrl = strings[0];

            try {
                // URL ??? ?????????????????? InputStream ??????
                InputStream inputStream = downloadUrl(strUrl);

                // InputStream ??? parse ?????? ????????? ??????
                return new HospitalXmlParser().parse(inputStream);

            } catch (IOException | XmlPullParserException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(List<Hospital> hospitalList) {

            if (hospitalList != null) {
                // ?????? ??????????????? ????????????
                reference.get().updateHospitalUIs(hospitalList);

            } else {
                Toast.makeText(reference.get().getContext(),
                        "??????????????? ??????????????????.", Toast.LENGTH_SHORT).show();
            }

            View view = reference.get().getView();
            if (view != null) {
                ProgressBar progressBar = view.findViewById(R.id.progress);
                progressBar.setVisibility(View.INVISIBLE);
            }
        }

        // URL ????????? InputStream ??? ????????????

        private InputStream downloadUrl(String strUrl) throws IOException {

            java.net.URL url = new URL(strUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(60000 /* ????????? */);
            conn.setConnectTimeout(60000 /* ????????? */);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);

            // ?????? ??????
            conn.connect();
            return conn.getInputStream();
        }
    }

}
