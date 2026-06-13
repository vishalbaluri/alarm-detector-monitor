package com.skylark.detectormonitor;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.RouteInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.media.AudioAttributes;
import android.media.MediaPlayer;

import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.skylark.detectormonitor.adapter.DeviceAdapter;
import com.skylark.detectormonitor.firebase.FirebaseHelper;
import com.skylark.detectormonitor.model.DetectorData;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "DetectorMonitor";

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .build();

    private static final String ESP32_IP = "192.168.4.1";

    private final BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateWifiStatus();
        }
    };

    private TextView textViewStatus;


    private View        ivLed;
    private TextView    tvLedStatus;
    private TextView    tvBatVoltage;
    private ProgressBar pbBattery;
    private TextView    tvDevid;
    private TextView    tvStatus;
    private TextView    tvBuzzer;

    private TextView tvTest;
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private Switch swBuzzer;

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private Switch sw_test;

    // ── Animations ────────────────────────────────────────────────────────────
    private Animation blinkGreen;
    private Animation blinkRed;

    private MediaPlayer alertPlayer;
    private boolean isAlertPlaying = false;
    private Vibrator vibrator;

    // ── Room names (must match the device_location values the firmware sends) ─
    // The spinner shows exactly these strings, and they are compared directly
    // against the device_location field returned by Firebase.
    private final List<String> roomNames = new ArrayList<>();

    // The room label currently selected in the spinner
    private String selectedRoom = "";
    // Guard against duplicate onItemSelected callbacks Android sometimes fires
    private int lastSpinnerPos = -1;

    // ── Firebase ──────────────────────────────────────────────────────────────
    private FirebaseHelper firebaseHelper;

    // ── Firebase listener ─────────────────────────────────────────────────────
    //
    // The firmware writes everything to a single alarm_status node.
    // device_location tells us which room's sensor sent the update.
    //
    // Filter logic:
    //   • incoming device_location == selectedRoom  → show the data
    //   • incoming device_location != selectedRoom  → show "waiting" message
    //   • snapshot does not exist                   → show "no data" message
    //
    private final ValueEventListener alarmListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot snapshot) {

            if (!snapshot.exists()) {
                showWaiting("No data in database");
                return;
            }

            // Read every field from the snapshot
            DetectorData data = new DetectorData();
            data.devId       = getVal(snapshot, "device_id");
            data.devLocation = getVal(snapshot, "device_location");
            data.holderName  = getVal(snapshot, "holder_name");
            data.houseNumber = getVal(snapshot, "house_number");
            data.location    = getVal(snapshot, "location");

            DataSnapshot mon = snapshot.child("MONDATA");
            data.led        = getVal(mon, "L01");
            data.battery = getVal(mon, "D62");
            data.buzzer     = getVal(mon, "B01");
            data.Switch = getVal(mon, "S01");

            Log.d(TAG, "Firebase update → device_location=\"" + data.devLocation
                    + "\"  selectedRoom=\"" + selectedRoom + "\"");

            // ── KEY FIX ───────────────────────────────────────────────────────
            // Only display the data when the incoming device_location matches
            // the room currently selected in the spinner.
            // If it doesn't match the user sees a clear "waiting" message.
            if (selectedRoom.equalsIgnoreCase(data.devLocation)) {
                updateUI(data);
            } else {
                // Data exists but belongs to a different room
                showWaiting("Waiting for data from: " + selectedRoom
                        + "\n(Server has: " + data.devLocation + ")");
            }
        }

        @Override
        public void onCancelled(@NonNull DatabaseError error) {
            Log.w(TAG, "Listener cancelled: " + error.getMessage());
            tvStatus.setText(R.string.connection_error);
        }
    };

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Room names must match exactly the device_location values the firmware
        // sends (case-insensitive comparison is used in the filter above).
        roomNames.add("Kitchen");
        roomNames.add("Living Room");
        roomNames.add("Bedroom");

        firebaseHelper = new FirebaseHelper();

        textViewStatus = findViewById(R.id.textViewStatus);
        Button buttonSettings = findViewById(R.id.buttonSettings);
        buttonSettings.setOnClickListener(this::showSettingsMenu);

        // Vibrator setup (handles both old and new Android API)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ (API 31+)
            VibratorManager vm = (VibratorManager) getSystemService(VIBRATOR_MANAGER_SERVICE);
            vibrator = vm.getDefaultVibrator();
        } else {
            // Android 11 and below
            vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        }

        blinkGreen = AnimationUtils.loadAnimation(this, R.anim.blink_green);
        blinkRed   = AnimationUtils.loadAnimation(this, R.anim.blink_red);

        // ── UI ────────────────────────────────────────────────────────────────────
        Spinner spinnerRoom = findViewById(R.id.spinner_room);
        ivLed        = findViewById(R.id.iv_led);
        tvLedStatus  = findViewById(R.id.tv_led_status);
        tvBatVoltage = findViewById(R.id.tv_bat_voltage);
        pbBattery    = findViewById(R.id.pb_battery);
        swBuzzer     = findViewById(R.id.sw_buzzer);
        tvDevid      = findViewById(R.id.tv_devid);
        tvStatus     = findViewById(R.id.tv_status);
        tvBuzzer     = findViewById(R.id.tv_buzzer);
        tvTest = findViewById(R.id.tv_test);
        sw_test = findViewById(R.id.sw_test);

        DeviceAdapter adapter = new DeviceAdapter(this, R.layout.item_device, roomNames);
        spinnerRoom.setAdapter(adapter);

        spinnerRoom.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {

                // Ignore duplicate callbacks (Android quirk on resume/redraw)
                if (position == lastSpinnerPos) return;
                lastSpinnerPos = position;

                selectedRoom = roomNames.get(position);
                Log.d(TAG, "Selected room: " + selectedRoom);

                // Clear stale data immediately so user knows room switched
                clearUI(selectedRoom);

                // The listener is already attached to alarm_status.
                // No need to restart it — the next Firebase event (or the
                // cached one) will be re-evaluated against the new selectedRoom.
                //
                // Force re-evaluation by re-attaching (triggers immediate callback
                // with the cached value so the user sees a result right away).
                firebaseHelper.startListening(alarmListener);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    private void showSettingsMenu(View anchor) {
        PopupMenu popupMenu = new PopupMenu(this, anchor);
        popupMenu.getMenu().add("Find Device");
        popupMenu.getMenu().add("Connect WIFI");

        popupMenu.setOnMenuItemClickListener(item -> {
            String selected = Objects.requireNonNull(item.getTitle()).toString();
            switch (selected) {
                case "Find Device":
                    startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                    break;
                case "Connect WIFI":
                    showWifiDialog();
                    break;
            }
            return true;
        });

        popupMenu.show();
    }

    private void updateWifiStatus() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (cm == null) return;
        Network network = cm.getActiveNetwork();
        LinkProperties props = cm.getLinkProperties(network);
        boolean connected = false;
        if (props != null) {
            for (RouteInfo route : props.getRoutes()) {
                if ("192.168.4.1".equals(route.getGateway() != null ? route.getGateway().getHostAddress() : null)) {
                    connected = true;
                    break;
                }
            }
        }
        final boolean status = connected;
        runOnUiThread(() -> textViewStatus.setText(status ? "Status: Connected to Device" : "Status:"));
    }

    private void showWifiDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Connect to Router");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText ssidInput = new EditText(this);
        ssidInput.setHint("Enter SSID");
        ssidInput.setPadding(20, 20, 20, 20);

        final EditText passInput = new EditText(this);
        passInput.setHint("Enter Password");
        passInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passInput.setPadding(20, 20, 20, 20);

        layout.addView(ssidInput);
        layout.addView(passInput);
        builder.setView(layout);

        builder.setPositiveButton("Next", null);
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String ssid = ssidInput.getText().toString().trim();
            String password = passInput.getText().toString().trim();
            if (ssid.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both SSID and Password", Toast.LENGTH_SHORT).show();
            } else {
                showConfirmationDialog(ssid, password, dialog);
            }
        });
    }

    private void showConfirmationDialog(String ssid, String password, AlertDialog parentDialog) {
        String message = "Are you sure you want to connect to:\n\nSSID: " + ssid +
                "\nPassword: " + password + "\n\nContinue?";

        new AlertDialog.Builder(this)
                .setTitle("Confirm Connection")
                .setMessage(message)
                .setPositiveButton("Connect", (d, w) -> {
                    parentDialog.dismiss();
                    sendCredentialsToESP32(ssid, password);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void sendCredentialsToESP32(String ssid, String password) {
        String url = "http://" + ESP32_IP + "/connect?ssid=" + ssid + "&pass=" + password;

        Toast.makeText(this, "Sending credentials to ESP32...", Toast.LENGTH_SHORT).show();

        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        Toast.makeText(MainActivity.this,
                                "✅ Wi-Fi credentials sent successfully!\nConnecting to " + ssid + "...",
                                Toast.LENGTH_LONG).show();
                        new Handler().postDelayed(() -> checkConnectionToNetwork(ssid), 4000);
                    } else {
                        Toast.makeText(MainActivity.this,
                                "ESP32 returned error: " + response.code(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void checkConnectionToNetwork(String expectedSsid) {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null) {
            Toast.makeText(this, "Wi-Fi not supported", Toast.LENGTH_LONG).show();
            return;
        }
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo != null) {
            String currentSsid = wifiInfo.getSSID();
            if (currentSsid != null && currentSsid.replace("\"", "").equals(expectedSsid)) {
                Toast.makeText(this, "✅ Connected to " + expectedSsid, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "⚠️ Not connected to " + expectedSsid, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        firebaseHelper.startListening(alarmListener);
        registerReceiver(wifiReceiver, new android.content.IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
        updateWifiStatus();
    }

    @Override
    protected void onPause() {
        super.onPause();
        firebaseHelper.stopListening();
        unregisterReceiver(wifiReceiver);
        stopAlert();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAlert();
    }

    // ── UI states ─────────────────────────────────────────────────────────────

    /**
     * Clear all sensor indicators when the user switches rooms.
     * Shows "Connecting…" so the user knows a fresh result is on the way.
     */
    @SuppressLint({"UseCompatLoadingForDrawables", "SetTextI18n"})
    private void clearUI(String roomLabel) {
        stopAlert();
        ivLed.clearAnimation();
        ivLed.setBackgroundResource(R.drawable.circle_red);
        tvLedStatus.setText("—");
        tvLedStatus.setTextColor(getColor(R.color.text_secondary));
        pbBattery.setProgress(0);
        tvBatVoltage.setText("--");
        tvBuzzer.setText("—");
        tvBuzzer.setTextColor(getColor(R.color.text_secondary));
        tvBuzzer.setBackground(getDrawable(R.drawable.bg_buzzer_off));
        tvTest.setText("--");
        tvTest.setTextColor(getColor(R.color.text_secondary));
        tvTest.setBackground(getDrawable(R.drawable.bg_buzzer_off));
        swBuzzer.setChecked(false);
        sw_test.setChecked(false);

        tvDevid.setText("Room: " + roomLabel);
        tvStatus.setText("Connecting…");
    }

    /**
     * Show a human-readable waiting/mismatch message without touching the
     * sensor widgets (they keep showing their last state or the cleared state).
     */
    @SuppressLint("UseCompatLoadingForDrawables")
    private void showWaiting(String message) {
        stopAlert();
        tvStatus.setText(message);
        // Dim the sensor widgets to signal "data not current"
        ivLed.clearAnimation();
        ivLed.setBackgroundResource(R.drawable.circle_red);
        tvLedStatus.setText("—");
        tvLedStatus.setTextColor(getColor(R.color.text_secondary));
        pbBattery.setProgress(0);
        tvBatVoltage.setText("--");
        tvBuzzer.setText("—");
        tvBuzzer.setTextColor(getColor(R.color.text_secondary));
        tvBuzzer.setBackground(getDrawable(R.drawable.bg_buzzer_off));
        tvTest.setText("—");
        tvTest.setTextColor(getColor(R.color.text_secondary));
        tvTest.setBackground(getDrawable(R.drawable.bg_buzzer_off));
        swBuzzer.setChecked(false);
        sw_test.setChecked(false);
    }

    /** Populate all sensor widgets with live data. */
    @SuppressLint({"SetTextI18n", "DefaultLocale", "UseCompatLoadingForDrawables"})
    private void updateUI(DetectorData data) {

        boolean buzzerOn = parseFlag(data.buzzer);
        boolean switchOn = parseFlag(data.Switch);

        // =====================================================
        // MODULE STATUS LOGIC
        // B01=01 & S01=00 -> RED
        // Otherwise       -> GREEN
        // =====================================================

        boolean moduleRed = buzzerOn && !switchOn;

        ivLed.clearAnimation();

        if (moduleRed) {

            ivLed.setBackgroundResource(R.drawable.circle_red);
            ivLed.startAnimation(blinkRed);

            tvLedStatus.setText("RED - Fault");
            tvLedStatus.setTextColor(
                    getColor(R.color.led_red));

        } else {

            ivLed.setBackgroundResource(R.drawable.circle_green);
            ivLed.startAnimation(blinkGreen);

            tvLedStatus.setText("GREEN");
            tvLedStatus.setTextColor(
                    getColor(R.color.led_green));
        }

        // =====================================================
        // BATTERY
        // =====================================================

        try {

            float voltage = Float.parseFloat(data.battery.trim());

            float MAX_VOLTS = 3.3f;
            float MIN_VOLTS = 0.0f;

            int percent = Math.round(
                    ((voltage - MIN_VOLTS)
                            / (MAX_VOLTS - MIN_VOLTS)) * 100f);

            percent = Math.max(0, Math.min(100, percent));

            pbBattery.setProgress(percent);
            tvBatVoltage.setText(
                    String.format("%.2fV (%d%%)",
                            voltage,
                            percent));

            if (percent <= 20) {

                pbBattery.setProgressTintList(
                        android.content.res.ColorStateList.valueOf(
                                Color.parseColor("#F44336")));

            } else if (percent <= 50) {

                pbBattery.setProgressTintList(
                        android.content.res.ColorStateList.valueOf(
                                Color.parseColor("#FF9800")));

            } else {

                pbBattery.setProgressTintList(
                        android.content.res.ColorStateList.valueOf(
                                Color.parseColor("#28A745")));
            }

        } catch (Exception e) {

            pbBattery.setProgress(0);
            tvBatVoltage.setText("--");

            pbBattery.setProgressTintList(
                    android.content.res.ColorStateList.valueOf(
                            Color.parseColor("#F44336")));
        }

        // =====================================================
        // TEST SWITCH
        // =====================================================

        tvTest.setText(
                switchOn
                        ? "ON — Active"
                        : "OFF — Silent");

        tvTest.setTextColor(
                switchOn
                        ? Color.RED
                        : getColor(R.color.text_secondary));

        tvTest.setBackground(
                getDrawable(
                        switchOn
                                ? R.drawable.bg_buzzer_on
                                : R.drawable.bg_buzzer_off));

        sw_test.setChecked(switchOn);

        // =====================================================
        // BUZZER
        // =====================================================

        tvBuzzer.setText(
                buzzerOn
                        ? "ON — Active"
                        : "OFF — Silent");

        tvBuzzer.setTextColor(
                buzzerOn
                        ? Color.RED
                        : getColor(R.color.text_secondary));

        tvBuzzer.setBackground(
                getDrawable(
                        buzzerOn
                                ? R.drawable.bg_buzzer_on
                                : R.drawable.bg_buzzer_off));

        swBuzzer.setChecked(buzzerOn);

        // =====================================================
        // FOOTER
        // =====================================================

        String roomLabel =
                data.devLocation.isEmpty()
                        ? selectedRoom
                        : data.devLocation;

        tvDevid.setText(
                data.devId + "  •  " + roomLabel);

        String holder =
                data.holderName.isEmpty()
                        ? ""
                        : data.holderName + " • ";

        tvStatus.setText(
                holder + data.location + "  ●  Live");

        // =====================================================
        // ALERT
        // =====================================================

        if (buzzerOn || switchOn) {
            startAlert();
        } else {
            stopAlert();
        }
    }
    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean parseFlag(String value) {
        if (value == null || value.isEmpty()) return false;
        try {
            return Integer.parseInt(value.trim()) != 0;
        } catch (NumberFormatException e) {
            return "1".equals(value.trim());
        }
    }

    private String getVal(DataSnapshot snap, String key) {
        Object v = snap.child(key).getValue();
        return v != null ? v.toString() : "";
    }

    /**
     * Starts a looping alarm sound if not already playing.
     * Uses the system default alarm ringtone.
     */
    private void startAlert() {
        if (isAlertPlaying) return;

        try {
            // MediaPlayer.create() handles prepare() internally — don't call it again
            alertPlayer = MediaPlayer.create(this, R.raw.alarm_sound);

            if (alertPlayer == null) {
                Log.e(TAG, "MediaPlayer.create() returned null — check res/raw/alarm_sound.mp3");
                return;
            }

            alertPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
            );
            alertPlayer.setLooping(true);
            alertPlayer.start();
            isAlertPlaying = true;
            Log.d(TAG, "Alert sound started");

        } catch (Exception e) {
            Log.e(TAG, "Failed to start alert: " + e.getMessage());
        }

        startVibration();
    }
    /**
     * Stops and releases the alert sound.
     */
    private void stopAlert() {
        if (alertPlayer != null) {
            try {
                if (alertPlayer.isPlaying()) alertPlayer.stop();
                alertPlayer.release();
            } catch (Exception e) {
                Log.e(TAG, "Error stopping alert: " + e.getMessage());
            }
            alertPlayer = null;
        }
        isAlertPlaying = false;
        Log.d(TAG, "Alert sound stopped");
        stopVibration();
    }

    /**
     * Repeating vibration pattern: 0ms delay → 500ms ON → 500ms OFF → repeat
     * Pattern: { delay, vibrate, sleep, vibrate, sleep, ... }
     */
    private void startVibration() {
        if (vibrator == null || !vibrator.hasVibrator()) return;

        long[] pattern = { 0, 500, 500 }; // delay=0, on=500ms, off=500ms

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // API 26+
            vibrator.vibrate(
                    VibrationEffect.createWaveform(pattern, 0) // 0 = repeat from index 0
            );
        } else {
            vibrator.vibrate(pattern, 0); // 0 = repeat from index 0
        }
    }

    private void stopVibration() {
        if (vibrator != null) {
            vibrator.cancel();
        }
    }
}
