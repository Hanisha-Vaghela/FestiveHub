package com.example.festivehub;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
public class ManageAdapter extends RecyclerView.Adapter<ManageAdapter.ViewHolder> {

    public interface Listener {
        void onEdit(DocumentSnapshot doc);

        void onDelete(DocumentSnapshot doc);

        void onRegistrations(DocumentSnapshot doc);
    }

    private final AdminEntity entity;
    private final ArrayList<DocumentSnapshot> docs;
    private final Listener listener;

    public ManageAdapter(AdminEntity entity, ArrayList<DocumentSnapshot> docs, Listener listener) {
        this.entity = entity;
        this.docs = docs;
        this.listener = listener;
    }

    public static String valueOf(DocumentSnapshot doc, String key) {
        Object value = doc.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_manage_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DocumentSnapshot doc = docs.get(position);

        String title = valueOf(doc, entity.titleKey);
        holder.tvTitle.setText(title.isEmpty() ? "(no title)" : title);

        StringBuilder subtitle = new StringBuilder();
        for (String key : entity.subtitleKeys) {
            String value = valueOf(doc, key);
            if (!value.isEmpty()) {
                if (subtitle.length() > 0) {
                    subtitle.append("  •  ");
                }
                subtitle.append(value);
            }
        }
        holder.tvSubtitle.setText(subtitle);
        holder.tvSubtitle.setVisibility(subtitle.length() == 0 ? View.GONE : View.VISIBLE);

        holder.btnEdit.setOnClickListener(v -> listener.onEdit(doc));
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(doc));

        // "Registered Users" only makes sense for events
        holder.btnUsers.setVisibility(entity.key.equals("events") ? View.VISIBLE : View.GONE);
        holder.btnUsers.setOnClickListener(v -> listener.onRegistrations(doc));
    }

    @Override
    public int getItemCount() {
        return docs.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvSubtitle;
        Button btnEdit, btnDelete, btnUsers;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.manage_row_title);
            tvSubtitle = itemView.findViewById(R.id.manage_row_subtitle);
            btnEdit = itemView.findViewById(R.id.manage_row_edit);
            btnDelete = itemView.findViewById(R.id.manage_row_delete);
            btnUsers = itemView.findViewById(R.id.manage_row_users);
        }
    }
}
