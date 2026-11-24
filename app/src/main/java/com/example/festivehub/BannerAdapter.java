package com.example.festivehub;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/** Adapter for the banner carousel (ViewPager2) on the Home screen. */
public class BannerAdapter extends RecyclerView.Adapter<BannerAdapter.BannerViewHolder> {

    public interface OnBannerClickListener {
        void onBannerClick(EventList event);
    }

    private final ArrayList<EventList> items = new ArrayList<>();
    private final OnBannerClickListener listener;

    public BannerAdapter(OnBannerClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<EventList> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BannerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_banner, parent, false);
        return new BannerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BannerViewHolder holder, int position) {
        EventList event = items.get(position);
        ImageUtil.loadEventImage(holder.ivBanner, event);
        holder.tvBannerTitle.setText(event.getTitle());
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBannerClick(event);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class BannerViewHolder extends RecyclerView.ViewHolder {
        ImageView ivBanner;
        TextView tvBannerTitle;

        BannerViewHolder(@NonNull View itemView) {
            super(itemView);
            ivBanner = itemView.findViewById(R.id.ivBanner);
            tvBannerTitle = itemView.findViewById(R.id.tvBannerTitle);
        }
    }
}
