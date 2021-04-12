package com.holy.mytrace;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.holy.mytrace.services.TracingService;


public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {

    public static final int REQUEST_LOCATION = 100;

    // 사용될 프래그먼트들
    private HomeFragment mHomeFragment;
    private TraceFragment mTraceFragment;
    private HospitalFragment mHospitalFragment;

    // 현재 띄워진 프래그먼트
    private Fragment mCurrentFragment;

    // 서비스
    private TracingService mTracingService;
    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            TracingService.LocalBinder binder = (TracingService.LocalBinder) service;
            mTracingService = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {}
    };

    // BR
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Waypoint 업데이트 방송 수신
            if (TracingService.ACTION_WAYPOINT_UPDATED.equals(intent.getAction())) {
                // 현재 프래그먼트의 UI 업데이트
                if (mCurrentFragment == mHomeFragment) {
                    mHomeFragment.updateWaypointUIs();
                } else if (mCurrentFragment == mTraceFragment) {
                    mTraceFragment.updateWaypointUIs();
                }
            } else if (TracingService.ACTION_WAYPOINT_ADDED.equals(intent.getAction())) {
                // 현재 프래그먼트의 UI 업데이트
                if (mCurrentFragment == mHomeFragment) {
                    mHomeFragment.updateWaypointUIs();
                    mHomeFragment.updateLocationUIs(true);
                } else if (mCurrentFragment == mTraceFragment) {
                    mTraceFragment.updateWaypointUIs();
                    mTraceFragment.updateLocationUIs(true);
                }
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 필요한 퍼미션을 요청한다
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
            }, REQUEST_LOCATION);
        }

        // 프래그먼트를 생성한다
        mHomeFragment = new HomeFragment();
        mTraceFragment = new TraceFragment();
        mHospitalFragment = new HospitalFragment();

        // 홈 프래그먼트를 띄운다
        showHomeFragment();

        // Bottom Navigation 에 메뉴 선택 리스너를 설정한다.
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        bottomNavigation.setOnNavigationItemSelectedListener(this);

        // 서비스를 시작한다
        Intent serviceIntent = new Intent(this, TracingService.class);
        startForegroundService(serviceIntent);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // 바인딩을 시작한다
        Intent serviceIntent = new Intent(this, TracingService.class);
        bindService(serviceIntent, mConnection, BIND_AUTO_CREATE);

        // BR 등록
        IntentFilter filter = new IntentFilter(TracingService.ACTION_WAYPOINT_UPDATED);
        registerReceiver(mBroadcastReceiver, filter);
    }

    @Override
    protected void onStop() {
        super.onStop();

        // 바인딩을 중단한다
        unbindService(mConnection);

        // BR 등록해제
        unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // 퍼미션 처리
        if (requestCode == REQUEST_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this,
                        "퍼미션이 없으면 위치를 확인할 수 없습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * 액션 바의 타이틀을 업데이트한다 (현재 띄워진 프래그먼트에 따라)
     */
    private void updateTitle() {

        ActionBar actionBar = getSupportActionBar();
        if (actionBar == null) {
            return;
        }

        if (mCurrentFragment == mHomeFragment) {
            actionBar.setTitle("홈");
        } else if (mCurrentFragment == mTraceFragment) {
            actionBar.setTitle("이동경로");
        } else if (mCurrentFragment == mHospitalFragment) {
            actionBar.setTitle("의료시설");
        }
    }

    /**
     * HomeFragment 를 띄운다
     */
    private void showHomeFragment() {

        mCurrentFragment = mHomeFragment;

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragContainer, mCurrentFragment)
                .addToBackStack(null)
                .commit();

        // 앱 바의 제목을 바꾼다
        updateTitle();
    }

    /**
     * TraceFragment 를 띄운다
     */
    private void showTraceFragment() {

        mCurrentFragment = mTraceFragment;

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragContainer, mCurrentFragment)
                .addToBackStack(null)
                .commit();

        // 앱 바의 제목을 바꾼다
        updateTitle();
    }

    /**
     * HospitalFragment 를 띄운다
     */
    private void showHospitalFragment() {

        mCurrentFragment = mHospitalFragment;

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragContainer, mCurrentFragment)
                .addToBackStack(null)
                .commit();

        // 앱 바의 제목을 바꾼다
        updateTitle();
    }

    /**
     * Bottom Navigation 의 메뉴 선택을 처리한다
     */
    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()) {
            case R.id.itemHome:
                showHomeFragment();
                return true;
            case R.id.itemTrace:
                showTraceFragment();
                return true;
            case R.id.itemHospital:
                showHospitalFragment();
                return true;
        }
        return false;
    }
}