package com.holy.mytrace.models;

import java.time.LocalDateTime;

public class Waypoint {

    // 고유키
    int id;
    // GPS 좌표
    double latitude;
    double longitude;
    // 시간
    LocalDateTime beginTime;
    LocalDateTime endTime;

    public Waypoint(int id, double latitude, double longitude, LocalDateTime beginTime, LocalDateTime endTime) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.beginTime = beginTime;
        this.endTime = endTime;
    }

    public Waypoint(double latitude, double longitude, LocalDateTime beginTime, LocalDateTime endTime) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.beginTime = beginTime;
        this.endTime = endTime;
    }

    public int getId() {
        return id;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public LocalDateTime getBeginTime() {
        return beginTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

}
