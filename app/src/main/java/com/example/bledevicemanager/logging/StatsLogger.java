package com.example.bledevicemanager.logging;

import android.content.Context;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;

public class StatsLogger {
    public static class Row {
        public final long tWallMs;
        public final String addr;
        public final int rssi;
        public final long latencyMs;
        public final int seq;
        public final int type;

        public Row(long tWallMs, String addr, int rssi, long latencyMs, int seq, int type) {
            this.tWallMs = tWallMs;
            this.addr = addr;
            this.rssi = rssi;
            this.latencyMs = latencyMs;
            this.seq = seq;
            this.type = type;
        }

        public String toCsv() {
            return String.format(Locale.US, "%d,%s,%d,%d,%d,%d",
                    tWallMs, addr, rssi, latencyMs, seq, type);
        }
    }

    private final ConcurrentLinkedQueue<Row> rows = new ConcurrentLinkedQueue<>();

    public void add(Row r) { rows.add(r); }

    public File exportToCsv(Context ctx) throws IOException {
        File out = new File(ctx.getExternalFilesDir(null), "ble_stats_" + System.currentTimeMillis() + ".csv");
        try (FileWriter fw = new FileWriter(out)) {
            fw.write("t_wall_ms,addr,rssi_dbm,latency_ms,seq,type
");
            for (Row r : rows) {
                fw.write(r.toCsv());
                fw.write("
");
            }
        }
        return out;
    }
}
