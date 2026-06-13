package com.skylark.detectormonitor.firebase;

import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class FirebaseHelper {

    private static final String TAG = "FirebaseHelper";

    // Explicit Singapore URL — google-services.json has no firebase_url field
    // so getInstance() would silently connect to the empty US region.
    private static final String DB_URL =
            "https://alarm-detector-e20b0-default-rtdb.asia-southeast1.firebasedatabase.app";

    // Firmware always writes to a single flat node: alarm_status/
    // The device_location field inside identifies which room sent the data.
    private final DatabaseReference alarmStatusRef;
    private ValueEventListener currentListener;

    public FirebaseHelper() {
        alarmStatusRef = FirebaseDatabase
                .getInstance(DB_URL)
                .getReference("alarm_status");
        Log.d(TAG, "FirebaseHelper ready → alarm_status (single-node, location-filtered)");
    }

    /**
     * Attach (or re-attach) a real-time listener to alarm_status.
     * The caller is responsible for filtering the incoming snapshot by
     * device_location — this class just delivers every update as-is.
     */
    public void startListening(ValueEventListener listener) {
        stopListening();
        currentListener = listener;
        alarmStatusRef.addValueEventListener(currentListener);
        Log.d(TAG, "Listener attached to alarm_status");
    }

    public void stopListening() {
        if (currentListener != null) {
            alarmStatusRef.removeEventListener(currentListener);
            currentListener = null;
            Log.d(TAG, "Listener detached");
        }
    }
}
