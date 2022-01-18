package com.aueb.hermes;

import android.app.AppOpsManager;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;

import androidx.appcompat.app.AppCompatActivity;

import com.aueb.hermes.presenter.MainPresenter;
import com.aueb.hermes.utils.InitFinishedReceiver;
import com.aueb.hermes.view.StatisticsDisplayActivity;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MainActivity extends AppCompatActivity {

    private InitFinishedReceiver mInitFinishedReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check whether the android.permission.PACKAGE_USAGE_STATS has been granted
        AppOpsManager appOps = (AppOpsManager) getSystemService(APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), getPackageName());
        if (mode != AppOpsManager.MODE_ALLOWED) {
            // Request the aforementioned permission
            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            startActivity(intent);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
        String lastStr = null;

        // On install
        SharedPreferences sharedPreferences = getSharedPreferences("Prefs", MODE_PRIVATE);
        boolean registered = sharedPreferences.getBoolean("registered", false);

        MainPresenter presenter = new MainPresenter(this, sharedPreferences);

        if (!registered) {
            presenter.registerDevice();

            // Set the device as registered
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("registered", true);

            //initialize constant values
            editor.putString("BACKEND_IP_ADDRESS", "192.168.68.110:8080");
            editor.putInt("TIME_SLOT_SIZE", 4);
            lastStr = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0).format(formatter);
            editor.putString("last", lastStr);
            editor.apply();
        }

        // Collect and send the device's data to the server
        lastStr = sharedPreferences.getString("last", null);
        //lastStr = LocalDateTime.now().withHour(2).withMinute(0).withSecond(0).withNano(0).format(formatter);
        LocalDateTime last;
        if (lastStr == null) {
            last = LocalDateTime.now();
        } else {
            last = LocalDateTime.parse(lastStr, formatter);
        }
        presenter.collectAndSendRawData(last);

        // Update the variable storing the date and time the server was informed
        // IMPORTANT NOTE: last holds the last beginning and not the last hour documented!
        last = LocalDateTime.now().minusHours(12).withMinute(0).withSecond(0).withNano(0);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("last", last.format(formatter));
        editor.apply();
    }

    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter intentFilter = new IntentFilter("android.intent.action.INIT_FINISHED");
        mInitFinishedReceiver = new InitFinishedReceiver();
        registerReceiver(mInitFinishedReceiver, intentFilter);

    }

    @Override
    protected void onPause() {
        super.onPause();

        unregisterReceiver(mInitFinishedReceiver);
    }
}