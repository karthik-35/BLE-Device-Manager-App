package com.example.bledevicemanager;

import android.Manifest;
import android.os.Build;

public final class Permissions {
    public static final int REQ = 1001;

    public static String[] required() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return new String[] {
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
            };
        }
        return new String[] { Manifest.permission.ACCESS_FINE_LOCATION };
    }

    private Permissions() {}
}
