package com.example.bledevicemanager;

public final class NativeParser {
    static {
        System.loadLibrary("ble_native");
    }

    // Returns JSON:
    // { ok: bool, type:int, seq:int, dev_ts_ms:long, payload_hex:string, crc_ok:bool, error?:string }
    public static native String parsePacket(byte[] data);

    private NativeParser() {}
}
