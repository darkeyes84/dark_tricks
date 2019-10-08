package com.darkeyes.tricks;

import android.content.Context;
import android.graphics.Rect;
import android.os.Build;

import java.io.File;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.getIntField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setBooleanField;
import static de.robv.android.xposed.XposedHelpers.setIntField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;

public class Main implements IXposedHookLoadPackage {

    private static final File prefFile = new File("/data/user_de/0/com.darkeyes.tricks/shared_prefs/com.darkeyes.tricks_preferences.xml");
    private static XSharedPreferences pref;
    int mRotation;
    Object mObject;

    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam param) {

        pref = new XSharedPreferences(prefFile);

        if (param.packageName.equals("com.android.systemui")) {

            if (pref.getBoolean("trick_hideLtePlus", true) || pref.getBoolean("trick_show4gForLte", false)) {

                findAndHookMethod("com.android.systemui.statusbar.policy.NetworkControllerImpl.Config", param.classLoader, "readConfig", "android.content.Context", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (pref.getBoolean("trick_hideLtePlus", true)) {
                            setBooleanField(param.getResult(), "hideLtePlus", true);
                        } else {
                            setBooleanField(param.getResult(), "hideLtePlus", false);
                        }
                        if (pref.getBoolean("trick_show4gForLte", false)) {
                            setBooleanField(param.getResult(), "show4gForLte", true);
                        } else {
                            setBooleanField(param.getResult(), "show4gForLte", false);
                        }
                    }
                });
            }

            if (pref.getBoolean("trick_hideNextAlarm", true)) {
                findAndHookMethod("com.android.systemui.statusbar.phone.PhoneStatusBarPolicy", param.classLoader, "updateAlarm", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        param.setResult(null);
                    }
                });

                if (Build.VERSION.SDK_INT >= 28) {
                    findAndHookMethod("com.android.systemui.qs.QuickStatusBarHeader", param.classLoader, "onNextAlarmChanged", "android.app.AlarmManager.AlarmClockInfo", new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            param.setResult(null);
                        }
                    });

                    findAndHookMethod("com.android.systemui.keyguard.KeyguardSliceProvider", param.classLoader, "onNextAlarmChanged", "android.app.AlarmManager.AlarmClockInfo", new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            param.setResult(null);
                        }
                    });
                } else {
                    String CLASS = Build.VERSION.SDK_INT == 27 ? "com.android.systemui.qs.QSFooterImpl" : "com.android.systemui.qs.QSFooter";
                    findAndHookMethod(CLASS, param.classLoader, "onNextAlarmChanged", "android.app.AlarmManager.AlarmClockInfo", new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            param.setResult(null);
                        }
                    });

                    findAndHookMethod("com.android.keyguard.KeyguardStatusView", param.classLoader, "refreshAlarmStatus", "android.app.AlarmManager.AlarmClockInfo", new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            param.setResult(null);
                        }
                    });

                    findAndHookMethod("com.android.keyguard.KeyguardStatusView.Patterns", param.classLoader, "update", Context.class, boolean.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            param.args[1] = false;
                        }
                    });
                }
            }

            if (pref.getBoolean("trick_hideVpn", true)) {

                findAndHookMethod("com.android.systemui.statusbar.policy.SecurityControllerImpl", param.classLoader, "isVpnEnabled", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        param.setResult(false);
                    }
                });

                findAndHookMethod("com.android.systemui.statusbar.policy.SecurityControllerImpl", param.classLoader, "getPrimaryVpnName", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        param.setResult(null);
                    }
                });
            }

            if (pref.getBoolean("trick_hideCert", true)) {

                findAndHookMethod("com.android.systemui.statusbar.policy.SecurityControllerImpl", param.classLoader, "hasCACertInCurrentUser", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        param.setResult(false);
                    }
                });
            }

            if (pref.getBoolean("trick_useKeyguardPhone", true) && (Build.VERSION.SDK_INT < 28)) {
                findAndHookMethod("com.android.systemui.statusbar.phone.KeyguardBottomAreaView", param.classLoader, "canLaunchVoiceAssist", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        param.setResult(false);
                    }
                });
            }

            if (pref.getBoolean("trick_navbarAlwaysRight", true)) {
                findAndHookMethod("com.android.systemui.statusbar.phone.NavigationBarInflaterView", param.classLoader, "setAlternativeOrder", boolean.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        param.args[0] = true;
                    }
                });

                if ((Build.VERSION.SDK_INT >= 28)) {
                    findAndHookMethod("com.android.systemui.util.leak.RotationUtils", param.classLoader, "getRotation", "android.content.Context", new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if ((int)param.getResult() == 2) {
                                param.setResult(1);
                            }
                        }
                    });

                    findAndHookMethod("com.android.systemui.util.leak.RotationUtils", param.classLoader, "getExactRotation", "android.content.Context", new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if ((int)param.getResult() == 2) {
                                param.setResult(1);
                            }
                        }
                    });
                }
            }

            if (pref.getBoolean("trick_forceDarkTheme", true) && (Build.VERSION.SDK_INT == 27)) {
                findAndHookMethod("android.app.WallpaperColors", param.classLoader, "getColorHints", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        int colorHints = getIntField(param.thisObject, "mColorHints");
                        colorHints |= 1<<1;
                        param.setResult(colorHints);
                    }
                });
            }

            if (pref.getBoolean("trick_hideBuildVersion", true)) {

                if (Build.VERSION.SDK_INT == 29) {
                    findAndHookMethod("com.android.systemui.qs.QSFooterImpl", param.classLoader, "setBuildText", new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            param.setResult(null);
                        }
                    });
                }
            }

            findAndHookMethod("com.android.systemui.qs.QSCarrier", param.classLoader, "setCarrierText", CharSequence.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    String carrierText = pref.getString("trick_customCarrierText", "");
                    if (carrierText != null && !carrierText.isEmpty()) {
                        param.args[0] = carrierText.trim().isEmpty() ? "" : carrierText;
                    }
                }
            });

            findAndHookMethod("com.android.keyguard.CarrierTextController", param.classLoader, "postToCallback", "com.android.keyguard.CarrierTextController.CarrierTextCallbackInfo", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    String carrierText = pref.getString("trick_customCarrierText", "");
                    if (carrierText != null && !carrierText.isEmpty()) {
                        setObjectField(param.args[0], "carrierText",
                                carrierText.trim().isEmpty() ? "" : carrierText);
                    }
                }
            });

        } else if (param.packageName.equals("android")) {
            if (pref.getBoolean("trick_navbarAlwaysRight", true)) {
                if (Build.VERSION.SDK_INT == 28) {

                    findAndHookMethod("com.android.server.wm.DisplayFrames", param.classLoader, "onDisplayInfoUpdated", "android.view.DisplayInfo", "com.android.server.wm.utils.WmDisplayCutout", new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            mObject = param.thisObject;
                            mRotation = getIntField(mObject, "mRotation");
                        }
                    });

                    findAndHookMethod("com.android.server.policy.PhoneWindowManager", param.classLoader, "layoutNavigationBar", "com.android.server.wm.DisplayFrames", int.class, "android.graphics.Rect", boolean.class, boolean.class, boolean.class, boolean.class, new XC_MethodHook() {
                        boolean mTampered = false;

                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (mRotation == 3) {
                                setIntField(mObject, "mRotation", 1);
                                mTampered = true;
                            }
                        }

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (mTampered == true) {
                                setIntField(mObject, "mRotation", 3);
                                mTampered = false;
                            }
                        }
                    });

                    findAndHookMethod("com.android.server.policy.PhoneWindowManager", param.classLoader, "getNonDecorInsetsLw", int.class, int.class, int.class, "android.view.DisplayCutout", "android.graphics.Rect", new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if ((int) param.args[0] == 3) {
                                param.args[0] = 1;
                            }
                        }
                    });

                    findAndHookMethod("com.android.server.policy.PhoneWindowManager", param.classLoader, "isDockSideAllowed", int.class, int.class, int.class, int.class, int.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if ((int) param.args[4] == 3) {
                                param.args[4] = 1;
                            }
                        }
                    });

                } else if (Build.VERSION.SDK_INT < 28) {

                    findAndHookMethod("com.android.server.policy.PhoneWindowManager", param.classLoader, "navigationBarPosition", int.class, int.class, int.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            param.args[2] = 1;
                        }
                    });
                }
            }

            if (pref.getBoolean("trick_hideAdbNotification", true)) {
                findAndHookMethod("com.android.server.usb.UsbDeviceManager$UsbHandler", param.classLoader, "updateAdbNotification", boolean.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        param.setResult(null);
                    }
                });
            }

        } else if (param.packageName.equals("com.google.android.apps.nexuslauncher")) {
            if (pref.getBoolean("trick_navbarAlwaysRight", true) && (Build.VERSION.SDK_INT == 28)) {
                findAndHookMethod("com.android.launcher3.DeviceProfile", param.classLoader, "updateIsSeascape", "android.view.WindowManager", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        param.setResult(false);
                    }
                });

                findAndHookMethod("com.android.quickstep.OtherActivityTouchConsumer", param.classLoader, "isNavBarOnRight", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        int mDisplayRotation = getIntField(param.thisObject, "mDisplayRotation");
                        Rect mStableInsets = (Rect)getObjectField(param.thisObject, "mStableInsets");
                        param.setResult((mDisplayRotation == 1 || mDisplayRotation == 3) && mStableInsets.right > 0);
                    }
                });

                findAndHookMethod("com.android.quickstep.OtherActivityTouchConsumer", param.classLoader, "isNavBarOnLeft", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        param.setResult(false);
                    }
                });
            }

            if (pref.getBoolean("trick_forceDarkTheme", true) && (Build.VERSION.SDK_INT == 27)) {
                findAndHookMethod("android.app.WallpaperColors", param.classLoader, "getColorHints", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        int colorHints = getIntField(param.thisObject, "mColorHints");
                        colorHints |= 1<<1;
                        param.setResult(colorHints);
                    }
                });
            }
        }
    }
}
