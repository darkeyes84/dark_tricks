package com.darkeyes.tricks;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;

import java.io.File;

public class SettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {

    private EditTextPreference customCarrierText;
    private ListPreference cursorControl;
    private ListPreference lessNotifications;
    private PackageManager mPackageManager;
    private boolean mTorchAvailable;
    private SwitchPreference quickUnlock;
    private static SharedPreferences sp;
    private ListPreference gestureHeight;
    private PreferenceScreen prefScreen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sp = getSharedPreferences("com.darkeyes.tricks_preferences", Context.MODE_WORLD_READABLE);
        addPreferencesFromResource(R.xml.pref_tricks);

        prefScreen = (PreferenceScreen) findPreference("prefScreen");
        SwitchPreference forceDarkTheme = (SwitchPreference) findPreference("trick_forceDarkTheme");
        SwitchPreference useKeyguardPhone = (SwitchPreference) findPreference("trick_useKeyguardPhone");
        SwitchPreference navbarAlwaysRight = (SwitchPreference) findPreference("trick_navbarAlwaysRight");
        SwitchPreference hideBuildVersion = (SwitchPreference) findPreference("trick_hideBuildVersion");
        SwitchPreference powerTorch = (SwitchPreference) findPreference("trick_powerTorch");
        SwitchPreference doubleTapStatusBar = (SwitchPreference) findPreference("trick_doubleTapStatusBar");
        SwitchPreference doubleTapLockScreen = (SwitchPreference) findPreference("trick_doubleTapLockScreen");
        quickUnlock = (SwitchPreference) findPreference("trick_quickUnlock");
        SwitchPreference batteryEstimate = (SwitchPreference) findPreference("trick_batteryEstimate");
        customCarrierText = (EditTextPreference) findPreference("trick_customCarrierText");
        cursorControl = (ListPreference) findPreference("trick_cursorControl");
        lessNotifications = (ListPreference) findPreference("trick_lessNotifications");
        SwitchPreference smallClock = (SwitchPreference) findPreference("trick_smallClock");
        gestureHeight = (ListPreference) findPreference("trick_gestureHeight");
        SwitchPreference expandedNotifications = (SwitchPreference) findPreference("trick_expandedNotifications");

        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        updateSummary();

        if (Build.VERSION.SDK_INT != 27) {
            prefScreen.removePreference(forceDarkTheme);
        }
        if (Build.VERSION.SDK_INT >= 28) {
            prefScreen.removePreference(useKeyguardPhone);
        }
        if (Build.VERSION.SDK_INT >= 29) {
            prefScreen.removePreference(navbarAlwaysRight);
        }
        if (Build.VERSION.SDK_INT < 29) {
            prefScreen.removePreference(hideBuildVersion);
            prefScreen.removePreference(lessNotifications);
        }
        if (Build.VERSION.SDK_INT < 31) {
            prefScreen.removePreference(doubleTapStatusBar);
            prefScreen.removePreference(doubleTapLockScreen);
            prefScreen.removePreference(quickUnlock);
            prefScreen.removePreference(batteryEstimate);
            prefScreen.removePreference(smallClock);
            prefScreen.removePreference(gestureHeight);
            prefScreen.removePreference(expandedNotifications);
        }
        if (Build.VERSION.SDK_INT != 31) {
            prefScreen.removePreference(smallClock);
        }
        if (!torchAvailable()) {
            prefScreen.removePreference(powerTorch);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        updateSummary();
    }

    @Override
    protected void onPause() {
        super.onPause();

        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);

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

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        updateSummary();
    }

    private void updateSummary() {
        Resources resources = getResources();
        int resourceId = resources.getIdentifier("config_navBarInteractionMode", "integer", "android");
        if (resources.getInteger(resourceId) != 2)
            prefScreen.removePreference(gestureHeight);

        String carrierText = customCarrierText.getText();
        if (carrierText == null || carrierText.isEmpty()) {
            customCarrierText.setSummary("Default");
        } else if (carrierText.trim().isEmpty()) {
            customCarrierText.setSummary("Empty");
        } else {
            customCarrierText.setSummary(carrierText);
        }

        cursorControl.setSummary(cursorControl.getEntry());
        lessNotifications.setSummary(lessNotifications.getEntry());
        if ("Everywhere".equals(gestureHeight.getEntry()))
            gestureHeight.setSummary("Swipe everywhere for back gesture");
        else
            gestureHeight.setSummary("Swipe below " + gestureHeight.getEntry() + " for back gesture");

        boolean checked = sp.getBoolean("trick_quickUnlock", false);
        quickUnlock.setChecked(checked);
        int passwordLength = sp.getInt("passwordLength", -1);
        quickUnlock.setEnabled(passwordLength > 0);
        quickUnlock.setSummary(passwordLength > 0
                ? "Automatically unlock the device when the correct PIN/password is entered"
                : "Unavailable, recreate your PIN/password");
    }

    private boolean torchAvailable() {
        if (mPackageManager == null) {
            mPackageManager = getPackageManager();
            try {
                mTorchAvailable = mPackageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
            } catch (Throwable t) {
                mTorchAvailable = false;
            }
        }
        return mTorchAvailable;
    }

    public static class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (sp == null)
                sp = context.getSharedPreferences("com.darkeyes.tricks_preferences", Context.MODE_WORLD_READABLE);
            String action = intent.getAction();
            Bundle extras = intent.getExtras();

            if ("com.darkeyes.tricks.SET_INTEGER".equals(action)) {
                setInteger(extras);
            } else if ("com.darkeyes.tricks.SET_BOOLEAN".equals(action)) {
                setBoolean(extras);
            }
        }

        private static void setInteger(Bundle extras) {
            String preference = extras.getString("preference");
            int value = extras.getInt("value");
            sp.edit().putInt(preference, value).commit();
        }

        private static void setBoolean(Bundle extras) {
            String preference = extras.getString("preference");
            boolean value = extras.getBoolean("value");
            sp.edit().putBoolean(preference, value).commit();
        }
    }
}