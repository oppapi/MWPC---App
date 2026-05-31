package com.example.mppxv1;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class LocationForegroundService extends Service {

    public static final String ACTION_LOCATION_UPDATE = "com.example.mppxv1.LOCATION_UPDATE";

    private static final String CHANNEL_ID = "location_channel";
    private static final int NOTIFICATION_ID = 1;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private DatabaseReference dbRef;
    private String userId;

    @Override
    public void onCreate() {
        super.onCreate();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        userId = FirebaseAuth.getInstance().getUid();
        
        // Use user ID in the path to keep locations organized
        if (userId != null) {
            dbRef = FirebaseDatabase.getInstance().getReference("driver_locations").child(userId);
        } else {
            dbRef = FirebaseDatabase.getInstance().getReference("driver_locations").child("anonymous");
        }

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;
                for (Location location : locationResult.getLocations()) {
                    if (location != null) sendToFirebaseAndBroadcast(location);
                }
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();

        Intent notificationIntent = new Intent(this, HomeActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 
                PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Logistic Tracking Active")
                .setContentText("Sharing your location securely")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingIntent)
                .setOngoing(true);

        startForeground(NOTIFICATION_ID, builder.build());

        boolean highAccuracy = intent != null && intent.getBooleanExtra("highAccuracy", false);
        startLocationUpdates(highAccuracy);
        
        return START_STICKY;
    }

    @android.annotation.SuppressLint("MissingPermission")
    private void startLocationUpdates(boolean highAccuracy) {
        LocationRequest locationRequest = new LocationRequest.Builder(
                highAccuracy ? Priority.PRIORITY_HIGH_ACCURACY : Priority.PRIORITY_BALANCED_POWER_ACCURACY, 
                15000)
                .setMinUpdateIntervalMillis(10000)
                .setMinUpdateDistanceMeters(10)
                .build();

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void sendToFirebaseAndBroadcast(Location location) {
        HashMap<String, Object> data = new HashMap<>();
        data.put("lat", location.getLatitude());
        data.put("lng", location.getLongitude());
        data.put("bearing", location.getBearing());
        data.put("speed", location.getSpeed());
        data.put("accuracy", location.getAccuracy());
        data.put("timestamp", System.currentTimeMillis());
        
        // Use setValue instead of push if you only want the "latest" position
        // Or keep push() for a tracking history
        dbRef.child("current").setValue(data);
        dbRef.child("history").push().setValue(data);

        Intent intent = new Intent(ACTION_LOCATION_UPDATE);
        intent.putExtra("lat", location.getLatitude());
        intent.putExtra("lng", location.getLongitude());
        intent.putExtra("speed", location.getSpeed());
        intent.putExtra("accuracy", location.getAccuracy());
        sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Location Tracking",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Shows a notification while logistic tracking is active");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
