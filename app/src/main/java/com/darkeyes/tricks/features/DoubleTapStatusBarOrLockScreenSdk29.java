package com.darkeyes.tricks.features;

import android.content.Context;
import android.os.Build;
import android.os.PowerManager;
import android.view.MotionEvent;

import com.darkeyes.tricks.Utils;

import java.util.Objects;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

public class DoubleTapStatusBarOrLockScreenSdk29 extends DoubleTapStatusBarOrLockScreenSdk31AndHigher {

    public static boolean isPlatformSupported(final String featureName) {
        return Build.VERSION.SDK_INT == 29 // Android 10
                && (Objects.equals(featureName, FeatureNames.TRICK_DOUBLE_TAP_STATUSBAR)
                || Objects.equals(featureName, FeatureNames.TRICK_DOUBLE_TAP_LOCKSCREEN));
    }

    @Override
    public void inject(final XC_LoadPackage.LoadPackageParam param,
                       final XSharedPreferences pref,
                       final Utils utils) {
        findAndHookMethod("com.android.systemui.statusbar.phone.PanelView", param.classLoader, "onFinishInflate", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                final Context context = (Context) getObjectField(param.thisObject, "mContext");
                if (mPowerManager == null)
                    mPowerManager = context.getSystemService(PowerManager.class);
                registerGestureDetectorListener(param, context, mPowerManager);
            }
        });

        findAndHookMethod("com.android.systemui.statusbar.phone.NotificationPanelView", param.classLoader, "onTouchEvent", MotionEvent.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                fireOnTouchEventIfPossible((MotionEvent) param.args[0], pref);
            }
        });
    }
}
