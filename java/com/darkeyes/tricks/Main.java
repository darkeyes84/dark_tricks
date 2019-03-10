package com.darkeyes.tricks;

import android.content.Context;
import android.os.Build;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.getIntField;

public class Main implements IXposedHookLoadPackage {

    private static final File prefFile = new File("/data/user_de/0/com.darkeyes.tricks/shared_prefs/com.darkeyes.tricks_preferences.xml");
    private static XSharedPreferences pref;

    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam param) {

        pref = new XSharedPreferences(prefFile);

        if (param.packageName.equals("com.android.systemui")) {

            new ResourceProxy(param.packageName, new ResourceProxy.Interceptor() {
                @Override
                public List<String> getSupportedResourceNames() {
                    return Arrays.asList("config_hideLtePlus");
                }

                @Override
                public boolean onIntercept(ResourceProxy.ResourceSpec resourceSpec) {
                    switch(resourceSpec.name) {
                        case ("config_hideLtePlus") :
                            if (pref.getBoolean("trick_hideLtePlus", true)) {
                                resourceSpec.value = true;
                                return true;
                            }
                    }
                    return false;
                }
            });

            if (pref.getBoolean("trick_hideNextAlarm", true)) {
                findAndHookMethod("com.android.systemui.statusbar.phone.PhoneStatusBarPolicy", param.classLoader, "updateAlarm", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        param.setResult(null);
                    }
                });

                if (Build.VERSION.SDK_INT == 28) {
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
                }

                if (Build.VERSION.SDK_INT != 28) {
                    String CLASS = Build.VERSION.SDK_INT == 27 ? "com.android.systemui.qs.QSFooterImpl" : "com.android.systemui.qs.QSFooter";
                    findAndHookMethod(CLASS, param.classLoader, "onNextAlarmChanged", "android.app.AlarmManager.AlarmClockInfo", new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            param.setResult(null);
                        }
                    });

                    findAndHookMethod("com.android.keyguard.KeyguardStatusView", param.classLoader, "refreshAlarmStatus", "android.app.AlarmManager.AlarmClockInfo", new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            param.setResult(null);
                        }
                    });

                    findAndHookMethod("com.android.keyguard.KeyguardStatusView.Patterns", param.classLoader, "update", Context.class, boolean.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
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

            if (pref.getBoolean("trick_useKeyguardPhone", true) && (Build.VERSION.SDK_INT != 28)) {
                findAndHookMethod("com.android.systemui.statusbar.phone.KeyguardBottomAreaView", param.classLoader, "canLaunchVoiceAssist", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        param.setResult(false);
                    }
                });
            }

            if (pref.getBoolean("trick_navbarAlwaysRight", true) && (Build.VERSION.SDK_INT != 28)) {
                findAndHookMethod("com.android.systemui.statusbar.phone.NavigationBarInflaterView", param.classLoader, "setAlternativeOrder", boolean.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        param.args[0] = true;
                    }
                });
            }

            if (pref.getBoolean("trick_forceDarkTheme", true) && (Build.VERSION.SDK_INT == 27)) {
                findAndHookMethod("android.app.WallpaperColors", param.classLoader, "getColorHints", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        int colorHints = getIntField(param.thisObject, "mColorHints");
                        colorHints |= 1<<1;
                        param.setResult(colorHints);
                    }
                });


            }

        } else if (param.packageName.equals("android")) {

            if (pref.getBoolean("trick_navbarAlwaysRight", true) && (Build.VERSION.SDK_INT != 28)) {
                findAndHookMethod("com.android.server.policy.PhoneWindowManager", param.classLoader, "navigationBarPosition", int.class, int.class, int.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        param.args[2] = 1;
                    }
                });
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

            if (pref.getBoolean("trick_forceDarkTheme", true) && (Build.VERSION.SDK_INT == 27)) {
                findAndHookMethod("android.app.WallpaperColors", param.classLoader, "getColorHints", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        int colorHints = getIntField(param.thisObject, "mColorHints");
                        colorHints |= 1<<1;
                        param.setResult(colorHints);
                    }
                });
            }
        }
    }
}
