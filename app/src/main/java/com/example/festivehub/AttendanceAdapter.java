package com.example.festivehub;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AttendanceAdapter extends RecyclerView.Adapter<AttendanceAdapter.ViewHolder> {

    private final List<AttendanceRecord> records;

    public AttendanceAdapter(List<AttendanceRecord> records) {
        this.records = records;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_attendance_record, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AttendanceRecord record = records.get(position);
        holder.tvUserName.setText(record.getUserName() != null ? record.getUserName() : "Attendee");
        holder.tvEventName.setText(record.getEventName() != null ? record.getEventName() : "Event");

        String date = record.getCheckInDate() != null ? record.getCheckInDate() : "";
        String time = record.getCheckInTime() != null ? record.getCheckInTime() : "";
        holder.tvDateTime.setText(date + (time.isEmpty() ? "" : " • " + time));

        holder.tvStatus.setText(record.getAttendanceStatus() != null ? record.getAttendanceStatus() : "Present");
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName, tvEventName, tvDateTime, tvStatus;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.att_item_userName);
            tvEventName = itemView.findViewById(R.id.att_item_eventName);
            tvDateTime = itemView.findViewById(R.id.att_item_dateTime);
            tvStatus = itemView.findViewById(R.id.att_item_status);
        }
    }
}