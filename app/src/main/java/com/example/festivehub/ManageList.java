package com.example.festivehub;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;

//Shows every item of one type with Add, Edit and Delete. Works for all admin types.
public class ManageList extends AppCompatActivity {

    AdminEntity entity;
    FirebaseFirestore db;
    ListenerRegistration registration;

    TextView tvBack, tvTitle, tvEmpty;
    Button btnAdd;
    RecyclerView rvItems;

    ArrayList<DocumentSnapshot> docs = new ArrayList<>();
    ManageAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_list);

        entity = AdminEntity.byKey(getIntent().getStringExtra("entity"));
        if (entity == null) {
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();

        tvBack = findViewById(R.id.manage_back);
        tvTitle = findViewById(R.id.manage_title);
        tvEmpty = findViewById(R.id.manage_empty);
        btnAdd = findViewById(R.id.manage_btn_add);
        rvItems = findViewById(R.id.manage_rv);

        tvTitle.setText("Manage " + entity.title);
        btnAdd.setText("+ Add " + entity.singular);
        tvBack.setOnClickListener(v -> finish());

        btnAdd.setOnClickListener(v -> {
            Intent intent = new Intent(this, ManageForm.class);
            intent.putExtra("entity", entity.key);
            startActivity(intent);
        });

        adapter = new ManageAdapter(entity, docs, new ManageAdapter.Listener() {
            @Override
            public void onEdit(DocumentSnapshot doc) {
                Intent intent = new Intent(ManageList.this, ManageForm.class);
                intent.putExtra("entity", entity.key);
                intent.putExtra("docId", doc.getId());
                startActivity(intent);
            }

            @Override
            public void onDelete(DocumentSnapshot doc) {
                confirmDelete(doc);
            }

            @Override
            public void onRegistrations(DocumentSnapshot doc) {
                Intent intent = new Intent(ManageList.this, RegisteredUsers.class);
                intent.putExtra("eventId", doc.getId());
                intent.putExtra("eventTitle", ManageAdapter.valueOf(doc, entity.titleKey));
                startActivity(intent);
            }
        });
        rvItems.setLayoutManager(new LinearLayoutManager(this));
        rvItems.setAdapter(adapter);

        listen();
    }

    // Live list: changes made in Edit/Add show up here and on Home automatically
    private void listen() {
        registration = db.collection(entity.collection).addSnapshotListener((snapshots, error) -> {
            if (error != null) {
                Toast.makeText(this, "Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
                return;
            }
            docs.clear();
            if (snapshots != null) {
                docs.addAll(snapshots.getDocuments());
            }
            Collections.sort(docs, (a, b) -> ManageAdapter.valueOf(a, entity.titleKey)
                    .compareToIgnoreCase(ManageAdapter.valueOf(b, entity.titleKey)));
            adapter.notifyDataSetChanged();
            tvEmpty.setText("No " + entity.title.toLowerCase() + " yet. Tap \"+ Add " + entity.singular + "\".");
            tvEmpty.setVisibility(docs.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    private void confirmDelete(DocumentSnapshot doc) {
        String name = ManageAdapter.valueOf(doc, entity.titleKey);
        new AlertDialog.Builder(this)
                .setTitle("Delete " + entity.singular + "?")
                .setMessage("\"" + name + "\" will be deleted. This cannot be undone.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) ->
                        db.collection(entity.collection).document(doc.getId()).delete()
                                .addOnSuccessListener(unused ->
                                        Toast.makeText(this, entity.singular + " deleted", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "Delete failed: " + e.getMessage(), Toast.LENGTH_LONG).show()))
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (registration != null) {
            registration.remove();
        }
    }
}
