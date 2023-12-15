package com.darkeyes.tricks;

import android.content.Context;
import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

public class SettingsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        getPreferenceManager().setSharedPreferencesMode(Context.MODE_WORLD_READABLE);
        getPreferenceManager().setSharedPreferencesName("com.darkeyes.tricks_shared");
        setPreferencesFromResource(R.xml.pref_tricks, rootKey);
    }
}