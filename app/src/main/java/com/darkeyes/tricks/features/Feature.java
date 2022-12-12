package com.darkeyes.tricks.features;

import com.darkeyes.tricks.Utils;

import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * A feature is the implementation of an modification you want to do with Xposed.
 * <p>
 * Every class that implements {@link Feature} should also implement a static method
 * that determines if a feature is supported on which platforms. The static method
 * should have a signature like this:
 * <p>
 * <code>static boolean hasFeature(String featureName);</code>
 * </p>
 * This method should be used in the {@link com.darkeyes.tricks.FeatureFactory} to
 * actually create the feature if available.
 */
public interface Feature {
    void inject(XC_LoadPackage.LoadPackageParam param, XSharedPreferences pref, Utils utils);
}
