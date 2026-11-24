package com.example.festivehub;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

// Admin screen: everyone who registered for one event.

public class RegisteredUsers extends AppCompatActivity {

    TextView tvBack, tvEventTitle, tvCount, tvEmpty;
    RecyclerView rvUsers;

    FirebaseFirestore db;
    ListenerRegistration registration;

    ArrayList<DocumentSnapshot> docs = new ArrayList<>();
    UserAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registered_users);

        String eventId = getIntent().getStringExtra("eventId");
        String eventTitle = getIntent().getStringExtra("eventTitle");
        if (eventId == null) {
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();

        tvBack = findViewById(R.id.reg_users_back);
        tvEventTitle = findViewById(R.id.reg_users_event);
        tvCount = findViewById(R.id.reg_users_count);
        tvEmpty = findViewById(R.id.reg_users_empty);
        rvUsers = findViewById(R.id.reg_users_rv);

        tvBack.setOnClickListener(v -> finish());
        tvEventTitle.setText(eventTitle != null ? eventTitle : "");

        adapter = new UserAdapter(docs);
        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        rvUsers.setAdapter(adapter);

        registration = db.collection("events").document(eventId).collection("registrations")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }
                    docs.clear();
                    if (snapshots != null) {
                        docs.addAll(snapshots.getDocuments());
                    }
                    // newest first
                    Collections.sort(docs, (a, b) -> {
                        Timestamp ta = a.getTimestamp("registeredAt");
                        Timestamp tb = b.getTimestamp("registeredAt");
                        if (ta == null && tb == null) return 0;
                        if (ta == null) return 1;
                        if (tb == null) return -1;
                        return tb.compareTo(ta);
                    });
                    adapter.notifyDataSetChanged();
                    tvCount.setText(docs.size() + (docs.size() == 1 ? " person registered" : " people registered"));
                    tvEmpty.setVisibility(docs.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (registration != null) {
            registration.remove();
        }
    }

    // ---------- list adapter ----------

    static class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {

        private final ArrayList<DocumentSnapshot> items;
        private final SimpleDateFormat format = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());

        UserAdapter(ArrayList<DocumentSnapshot> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_registered_user, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            DocumentSnapshot doc = items.get(position);
            String name = doc.getString("name");
            String email = doc.getString("email");
            String mobile = doc.getString("mobile");
            Timestamp time = doc.getTimestamp("registeredAt");

            // Older registrations have no name saved, so fall back to the email
            if (name == null || name.isEmpty()) {
                name = (email != null && !email.isEmpty()) ? email : "(unknown user)";
            }
            holder.tvName.setText(name);

            holder.tvEmail.setText(email != null ? email : "");
            holder.tvEmail.setVisibility(email == null || email.isEmpty() ? View.GONE : View.VISIBLE);

            holder.tvMobile.setText(mobile != null && !mobile.isEmpty() ? "Mobile: " + mobile : "");
            holder.tvMobile.setVisibility(mobile == null || mobile.isEmpty() ? View.GONE : View.VISIBLE);

            holder.tvTime.setText(time != null ? "Registered on " + format.format(time.toDate()) : "");
            holder.tvTime.setVisibility(time == null ? View.GONE : View.VISIBLE);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvEmail, tvMobile, tvTime;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.reg_user_name);
                tvEmail = itemView.findViewById(R.id.reg_user_email);
                tvMobile = itemView.findViewById(R.id.reg_user_mobile);
                tvTime = itemView.findViewById(R.id.reg_user_time);
            }
        }
    }
}
