package com.example.festivehub;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    public interface OnBookClickListener {
        void onBookClick(EventList event);
    }

    private ArrayList<EventList> eventList;
    private final OnBookClickListener listener;

    public EventAdapter(ArrayList<EventList> eventList, OnBookClickListener listener) {
        this.eventList = eventList;
        this.listener = listener;
    }

    public void setFilteredList(ArrayList<EventList> filteredList) {
        this.eventList = filteredList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event_card, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        EventList event = eventList.get(position);

        ImageUtil.loadEventImage(holder.ivEventBanner, event);
        holder.tvEventCategory.setText(event.getCategory());
        holder.tvEventTitle.setText(event.getTitle());
        holder.tvEventDateLocation.setText(event.getDateAndLocation());
        holder.tvEventPrice.setText(event.getPriceText());

        View.OnClickListener open = v -> {
            if (listener != null) {
                listener.onBookClick(event);
            }
        };
        holder.btnBookEvent.setOnClickListener(open);
        holder.itemView.setOnClickListener(open);
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        ImageView ivEventBanner;
        TextView tvEventCategory, tvEventTitle, tvEventDateLocation, tvEventPrice;
        Button btnBookEvent;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            ivEventBanner = itemView.findViewById(R.id.ivEventBanner);
            tvEventCategory = itemView.findViewById(R.id.tvEventCategory);
            tvEventTitle = itemView.findViewById(R.id.tvEventTitle);
            tvEventDateLocation = itemView.findViewById(R.id.tvEventDateLocation);
            tvEventPrice = itemView.findViewById(R.id.tvEventPrice);
            btnBookEvent = itemView.findViewById(R.id.btnBookEvent);
        }
    }
}
