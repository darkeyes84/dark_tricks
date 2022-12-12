package com.darkeyes.tricks.features;

import android.os.Build;
import android.view.MotionEvent;
import android.view.ViewGroup;

import com.darkeyes.tricks.Utils;

import java.util.Objects;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static com.darkeyes.tricks.features.FeatureNames.TRICK_QUICK_PULLDOWN;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.getBooleanField;
import static de.robv.android.xposed.XposedHelpers.getIntField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setBooleanField;

public class QuickPullDownFeatureSdk31AndHigher implements Feature {
    private Object mNotificationPanelViewController;

    public static boolean isPlatformSupported(final String featureName) {
        return (Build.VERSION.SDK_INT >= 31)
                // && (Build.VERSION.SDK_INT <= 33)
                && Objects.equals(featureName, TRICK_QUICK_PULLDOWN);
    }

    @Override
    public void inject(final XC_LoadPackage.LoadPackageParam param,
                       final XSharedPreferences pref,
                       final Utils utils) {

        if (pref.getBoolean(TRICK_QUICK_PULLDOWN, true)) {

            final String notificationPanelViewController = utils.getComAndroidSystemui_NotificationPanelViewControllerClassName();
            findAndHookMethod(notificationPanelViewController, param.classLoader, "onFinishInflate", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    mNotificationPanelViewController = param.thisObject;
                }
            });

            if (Build.VERSION.SDK_INT >= 33) {
                findAndHookMethod("com.android.systemui.statusbar.phone.HeadsUpTouchHelper", param.classLoader, "onTouchEvent", MotionEvent.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        MotionEvent event = (MotionEvent) param.args[0];
                        if (getBooleanField(mNotificationPanelViewController, "mSplitShadeEnabled") &&
                                (boolean) callMethod(mNotificationPanelViewController, "touchXOutsideOfQs", event.getX()))
                            return;
                        ViewGroup view = (ViewGroup) getObjectField(mNotificationPanelViewController, "mView");
                        int state = (int) getObjectField(mNotificationPanelViewController, "mBarState");
                        int height = getIntField(mNotificationPanelViewController, "mStatusBarMinHeight");
                        boolean tracking = getBooleanField(param.thisObject, "mTrackingHeadsUp");

                        float w = view.getMeasuredWidth();
                        float x = event.getX();
                        float y = event.getY(event.getActionIndex());

                        if (x > 3.f * w / 4.f && state == 0 && !tracking && y < height) {
                            setBooleanField(mNotificationPanelViewController, "mQsExpandImmediate", true);
                            callMethod(mNotificationPanelViewController, "setShowShelfOnly", true);
                            String update = utils.isSecurityPatchAfterDecember2022() ? "updateExpandedHeightToMaxHeight" : "requestPanelHeightUpdate";
                            callMethod(mNotificationPanelViewController, update);
                            callMethod(mNotificationPanelViewController, "setListening", true);
                        }
                    }
                });
            } else {
                findAndHookMethod("com.android.systemui.statusbar.phone.NotificationPanelViewController", param.classLoader, "isOpenQsEvent", MotionEvent.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if ((boolean) param.getResult() == false) {
                            MotionEvent event = (MotionEvent) param.args[0];
                            ViewGroup view = (ViewGroup) getObjectField(param.thisObject, "mView");
                            int state = (int) getObjectField(param.thisObject, "mBarState");
                            float w = view.getMeasuredWidth();
                            float x = event.getX();

                            param.setResult(x > 3.f * w / 4.f && state == 0);
                        }
                    }
                });
            }
        }
    }
}
