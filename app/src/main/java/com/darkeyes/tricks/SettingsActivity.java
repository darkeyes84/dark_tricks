package com.darkeyes.tricks;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity implements OnSharedPreferenceChangeListener {
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
        prefs = getSharedPreferences("com.darkeyes.tricks_shared", Context.MODE_WORLD_READABLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        prefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        prefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Object pref = sharedPreferences.getAll().get(key);
        Intent intent = new Intent("com.darkeyes.tricks.PREFERENCES");
        intent.putExtra("preference", key);
        if (pref instanceof Boolean)
            intent.putExtra("value", sharedPreferences.getBoolean(key, false));
        else
            intent.putExtra("value", sharedPreferences.getString(key, key.equals("trick_customCarrierText") ? "" : "0"));
        sendBroadcast(intent);
    }
}