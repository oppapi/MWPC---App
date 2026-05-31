package com.example.mppxv1;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity {

    private static final int PERMISSION_FINE_LOCATION = 99;
    private static final int PERMISSION_POST_NOTIFICATIONS = 101;
    int updateCount = 0;

    TextView tv_lat, tv_lon, tv_altitude, tv_accuracy, tv_speed, tv_sensor, tv_updates, tv_address;
    SwitchCompat sw_locationsupdates, sw_gps;
    MaterialButton btnLogout;
    Toolbar toolbar;

    private final BroadcastReceiver locationReceiver = new BroadcastReceiver() {
        @SuppressLint("SetTextI18n")
        @Override
        public void onReceive(Context context, Intent intent) {
            double lat = intent.getDoubleExtra("lat", 0);
            double lng = intent.getDoubleExtra("lng", 0);
            float accuracy = intent.getFloatExtra("accuracy", 0);
            float speed = intent.getFloatExtra("speed", -1f);

            tv_lat.setText(String.format(Locale.US, "%.4f", lat));
            tv_lon.setText(String.format(Locale.US, "%.4f", lng));
            tv_accuracy.setText(String.format(Locale.US, "%.1f m", accuracy));
            tv_speed.setText(speed >= 0 ? String.format(Locale.US, "%.1f km/h", speed * 3.6) : "N/A");

            updateCount++;
            tv_altitude.setText("Updates sent: " + updateCount);

            // Reverse geocode for address
            try {
                Geocoder geocoder = new Geocoder(HomeActivity.this, Locale.getDefault());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocation(lat, lng, 1, addresses -> {
                        if (!addresses.isEmpty()) {
                            runOnUiThread(() -> tv_address.setText(addresses.get(0).getAddressLine(0)));
                        }
                    });
                } else {
                    List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        tv_address.setText(addresses.get(0).getAddressLine(0));
                    }
                }
            } catch (Exception e) {
                tv_address.setText("Address currently unavailable");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        tv_lat = findViewById(R.id.tv_lat);
        tv_lon = findViewById(R.id.tv_lon);
        tv_altitude = findViewById(R.id.tv_altitude);
        tv_accuracy = findViewById(R.id.tv_accuracy);
        tv_speed = findViewById(R.id.tv_speed);
        tv_sensor = findViewById(R.id.tv_sensor);
        tv_updates = findViewById(R.id.tv_updates);
        tv_address = findViewById(R.id.tv_address);

        sw_locationsupdates = findViewById(R.id.sw_locationsupdates);
        sw_gps = findViewById(R.id.sw_gps);
        btnLogout = findViewById(R.id.btnLogout);

        DatabaseReference connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");
        connectedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean connected = snapshot.getValue(Boolean.class);
                if (connected != null && connected) {
                    tv_updates.setText("Connected");
                    tv_updates.setTextColor(ContextCompat.getColor(HomeActivity.this, android.R.color.holo_green_dark));
                } else {
                    tv_updates.setText("Disconnected");
                    tv_updates.setTextColor(ContextCompat.getColor(HomeActivity.this, android.R.color.holo_red_dark));
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                tv_updates.setText("Error");
            }
        });

        sw_gps.setOnClickListener(v -> {
            tv_sensor.setText(sw_gps.isChecked() ? "High Accuracy" : "Power Balanced");
            // If service is running, we might want to restart it with new settings
            if (sw_locationsupdates.isChecked()) {
                stopService(new Intent(this, LocationForegroundService.class));
                startForegroundService();
            }
        });

        sw_locationsupdates.setOnClickListener(view -> {
            if (sw_locationsupdates.isChecked()) {
                checkPermissionAndStart();
            } else {
                stopService(new Intent(this, LocationForegroundService.class));
                tv_updates.setText("Inactive");
                tv_updates.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
            }
        });

        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> logout());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem logoutItem = menu.add(0, 1, 0, "Logout");
        logoutItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == 1) {
            logout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        stopService(new Intent(this, LocationForegroundService.class));
        startActivity(new Intent(HomeActivity.this, Login.class));
        finish();
    }

    private void checkPermissionAndStart() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_FINE_LOCATION);
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        PERMISSION_POST_NOTIFICATIONS);
                return;
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                // Show rationale for background location
                Toast.makeText(this, "Background location is needed to track shipments while the app is closed.", Toast.LENGTH_LONG).show();
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                        PERMISSION_FINE_LOCATION + 1);
                return;
            }
        }
        
        startForegroundService();
    }

    private void startForegroundService() {
        Intent serviceIntent = new Intent(this, LocationForegroundService.class);
        serviceIntent.putExtra("highAccuracy", sw_gps.isChecked());
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            checkPermissionAndStart();
        } else {
            sw_locationsupdates.setChecked(false);
            Toast.makeText(this, "Permission denied. Cannot track location.", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(LocationForegroundService.ACTION_LOCATION_UPDATE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(locationReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(locationReceiver, filter);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(locationReceiver);
        } catch (IllegalArgumentException e) {
            // Not registered
        }
    }
}
