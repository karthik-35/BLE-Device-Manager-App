package com.example.bledevicemanager.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import com.example.bledevicemanager.Config;
import com.example.bledevicemanager.NativeParser;
import com.example.bledevicemanager.logging.StatsLogger;

import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicBoolean;

public class BleDeviceConnection {
    public interface Listener {
        void onState(String addr, String state);
        void onRssi(String addr, int rssi);
        void onTelemetry(String addr, String prettyLine, long latencyMs, int seq, int type);
    }

    private final Context ctx;
    private final BluetoothDevice device;
    private final Listener listener;
    private final StatsLogger logger;

    private BluetoothGatt gatt;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private int lastRssi = 0;

    public BleDeviceConnection(Context ctx, BluetoothDevice device, Listener listener, StatsLogger logger) {
        this.ctx = ctx.getApplicationContext();
        this.device = device;
        this.listener = listener;
        this.logger = logger;
    }

    public String addr() { return device.getAddress(); }

    public void connect() {
        listener.onState(addr(), "connecting");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            gatt = device.connectGatt(ctx, false, cb, BluetoothDevice.TRANSPORT_LE);
        } else {
            gatt = device.connectGatt(ctx, false, cb);
        }
    }

    public void disconnect() {
        try {
            if (gatt != null) {
                gatt.disconnect();
                gatt.close();
            }
        } catch (Exception ignored) {}
        gatt = null;
        connected.set(false);
        listener.onState(addr(), "disconnected");
    }

    private void scheduleRssi() {
        main.postDelayed(new Runnable() {
            @Override public void run() {
                if (gatt != null && connected.get()) {
                    gatt.readRemoteRssi();
                    scheduleRssi();
                }
            }
        }, Config.RSSI_POLL_MS);
    }

    private void enableNotifications(BluetoothGattCharacteristic ch) {
        boolean ok = gatt.setCharacteristicNotification(ch, true);
        if (!ok) {
            listener.onState(addr(), "notify failed");
            return;
        }
        BluetoothGattDescriptor cccd = ch.getDescriptor(Config.CLIENT_CONFIG_UUID);
        if (cccd == null) {
            listener.onState(addr(), "CCCD missing");
            return;
        }
        cccd.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        gatt.writeDescriptor(cccd);
    }

    private final BluetoothGattCallback cb = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt g, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                connected.set(true);
                listener.onState(addr(), "connected, discovering");
                g.discoverServices();
                scheduleRssi();
            } else {
                connected.set(false);
                listener.onState(addr(), "disconnected");
                try { g.close(); } catch (Exception ignored) {}
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt g, int status) {
            BluetoothGattService svc = g.getService(Config.TELEMETRY_SERVICE_UUID);
            if (svc == null) { listener.onState(addr(), "service not found"); return; }
            BluetoothGattCharacteristic ch = svc.getCharacteristic(Config.TELEMETRY_CHAR_UUID);
            if (ch == null) { listener.onState(addr(), "char not found"); return; }
            listener.onState(addr(), "subscribing");
            enableNotifications(ch);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt g, BluetoothGattDescriptor descriptor, int status) {
            if (Config.CLIENT_CONFIG_UUID.equals(descriptor.getUuid())) {
                listener.onState(addr(), "subscribed");
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt g, BluetoothGattCharacteristic ch) {
            byte[] data = ch.getValue();
            long now = System.currentTimeMillis();
            try {
                String json = NativeParser.parsePacket(data);
                JSONObject o = new JSONObject(json);
                boolean ok = o.optBoolean("ok", false);
                if (!ok) {
                    listener.onTelemetry(addr(), "parse error: " + o.optString("error","?"), -1, -1, -1);
                    return;
                }
                int type = o.optInt("type", -1);
                int seq = o.optInt("seq", -1);
                long devTs = o.optLong("dev_ts_ms", -1);
                boolean crcOk = o.optBoolean("crc_ok", false);

                long latency = devTs >= 0 ? Math.max(0, now - devTs) : -1;
                String payloadHex = o.optString("payload_hex", "");

                String line = "type=" + type + " seq=" + seq + " crc=" + (crcOk ? "ok" : "BAD") +
                        " latency=" + latency + "ms payload=" + payloadHex;

                listener.onTelemetry(addr(), line, latency, seq, type);
                logger.add(new StatsLogger.Row(now, addr(), lastRssi, latency, seq, type));
            } catch (Exception e) {
                listener.onTelemetry(addr(), "exception: " + e.getMessage(), -1, -1, -1);
            }
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt g, int rssi, int status) {
            lastRssi = rssi;
            listener.onRssi(addr(), rssi);
        }
    };
}
