package com.example.bledevicemanager.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bledevicemanager.R;
import com.example.bledevicemanager.model.DeviceRow;

import java.util.ArrayList;
import java.util.List;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.VH> {
    private final List<DeviceRow> rows = new ArrayList<>();
    private OnRowClick onRowClick;

    public interface OnRowClick {
        void onClick(DeviceRow row);
    }

    public void setOnRowClick(OnRowClick cb) {
        this.onRowClick = cb;
    }

    public void upsert(DeviceRow row) {
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).address.equals(row.address)) {
                rows.set(i, row);
                notifyItemChanged(i);
                return;
            }
        }
        rows.add(row);
        notifyItemInserted(rows.size() - 1);
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_device, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        DeviceRow r = rows.get(position);
        holder.txtName.setText(r.name + "  (RSSI " + r.rssi + " dBm)");
        holder.txtAddr.setText(r.address);
        String lat = r.lastLatencyMs >= 0 ? (" | latency ~" + r.lastLatencyMs + "ms") : "";
        holder.txtState.setText(r.state + lat);

        holder.itemView.setOnClickListener(v -> {
            if (onRowClick != null) onRowClick.onClick(r);
        });
    }

    @Override
    public int getItemCount() { return rows.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView txtName, txtAddr, txtState;
        VH(@NonNull View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.txtName);
            txtAddr = itemView.findViewById(R.id.txtAddr);
            txtState = itemView.findViewById(R.id.txtState);
        }
    }
}
