package com.darkeyes.tricks;

import com.darkeyes.tricks.features.DoubleTapStatusBarOrLockScreenSdk31AndHigher;
import com.darkeyes.tricks.features.Feature;
import com.darkeyes.tricks.features.QuickPullDownFeatureSdk31AndHigher;

public class FeatureFactory {

    private FeatureFactory() {
    }

    /**
     * Create instances of features if available.
     *
     * @param featureName the feature you want to instantiate. See {@link com.darkeyes.tricks.features.FeatureNames}
     * @return null if feature is not available other the feature you want.
     */
    public static Feature createFeature(final String featureName) {
        Feature feature = null;

        if (DoubleTapStatusBarOrLockScreenSdk31AndHigher.isPlatformSupported(featureName)) {
            feature = new DoubleTapStatusBarOrLockScreenSdk31AndHigher();
        } else if (QuickPullDownFeatureSdk31AndHigher.isPlatformSupported(featureName)) {
            feature = new QuickPullDownFeatureSdk31AndHigher();
        }
        return feature;
    }

    /**
     * Check if a feature is available on this android platform.
     *
     * @param featureName the feature you want to instantiate. See {@link com.darkeyes.tricks.features.FeatureNames}
     * @return true if available otherwise false
     */
    public static boolean hasFeature(final String featureName) {
        boolean hasFeature = DoubleTapStatusBarOrLockScreenSdk31AndHigher.isPlatformSupported(featureName);
        if (!hasFeature) {
            hasFeature = QuickPullDownFeatureSdk31AndHigher.isPlatformSupported(featureName);
        }
        return hasFeature;
    }
}
