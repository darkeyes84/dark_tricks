package com.darkeyes.tricks;

import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;

import java.io.File;

public class SettingsActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSharedPreferences("com.darkeyes.tricks_preferences", MODE_PRIVATE);
        getPreferenceManager().setStorageDeviceProtected();

        addPreferencesFromResource(R.xml.pref_tricks);

        PreferenceScreen prefScreen = (PreferenceScreen) findPreference("prefScreen");
        SwitchPreference navbarAlwaysRight = (SwitchPreference) findPreference("trick_navbarAlwaysRight");
        SwitchPreference forceDarkTheme = (SwitchPreference) findPreference("trick_forceDarkTheme");
        SwitchPreference useKeyguardPhone = (SwitchPreference) findPreference("trick_useKeyguardPhone");

        if (Build.VERSION.SDK_INT != 27) {
            prefScreen.removePreference(forceDarkTheme);
        }
        if (Build.VERSION.SDK_INT == 28) {
            prefScreen.removePreference(useKeyguardPhone);
            prefScreen.removePreference(navbarAlwaysRight);
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