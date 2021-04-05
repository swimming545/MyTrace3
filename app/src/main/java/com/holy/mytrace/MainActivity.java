package com.holy.mytrace;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.MenuItem;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {

    // 사용될 프래그먼트들
    private HomeFragment mHomeFragment;
    private TraceFragment mTraceFragment;
    private HospitalFragment mHospitalFragment;

    // 현재 띄워진 프래그먼트
    private Fragment mCurrentFragment;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 프래그먼트를 생성한다
        mHomeFragment = new HomeFragment();
        mTraceFragment = new TraceFragment();
        mHospitalFragment = new HospitalFragment();

        // 홈 프래그먼트를 띄운다
        showHomeFragment();

        // Bottom Navigation 에 메뉴 선택 리스너를 설정한다.
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        bottomNavigation.setOnNavigationItemSelectedListener(this);
    }

    /**
     * 액션 바의 타이틀을 업데이트한다 (현재 띄워진 프래그먼트에 따라)
     */
    private void updateTitle() {

        if (mCurrentFragment == mHomeFragment) {
            getSupportActionBar().setTitle("홈");
        } else if (mCurrentFragment == mTraceFragment) {
            getSupportActionBar().setTitle("이동경로");
        } else if (mCurrentFragment == mHospitalFragment) {
            getSupportActionBar().setTitle("의료시설");
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