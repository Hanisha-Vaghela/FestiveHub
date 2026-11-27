package com.example.festivehub;

import android.graphics.Color;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Patterns;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Add / Edit form. The inputs are built from the AdminEntity fields,
 * so one screen serves Events, Students, Volunteers, Colleges and Notifications.
 * Opened without "docId" = Add, with "docId" = Edit.
 */
public class ManageForm extends AppCompatActivity {

    AdminEntity entity;
    String docId; // null = adding a new item
    FirebaseFirestore db;

    LinearLayout container;
    Button btnSave, btnCancel;
    TextView tvTitle, tvBack;

    final Map<String, View> inputs = new LinkedHashMap<>();
    final Map<String, Object> existing = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_form);

        entity = AdminEntity.byKey(getIntent().getStringExtra("entity"));
        if (entity == null) {
            finish();
            return;
        }
        docId = getIntent().getStringExtra("docId");
        db = FirebaseFirestore.getInstance();

        container = findViewById(R.id.form_container);
        btnSave = findViewById(R.id.form_btn_save);
        btnCancel = findViewById(R.id.form_btn_cancel);
        tvTitle = findViewById(R.id.form_title);
        tvBack = findViewById(R.id.form_back);

        tvTitle.setText((docId == null ? "Add " : "Edit ") + entity.singular);
        tvBack.setOnClickListener(v -> finish());
        btnCancel.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> save());

        buildForm();

        if (docId != null) {
            btnSave.setEnabled(false);
            db.collection(entity.collection).document(docId).get()
                    .addOnSuccessListener(doc -> {
                        if (!doc.exists()) {
                            Toast.makeText(this, "Item no longer exists", Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                        Map<String, Object> data = doc.getData();
                        if (data != null) {
                            existing.putAll(data);
                        }
                        fillValues();
                        btnSave.setEnabled(true);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Couldn't load: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        finish();
                    });
        }
    }

    // ---------- Build the inputs ----------

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void buildForm() {
        for (AdminField f : entity.fields) {
            if (f.type != AdminField.Type.BOOLEAN) {
                TextView label = new TextView(this);
                label.setText(f.required ? f.label + " *" : f.label);
                label.setTextColor(Color.parseColor("#555555"));
                label.setTextSize(12);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.topMargin = dp(14);
                lp.bottomMargin = dp(4);
                container.addView(label, lp);
            }

            View input;
            switch (f.type) {
                case BOOLEAN: {
                    CheckBox box = new CheckBox(this);
                    box.setText(f.label);
                    box.setTextColor(Color.BLACK);
                    input = box;
                    break;
                }
                case CHOICE: {
                    Spinner spinner = new Spinner(this);
                    List<String> values = new ArrayList<>(java.util.Arrays.asList(f.options));
                    setChoices(spinner, values, !f.required, null);
                    input = spinner;
                    break;
                }
                case CHOICE_FROM_COLLECTION: {
                    Spinner spinner = new Spinner(this);
                    setChoices(spinner, new ArrayList<>(), true, null); // placeholder until loaded
                    loadChoices(f, spinner);
                    input = spinner;
                    break;
                }
                default:
                    input = createEditText(f);
            }

            inputs.put(f.key, input);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (f.type == AdminField.Type.BOOLEAN) {
                params.topMargin = dp(14);
            }
            container.addView(input, params);
        }
    }

    private EditText createEditText(AdminField f) {
        EditText et = new EditText(this);
        et.setBackgroundResource(R.drawable.edittext_bg);
        et.setPadding(dp(14), dp(12), dp(14), dp(12));
        et.setHint(f.hint != null ? f.hint : f.label);
        et.setHintTextColor(Color.parseColor("#888888"));
        et.setTextColor(Color.BLACK);
        et.setTextSize(14);

        switch (f.type) {
            case MULTILINE:
                et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                        | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
                et.setMinLines(3);
                et.setGravity(Gravity.TOP | Gravity.START);
                break;
            case EMAIL:
                et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
                break;
            case PHONE:
                et.setInputType(InputType.TYPE_CLASS_PHONE);
                et.setFilters(new InputFilter[]{new InputFilter.LengthFilter(10)});
                break;
            case NUMBER:
                et.setInputType(InputType.TYPE_CLASS_NUMBER);
                et.setFilters(new InputFilter[]{new InputFilter.LengthFilter(9)});
                break;
            default:
                et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
                if (f.key.equals("imageUrl")) {
                    et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
                }
        }
        return et;
    }

    /** Puts the given values into the spinner. The values list is kept in the spinner's tag. */
    private void setChoices(Spinner spinner, List<String> values, boolean addNone, String selected) {
        List<String> real = new ArrayList<>(values);
        if (selected != null && !selected.isEmpty() && !real.contains(selected)) {
            real.add(selected); // keep a saved value even if it's no longer in the list
        }
        List<String> data = new ArrayList<>();
        List<String> shown = new ArrayList<>();
        if (addNone) {
            data.add("");
            shown.add("— None —");
        }
        data.addAll(real);
        shown.addAll(real);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, shown);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setTag(data);

        if (selected != null) {
            int index = data.indexOf(selected);
            if (index >= 0) {
                spinner.setSelection(index);
            }
        }
    }

    private void loadChoices(AdminField f, Spinner spinner) {
        db.collection(f.sourceCollection).get().addOnSuccessListener(snapshots -> {
            List<String> values = new ArrayList<>();
            snapshots.getDocuments().forEach(doc -> {
                String v = doc.getString(f.sourceField);
                if (v != null && !v.isEmpty()) {
                    values.add(v);
                }
            });
            Collections.sort(values, String::compareToIgnoreCase);
            Object saved = existing.get(f.key);
            setChoices(spinner, values, true, saved == null ? null : String.valueOf(saved));
        });
    }

    // ---------- Fill when editing ----------

    @SuppressWarnings("unchecked")
    private void fillValues() {
        for (AdminField f : entity.fields) {
            View view = inputs.get(f.key);
            Object value = existing.get(f.key);

            if (view instanceof CheckBox) {
                ((CheckBox) view).setChecked(Boolean.TRUE.equals(value));
            } else if (view instanceof Spinner) {
                Spinner spinner = (Spinner) view;
                List<String> data = (List<String>) spinner.getTag();
                String saved = value == null ? "" : String.valueOf(value);
                if (data != null) {
                    if (!saved.isEmpty() && !data.contains(saved)) {
                        // options not loaded yet or value missing: rebuild with it included
                        List<String> without = new ArrayList<>(data);
                        without.remove("");
                        setChoices(spinner, without, !f.required, saved);
                    } else {
                        int index = data.indexOf(saved);
                        if (index >= 0) {
                            spinner.setSelection(index);
                        }
                    }
                }
            } else if (view instanceof EditText && value != null) {
                ((EditText) view).setText(String.valueOf(value));
            }
        }
    }

    // ---------- Save ----------

    @SuppressWarnings("unchecked")
    private void save() {
        Map<String, Object> data = new HashMap<>();

        for (AdminField f : entity.fields) {
            View view = inputs.get(f.key);

            if (f.type == AdminField.Type.BOOLEAN) {
                data.put(f.key, ((CheckBox) view).isChecked());

            } else if (f.type == AdminField.Type.CHOICE || f.type == AdminField.Type.CHOICE_FROM_COLLECTION) {
                Spinner spinner = (Spinner) view;
                List<String> values = (List<String>) spinner.getTag();
                String chosen = "";
                if (values != null && !values.isEmpty()) {
                    chosen = values.get(spinner.getSelectedItemPosition());
                }
                if (f.required && chosen.isEmpty()) {
                    Toast.makeText(this, "Please select " + f.label, Toast.LENGTH_SHORT).show();
                    return;
                }
                data.put(f.key, chosen);

            } else {
                EditText et = (EditText) view;
                String text = et.getText().toString().trim();

                if (text.isEmpty()) {
                    if (f.required) {
                        et.setError("Enter " + f.label.toLowerCase());
                        et.requestFocus();
                        return;
                    }
                    data.put(f.key, f.type == AdminField.Type.NUMBER ? (Object) 0 : "");
                    continue;
                }

                if (f.type == AdminField.Type.EMAIL && !Patterns.EMAIL_ADDRESS.matcher(text).matches()) {
                    et.setError("Enter a valid email");
                    et.requestFocus();
                    return;
                }
                if (f.type == AdminField.Type.PHONE && text.length() != 10) {
                    et.setError("Enter a 10 digit number");
                    et.requestFocus();
                    return;
                }
                // Firebase Auth emails are lowercase, so save volunteer emails the same way
                if (f.type == AdminField.Type.EMAIL && entity.key.equals("volunteers")) {
                    text = text.toLowerCase(java.util.Locale.ROOT);
                }
                if (f.type == AdminField.Type.NUMBER) {
                    try {
                        data.put(f.key, Integer.parseInt(text));
                    } catch (NumberFormatException e) {
                        et.setError("Enter a valid number");
                        et.requestFocus();
                        return;
                    }
                } else {
                    data.put(f.key, text);
                }
            }
        }

        // Home shows priceText, so keep it in sync with the price
        if (entity.key.equals("events")) {
            int amount = (Integer) data.get("priceAmount");
            data.put("priceText", amount == 0 ? "Free Entry" : "₹" + amount);
        }

        btnSave.setEnabled(false);

        if (docId == null) {
            data.put("createdAt", FieldValue.serverTimestamp());
            db.collection(entity.collection).add(data)
                    .addOnSuccessListener(ref -> done(entity.singular + " added"))
                    .addOnFailureListener(this::failed);
        } else {
            // merge keeps fields the form doesn't show (for example an event's imageName)
            db.collection(entity.collection).document(docId).set(data, SetOptions.merge())
                    .addOnSuccessListener(unused -> done(entity.singular + " updated"))
                    .addOnFailureListener(this::failed);
        }
    }

    private void done(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        finish();
    }

    private void failed(Exception e) {
        btnSave.setEnabled(true);
        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
    }
}
