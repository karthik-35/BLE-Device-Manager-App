package com.example.bledevicemanager.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;

import com.example.bledevicemanager.logging.StatsLogger;

import java.util.HashMap;
import java.util.Map;

public class BleManager {
    public interface Listener {
        void onDiscovered(BluetoothDevice d, int rssi);
        void onState(String addr, String state);
        void onRssi(String addr, int rssi);
        void onTelemetry(String addr, String prettyLine, long latencyMs, int seq, int type);
    }

    private final Context ctx;
    private final Listener listener;
    private final StatsLogger logger;

    private final BluetoothAdapter adapter;
    private BluetoothLeScanner scanner;
    private boolean scanning = false;

    private final Map<String, BluetoothDevice> discovered = new HashMap<>();
    private final Map<String, BleDeviceConnection> conns = new HashMap<>();

    public BleManager(Context ctx, Listener listener, StatsLogger logger) {
        this.ctx = ctx.getApplicationContext();
        this.listener = listener;
        this.logger = logger;
        BluetoothManager bm = (BluetoothManager) this.ctx.getSystemService(Context.BLUETOOTH_SERVICE);
        adapter = bm.getAdapter();
    }

    public boolean isBleReady() {
        return adapter != null && adapter.isEnabled();
    }

    public void startScan() {
        if (scanning) return;
        scanner = adapter.getBluetoothLeScanner();
        if (scanner == null) return;
        scanning = true;
        scanner.startScan(cb);
    }

    public void stopScan() {
        if (!scanning) return;
        scanning = false;
        try { scanner.stopScan(cb); } catch (Exception ignored) {}
    }

    public void connectOrToggle(String address) {
        BluetoothDevice d = discovered.get(address);
        if (d == null) {
            listener.onState(address, "not in discovery cache (scan first)");
            return;
        }
        connectOrToggle(d);
    }

    public void connectOrToggle(BluetoothDevice d) {
        String addr = d.getAddress();
        BleDeviceConnection existing = conns.get(addr);
        if (existing != null) {
            existing.disconnect();
            conns.remove(addr);
            return;
        }
        BleDeviceConnection c = new BleDeviceConnection(ctx, d, new BleDeviceConnection.Listener() {
            @Override public void onState(String addr, String state) { listener.onState(addr, state); }
            @Override public void onRssi(String addr, int rssi) { listener.onRssi(addr, rssi); }
            @Override public void onTelemetry(String addr, String prettyLine, long latencyMs, int seq, int type) {
                listener.onTelemetry(addr, prettyLine, latencyMs, seq, type);
            }
        }, logger);
        conns.put(addr, c);
        c.connect();
    }

    private final ScanCallback cb = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice d = result.getDevice();
            discovered.put(d.getAddress(), d);
            listener.onDiscovered(d, result.getRssi());
        }
    };
}
