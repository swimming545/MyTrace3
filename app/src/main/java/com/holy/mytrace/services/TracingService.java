package com.holy.mytrace.services;

import android.Manifest;
import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Binder;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.holy.mytrace.App;
import com.holy.mytrace.R;
import com.holy.mytrace.helpers.SQLiteHelper;
import com.holy.mytrace.models.Waypoint;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

public class TracingService extends Service {

    public static final String ACTION_WAYPOINT_ADDED = "com.holy.mytrace.WAYPOINT_ADDED";
    public static final String ACTION_WAYPOINT_UPDATED = "com.holy.mytrace.WAYPOINT_UPDATED";
    public static final int THRESHOLD_SECONDS = 5;
    public static final double THRESHOLD_RADIUS_METER = 10;

    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;
    private LocationRequest mLocationRequest;

    private Location mCurrentLocation;
    private Location mAnchorLocation;
    private LocalDateTime mAnchoredTime;


    @Override
    public void onCreate() {
        super.onCreate();

        // 로케이션 서비스 관련 객체 초기화
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mLocationRequest = LocationRequest.create();
        mLocationRequest.setInterval(2000);
        mLocationRequest.setFastestInterval(1500);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {

                Location location = locationResult.getLastLocation();
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                LatLng latLng = new LatLng(latitude, longitude);

                mCurrentLocation = location;

                // 앵커 업데이트
                if (mAnchorLocation == null) {
                    mAnchorLocation = new Location(mCurrentLocation);
                    mAnchoredTime = LocalDateTime.now();
                } else {
                    // 앵커와의 거리 조사
                    double distance = mCurrentLocation.distanceTo(mAnchorLocation);
                    if (distance < THRESHOLD_RADIUS_METER) {
                        // 앵커 반경 10m 내에 머물러 있는 경우: 머무른 시간을 구한다
                        long staySeconds = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
                                - mAnchoredTime.toEpochSecond(ZoneOffset.UTC);
                        // 일정 시간 이상 머무른 경우 Waypoint 를 추가/업데이트한다
                        if (staySeconds >= THRESHOLD_SECONDS) {
                            addOrUpdateWaypoint(mAnchoredTime, latLng);
                        }
                    } else {
                        // 앵커 반경 10m 를 벗어난 경우, 앵커를 제거한다
                        mAnchorLocation = null;
                    }
                }
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Notification notification = new NotificationCompat.Builder(this, App.CHANNEL_ID)
                .setContentTitle("My Trace")
                .setContentText("Tracing...")
                .setSmallIcon(R.drawable.ic_launcher_background)
                .build();
        startForeground(2001,notification);

        // 로케이션 업데이트를 시작한다
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                    mLocationCallback,
                    Looper.getMainLooper());
        }

        return START_NOT_STICKY;
    }

    // Waypoint 추가

    private void addOrUpdateWaypoint(LocalDateTime beginTime, LatLng latLng) {

        if (!SQLiteHelper.getInstance(this)
                .hasWaypoint(beginTime)) {

            String name = "-";
            Geocoder geocoder = new Geocoder(this);
            try {
                List<Address> addressList = geocoder.getFromLocation(
                        latLng.latitude, latLng.longitude, 1);
                if (!addressList.isEmpty()) {
                    name = addressList.get(0).getFeatureName();
                    if (name == null) {
                        name = "-";
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            // 새 Waypoint 를 DB에 추가
            Waypoint waypoint = new Waypoint(latLng.latitude, latLng.longitude,
                    beginTime, LocalDateTime.now(), name);
            SQLiteHelper.getInstance(this).addWaypoint(waypoint);

            // 브로드캐스트 송출
            sendBroadcast(new Intent(ACTION_WAYPOINT_ADDED));

        } else {
            // 기존 Waypoint 의 endTime 업데이트
            SQLiteHelper.getInstance(this).updateWaypoint(beginTime, LocalDateTime.now());

            // 브로드캐스트 송출
            sendBroadcast(new Intent(ACTION_WAYPOINT_UPDATED));
        }
    }


    // Binder

    private final IBinder mBinder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class LocalBinder extends Binder {
        public TracingService getService() {
            return TracingService.this;
        }
    }

}
