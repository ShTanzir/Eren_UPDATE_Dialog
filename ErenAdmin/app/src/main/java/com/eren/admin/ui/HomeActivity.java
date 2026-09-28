package com.eren.admin.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.eren.admin.R;
import com.eren.admin.data.FirebaseHelper;
import com.eren.admin.data.Prefs;
import com.eren.admin.models.AppModel;
import com.eren.admin.util.KeyGen;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeActivity extends AppCompatActivity {

    private final List<AppModel> apps = new ArrayList<>();
    private AppsAdapter adapter;
    private ValueEventListener appsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        TextView tvTitle = findViewById(R.id.tvTitle);
        tvTitle.setText("Eren Admin");

        RecyclerView rv = findViewById(R.id.rvApps);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AppsAdapter();
        rv.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fabAdd);
        fab.setOnClickListener(v -> showAddAppDialog());

        findViewById(R.id.btnKeys).setOnClickListener(v ->
                startActivity(new Intent(this, CreateKeyActivity.class)));

        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            Prefs.clearSession();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        listenApps();
    }

    private void listenApps() {
        appsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                apps.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Object val = child.getValue();
                    if (val instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> map = (Map<String, Object>) val;
                        apps.add(AppModel.fromSnapshot(child.getKey(), map));
                    }
                }
                // newest first
                apps.sort((a, b) -> Long.compare(b.createdAt, a.createdAt));
                adapter.notifyDataSetChanged();
                findViewById(R.id.tvEmpty).setVisibility(apps.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(HomeActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };
        FirebaseHelper.apps().addValueEventListener(appsListener);
    }

    private void showAddAppDialog() {
        View form = LayoutInflater.from(this).inflate(R.layout.dialog_add_app, null);
        EditText etName = form.findViewById(R.id.etAppName);
        EditText etDetail = form.findViewById(R.id.etShortDetail);
        EditText etIcon = form.findViewById(R.id.etIconUrl);

        new AlertDialog.Builder(this)
                .setTitle("Add App")
                .setView(form)
                .setPositiveButton("Add", (d, w) -> {
                    String name = etName.getText().toString().trim();
                    String detail = etDetail.getText().toString().trim();
                    String icon = etIcon.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, "App name required", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    AppModel app = new AppModel();
                    app.name = name;
                    app.shortDetail = detail;
                    app.iconUrl = icon;
                    app.connectKey = KeyGen.generateConnectKey();
                    app.databaseURL = Prefs.getDatabaseUrl();
                    app.dialogEnabled = false; // start disabled until admin enables
                    app.createdAt = System.currentTimeMillis();
                    app.dialogConfig.brandName = name.length() > 3
                            ? name.substring(0, name.length() - 3) + name.substring(name.length() - 3)
                            : name;
                    if (name.length() >= 4) {
                        app.dialogConfig.brandName = name;
                        app.dialogConfig.brandHighlight = name.substring(Math.max(0, name.length() - 4));
                    }

                    String id = FirebaseHelper.apps().push().getKey();
                    FirebaseHelper.apps().child(id).setValue(app.toMap())
                            .addOnSuccessListener(a ->
                                    Toast.makeText(this, "App added", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (appsListener != null) {
            FirebaseHelper.apps().removeEventListener(appsListener);
        }
    }

    // ---- Adapter ----
    private class AppsAdapter extends RecyclerView.Adapter<AppsAdapter.VH> {

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_app, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            AppModel a = apps.get(position);
            h.tvName.setText(a.name);
            h.tvDetail.setText(a.shortDetail);
            h.tvKey.setText(a.connectKey);
            h.tvDate.setText(new SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    .format(new Date(a.createdAt)));
            h.tvStatus.setText(a.dialogEnabled ? "Dialog ON" : "Dialog OFF");
            h.tvStatus.setTextColor(a.dialogEnabled ? 0xFF2E7D32 : 0xFFC62828);

            if (a.iconUrl != null && !a.iconUrl.isEmpty()) {
                Glide.with(h.ivIcon).load(a.iconUrl).circleCrop().into(h.ivIcon);
            } else {
                h.ivIcon.setImageResource(R.drawable.ic_app_placeholder);
            }

            h.itemView.setOnClickListener(v -> {
                Intent i = new Intent(HomeActivity.this, AppDetailActivity.class);
                i.putExtra("app_id", a.id);
                startActivity(i);
            });
        }

        @Override
        public int getItemCount() {
            return apps.size();
        }

        class VH extends RecyclerView.ViewHolder {
            ImageView ivIcon;
            TextView tvName, tvDetail, tvKey, tvDate, tvStatus;

            VH(View v) {
                super(v);
                ivIcon = v.findViewById(R.id.ivIcon);
                tvName = v.findViewById(R.id.tvName);
                tvDetail = v.findViewById(R.id.tvDetail);
                tvKey = v.findViewById(R.id.tvKey);
                tvDate = v.findViewById(R.id.tvDate);
                tvStatus = v.findViewById(R.id.tvStatus);
            }
        }
    }
}
