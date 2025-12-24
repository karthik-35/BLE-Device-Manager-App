package com.example.bledevicemanager.model;

public class DeviceRow {
    public final String name;
    public final String address;
    public int rssi = 0;
    public String state = "discovered";
    public long lastLatencyMs = -1;

    public DeviceRow(String name, String address) {
        this.name = name != null ? name : "(unknown)";
        this.address = address;
    }
}
