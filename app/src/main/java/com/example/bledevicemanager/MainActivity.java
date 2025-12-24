package com.example.bledevicemanager;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.bledevicemanager.ble.BleManager;
import com.example.bledevicemanager.databinding.ActivityMainBinding;
import com.example.bledevicemanager.logging.StatsLogger;
import com.example.bledevicemanager.model.DeviceRow;
import com.example.bledevicemanager.ui.DeviceAdapter;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding vb;
    private BleManager ble;
    private final StatsLogger logger = new StatsLogger();

    private final DeviceAdapter adapter = new DeviceAdapter();
    private final Map<String, DeviceRow> rows = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        vb = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(vb.getRoot());

        vb.recyclerDevices.setLayoutManager(new LinearLayoutManager(this));
        vb.recyclerDevices.setAdapter(adapter);

        ble = new BleManager(this, new BleManager.Listener() {
            @Override public void onDiscovered(BluetoothDevice d, int rssi) {
                String addr = d.getAddress();
                DeviceRow r = rows.get(addr);
                if (r == null) {
                    r = new DeviceRow(d.getName(), addr);
                    rows.put(addr, r);
                }
                r.rssi = rssi;
                adapter.upsert(r);
            }

            @Override public void onState(String addr, String state) {
                DeviceRow r = rows.get(addr);
                if (r == null) {
                    r = new DeviceRow("(device)", addr);
                    rows.put(addr, r);
                }
                r.state = state;
                adapter.upsert(r);
                appendLog(addr + " state: " + state);
            }

            @Override public void onRssi(String addr, int rssi) {
                DeviceRow r = rows.get(addr);
                if (r != null) {
                    r.rssi = rssi;
                    adapter.upsert(r);
                }
            }

            @Override public void onTelemetry(String addr, String prettyLine, long latencyMs, int seq, int type) {
                DeviceRow r = rows.get(addr);
                if (r != null && latencyMs >= 0) {
                    r.lastLatencyMs = latencyMs;
                    adapter.upsert(r);
                }
                appendLog(addr + " " + prettyLine);
            }
        }, logger);

        adapter.setOnRowClick(row -> {
            if (!ensurePerms()) return;
            ble.connectOrToggle(row.address);
        });

        vb.btnScan.setOnClickListener(v -> {
            if (!ensurePerms()) return;
            if (!ble.isBleReady()) {
                Toast.makeText(this, "Enable Bluetooth", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
                return;
            }
            ble.startScan();
            appendLog("Scanning… (tap a device to connect)");
        });

        vb.btnStop.setOnClickListener(v -> {
            ble.stopScan();
            appendLog("Scan stopped");
        });

        vb.btnExport.setOnClickListener(v -> {
            try {
                File f = logger.exportToCsv(this);
                Toast.makeText(this, "Exported: " + f.getAbsolutePath(), Toast.LENGTH_LONG).show();
                appendLog("Exported CSV: " + f.getAbsolutePath());
            } catch (Exception e) {
                Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        ensurePerms();
    }

    private void appendLog(String line) {
        vb.txtLog.append(line + "\n");
    }

    private boolean ensurePerms() {
        String[] perms = Permissions.required();
        boolean all = true;
        for (String p : perms) {
            all &= ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED;
        }
        if (!all) {
            ActivityCompat.requestPermissions(this, perms, Permissions.REQ);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == Permissions.REQ) {
            boolean ok = true;
            for (int g : grantResults) ok &= (g == PackageManager.PERMISSION_GRANTED);
            if (!ok) Toast.makeText(this, "Permissions required for BLE", Toast.LENGTH_LONG).show();
        }
    }
}
