package com.holy.mytrace.models;

public class Hospital {

    private final String name;
    private final String address;
    private final String tel;
    private final double latitude;
    private final double longitude;
    private final double distance;

    public Hospital(String name, String address, String tel, double latitude, double longitude, double distance) {
        this.name = name;
        this.address = address;
        this.tel = tel;
        this.latitude = latitude;
        this.longitude = longitude;
        this.distance = distance;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public String getTel() {
        return tel;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getDistance() {
        return distance;
    }
}
