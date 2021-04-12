package com.holy.mytrace.models;

import java.time.LocalDateTime;

public class Waypoint {

    // 고유키
    private int id;
    // GPS 좌표
    private double latitude;
    private double longitude;
    // 시간
    private LocalDateTime beginTime;
    private LocalDateTime endTime;
    // 장소명
    private String name;

    public Waypoint(int id, double latitude, double longitude, LocalDateTime beginTime, LocalDateTime endTime, String name) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.beginTime = beginTime;
        this.endTime = endTime;
        this.name = name;
    }

    public Waypoint(double latitude, double longitude, LocalDateTime beginTime, LocalDateTime endTime, String name) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.beginTime = beginTime;
        this.endTime = endTime;
        this.name = name;
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

    public String getName() {
        return name;
    }
}
