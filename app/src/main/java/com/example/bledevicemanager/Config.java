package com.example.bledevicemanager;

import java.util.UUID;

public final class Config {
    // Change these to match your peripheral
    public static final UUID TELEMETRY_SERVICE_UUID =
            UUID.fromString("0000181c-0000-1000-8000-00805f9b34fb"); // example
    public static final UUID TELEMETRY_CHAR_UUID =
            UUID.fromString("00002a99-0000-1000-8000-00805f9b34fb"); // example

    // CCCD descriptor UUID (standard)
    public static final UUID CLIENT_CONFIG_UUID =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    public static final long RSSI_POLL_MS = 2000;

    private Config() {}
}
