package com.darkeyes.tricks;

import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.SwitchPreference;

import java.io.File;

public class SettingsActivity extends PreferenceActivity {

    private Preference mForceDarkTheme = (SwitchPreference) findPreference("trick_forceDarkTheme");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSharedPreferences("com.darkeyes.tricks_preferences", MODE_PRIVATE);
        getPreferenceManager().setStorageDeviceProtected();

        addPreferencesFromResource(R.xml.pref_tricks);

        if (Build.VERSION.SDK_INT != 27) {
            getPreferenceScreen().removePreference(mForceDarkTheme);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        File mainFolder = new File("/data/user_de/0/com.darkeyes.tricks");
        if (mainFolder.exists()) {
            mainFolder.setReadable(true, false);
            mainFolder.setExecutable(true, false);
        }
        File prefFolder = new File("/data/user_de/0/com.darkeyes.tricks/shared_prefs");
        if (prefFolder.exists()) {
            prefFolder.setReadable(true, false);
            prefFolder.setExecutable(true, false);
        }
        File prefFile = new File("/data/user_de/0/com.darkeyes.tricks/shared_prefs/com.darkeyes.tricks_preferences.xml");
        if (prefFile.exists()) {
            prefFile.setReadable(true, false);
        }
    }
}