package com.example.festivehub;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

/** Small cards for "My Registered Events" on the Profile screen. */
public class RegisteredEventAdapter extends RecyclerView.Adapter<RegisteredEventAdapter.ViewHolder> {

    public interface OnEventClickListener {
        void onEventClick(EventList event);
    }

    private final ArrayList<EventList> events;
    private final OnEventClickListener listener;

    public RegisteredEventAdapter(ArrayList<EventList> events, OnEventClickListener listener) {
        this.events = events;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_registered_event, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EventList event = events.get(position);
        ImageUtil.loadEventImage(holder.ivImage, event);
        holder.tvCategory.setText(event.getCategory());
        holder.tvTitle.setText(event.getTitle());
        holder.tvDate.setText(event.getDateAndLocation());
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEventClick(event);
            }
        });
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvCategory, tvTitle, tvDate;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.reg_image);
            tvCategory = itemView.findViewById(R.id.reg_category);
            tvTitle = itemView.findViewById(R.id.reg_title);
            tvDate = itemView.findViewById(R.id.reg_date);
        }
    }
}
