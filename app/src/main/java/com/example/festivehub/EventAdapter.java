package com.example.festivehub;

import android.media.metrics.Event;
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
        void onBookClick(EventList allEventsList);
    }
    private ArrayList<EventList> allEventsList;
    private OnBookClickListener listener;
    public EventAdapter(ArrayList<EventList> allEventsList, OnBookClickListener listener) {
        this.allEventsList = allEventsList;
        this.listener = listener;
    }

    public void setFilteredList(ArrayList<EventList> filteredList) {
        this.allEventsList = filteredList;
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
        EventList currentEvent = allEventsList.get(position);

        holder.ivEventBanner.setImageResource(currentEvent.getImageResId());
        holder.tvEventCategory.setText(currentEvent.getCategory());
        holder.tvEventTitle.setText(currentEvent.getTitle());
        holder.tvEventDateLocation.setText(currentEvent.getDateAndLocation());
        holder.tvEventPrice.setText(currentEvent.getPriceText());

        holder.btnBookEvent.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBookClick(currentEvent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return allEventsList.size();
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