package com.darkeyes.tricks;

import static android.content.Intent.FLAG_RECEIVER_FOREGROUND;

import android.app.AndroidAppHelper;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.inputmethodservice.InputMethodService;
import android.media.AudioManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.service.notification.StatusBarNotification;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.ArrayMap;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.File;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedBridge.invokeOriginalMethod;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getAdditionalInstanceField;
import static de.robv.android.xposed.XposedHelpers.getBooleanField;
import static de.robv.android.xposed.XposedHelpers.getFloatField;
import static de.robv.android.xposed.XposedHelpers.getIntField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setAdditionalInstanceField;
import static de.robv.android.xposed.XposedHelpers.setBooleanField;
import static de.robv.android.xposed.XposedHelpers.setIntField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;

public class Main implements IXposedHookZygoteInit, IXposedHookLoadPackage {

    private static final File prefFile = new File("/data/user_de/0/com.darkeyes.tricks/shared_prefs/com.darkeyes.tricks_preferences.xml");
    private static XSharedPreferences pref;
    private int mRotation;
    private Object mObject;
    private String carrierText;
    private String mCameraId;
    private InputMethodService mService;
    private boolean mVolumeLongPress;
    private boolean mPowerLongPress;
    private boolean mTorchEnabled;
    private AudioManager mAudioManager;
    private CameraManager mCameraManager;
    private CameraManager.TorchCallback mTorchCallback;
    private PowerManager mPowerManager;
    private Context mContext;
    private Handler mHandler;
    private SensorEventListener mProximityListener;
    private SensorManager mSensorManager;
    private Sensor mProximitySensor;
    private PowerManager.WakeLock mProximityWakeLock;
    private Context mWakeUpContext;
    private Handler mWakeUpHandler;
    private TelephonyManager mTelephonyManager;
    private SensorEventListener mWakeUpListener;
    private PowerManager.WakeLock mWakeUpWakeLock;
    private int MSG_WAKE_UP = 100;
    private ArrayMap<String, Long> mLastTimestamps = new ArrayMap<>();
    private long mDownTime = 0L;
    private boolean mCameraGesture;
    private GestureDetector mDoubleTapGesture;
    private Object mNotificationPanelViewController;
    private int mStatusBarHeight = 0;
    private int mStatusBarHeaderHeight = 0;
    private long mLastDownEvent = 0L;
    private Object mLockPatterUtils;
    private Object mLockCallback;
    private Object mKeyguardMonitor;
    private ClassLoader classLoader;
    private Runnable mWakeUp;

    public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) {

        if (XposedBridge.getXposedVersion() < 93)
            pref = new XSharedPreferences(prefFile);
        else
            pref = new XSharedPreferences("com.darkeyes.tricks");

        int cursorControl = Integer.parseInt(pref.getString("trick_cursorControl", "0"));
        if (cursorControl != 0) {

            findAndHookMethod("android.inputmethodservice.InputMethodService", null, "onCreate", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    mService = (InputMethodService) param.thisObject;
                }
            });

            findAndHookMethod("android.inputmethodservice.InputMethodService", null, "onKeyDown", int.class, KeyEvent.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    int keyCode = ((KeyEvent) param.args[1]).getKeyCode();
                    if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                        if (mService.isInputViewShown()) {
                            int newKeyCode = (cursorControl == 1) ?
                                    KeyEvent.KEYCODE_DPAD_LEFT : KeyEvent.KEYCODE_DPAD_RIGHT;
                            mService.sendDownUpKeyEvents(newKeyCode);
                            param.setResult(true);
                        } else
                            param.setResult(false);
                    } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                        if (mService.isInputViewShown()) {
                            int newKeyCode = (cursorControl == 1) ?
                                    KeyEvent.KEYCODE_DPAD_RIGHT : KeyEvent.KEYCODE_DPAD_LEFT;
                            mService.sendDownUpKeyEvents(newKeyCode);
                            param.setResult(true);
                        } else
                            param.setResult(false);
                    }
                }
            });

            findAndHookMethod("android.inputmethodservice.InputMethodService", null, "onKeyUp", int.class, KeyEvent.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    int keyCode = ((KeyEvent) param.args[1]).getKeyCode();
                    if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP)
                        param.setResult(mService.isInputViewShown());

                }
            });
        }
    }

    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam param) {

        if (param.packageName.equals("com.android.systemui")) {
            classLoader = param.classLoader;
            String CLASS = Build.VERSION.SDK_INT <= 30 ? "com.android.systemui.statusbar.policy.NetworkControllerImpl.Config"
                    : "com.android.settingslib.mobile.MobileMappings.Config";
            findAndHookMethod(CLASS, param.classLoader, "readConfig", "android.content.Context", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    setBooleanField(param.getResult(), "hideLtePlus", pref.getBoolean("trick_hideLtePlus", true));
                    setBooleanField(param.getResult(), "show4gForLte", pref.getBoolean("trick_show4gForLte", false));
                }
            });

            if (pref.getBoolean("trick_hideNextAlarm", true)) {
                findAndHookMethod("com.android.systemui.statusbar.phone.PhoneStatusBarPolicy", param.classLoader, "updateAlarm", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        param.setResult(null);
                    }
                });

                if (Build.VERSION.SDK_INT >= 28) {
                    if (Build.VERSION.SDK_INT <= 30) {
                        findAndHookMethod("com.android.systemui.qs.QuickStatusBarHeader", param.classLoader, "onNextAlarmChanged", "android.app.AlarmManager.AlarmClockInfo", new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) {
                                param.setResult(null);
                            }
                        });
                    } else {
                        findAndHookMethod("com.google.android.systemui.smartspace.KeyguardZenAlarmViewController", param.classLoader, "updateNextAlarm", new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) {
                                param.setResult(null);
                            }
                        });
                    }

                    findAndHookMethod("com.android.systemui.keyguard.KeyguardSliceProvider", param.classLoader, "onNextAlarmChanged", "android.app.AlarmManager.AlarmClockInfo", new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            param.setResult(null);
                        }
                    });

                } else {
                    String FOOTER = Build.VERSION.SDK_INT == 27 ? "com.android.systemui.qs.QSFooterImpl" : "com.android.systemui.qs.QSFooter";
                    findAndHookMethod(FOOTER, param.classLoader, "onNextAlarmChanged", "android.app.AlarmManager.AlarmClockInfo", new XC_MethodHook() {
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

            if (pref.getBoolean("trick_useKeyguardPhone", true) && Build.VERSION.SDK_INT < 28) {
                findAndHookMethod("com.android.systemui.statusbar.phone.KeyguardBottomAreaView", param.classLoader, "canLaunchVoiceAssist", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        param.setResult(false);
                    }
                });
            }

            if (pref.getBoolean("trick_navbarAlwaysRight", true) && Build.VERSION.SDK_INT < 29) {
                findAndHookMethod("com.android.systemui.statusbar.phone.NavigationBarInflaterView", param.classLoader, "setAlternativeOrder", boolean.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        param.args[0] = true;
                    }
                });

                if (Build.VERSION.SDK_INT == 28) {
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

            if (pref.getBoolean("trick_forceDarkTheme", true) && Build.VERSION.SDK_INT == 27) {
                findAndHookMethod("android.app.WallpaperColors", param.classLoader, "getColorHints", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        int colorHints = getIntField(param.thisObject, "mColorHints");
                        colorHints |= 1<<1;
                        param.setResult(colorHints);
                    }
                });
            }

            if (pref.getBoolean("trick_hideBuildVersion", true) && Build.VERSION.SDK_INT >= 29) {
                String FOOTER = Build.VERSION.SDK_INT <= 30 ? "com.android.systemui.qs.QSFooterImpl" : "com.android.systemui.qs.QSFooterView";
                findAndHookMethod(FOOTER, param.classLoader, "setBuildText", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        param.setResult(null);
                    }
                });
            }

            carrierText = pref.getString("trick_customCarrierText", "");
            if (carrierText != null && !carrierText.isEmpty()) {
                if (Build.VERSION.SDK_INT >= 33) {
                    findAndHookMethod("com.android.systemui.qs.carrier.QSCarrier", param.classLoader, "updateState", "com.android.systemui.qs.carrier.CellSignalState", boolean.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            TextView mCarrierText = (TextView) getObjectField(param.thisObject, "mCarrierText");
                            mCarrierText.setText(carrierText.trim().isEmpty() ? "" : carrierText);
                        }
                    });
                } else {
                    String CARRIER = Build.VERSION.SDK_INT >= 30 ? "com.android.systemui.qs.carrier.QSCarrier" : "com.android.systemui.qs.QSCarrier";
                    findAndHookMethod(CARRIER, param.classLoader, "setCarrierText", CharSequence.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            param.args[0] = carrierText.trim().isEmpty() ? "" : carrierText;
                        }
                    });
                }

                String CARRIERTEXT = Build.VERSION.SDK_INT <= 30 ? "com.android.keyguard.CarrierTextController" : "com.android.keyguard.CarrierTextManager";
                findAndHookMethod(CARRIERTEXT, param.classLoader, "postToCallback", CARRIERTEXT + ".CarrierTextCallbackInfo", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        setObjectField(param.args[0], "carrierText", carrierText.trim().isEmpty() ? "" : carrierText);
                    }
                });
            }

            if ((pref.getBoolean("trick_doubleTapStatusBar", false) || (pref.getBoolean("trick_doubleTapLockScreen", false)))
                    && Build.VERSION.SDK_INT >= 31) {
                if (Build.VERSION.SDK_INT >= 33) {
                    //TODO
                } else {
                    findAndHookMethod("com.android.systemui.statusbar.phone.NotificationPanelViewController", param.classLoader, "onFinishInflate", new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            mNotificationPanelViewController = param.thisObject;
                            mStatusBarHeight = getIntField(param.thisObject, "mStatusBarMinHeight");
                            mStatusBarHeaderHeight = getIntField(param.thisObject, "mStatusBarHeaderHeightKeyguard");
                            View view = (View) getObjectField(param.thisObject, "mView");
                            if (mPowerManager == null)
                                mPowerManager = (PowerManager) getObjectField(param.thisObject, "mPowerManager");
                            if (mDoubleTapGesture == null) {
                                mDoubleTapGesture = new GestureDetector(view.getContext(),
                                        new GestureDetector.SimpleOnGestureListener() {
                                            @Override
                                            public boolean onDoubleTap(MotionEvent e) {
                                                callMethod(mPowerManager, "goToSleep", e.getEventTime());
                                                return true;
                                            }
                                        });
                            }
                        }
                    });

                    findAndHookMethod("com.android.systemui.statusbar.phone.PanelViewController$TouchHandler", param.classLoader, "onTouch", View.class, MotionEvent.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (param.args[0].getClass().getName().equals("com.android.systemui.statusbar.phone.NotificationPanelView")
                                    && mNotificationPanelViewController != null && mDoubleTapGesture != null) {
                                MotionEvent event = (MotionEvent) param.args[1];
                                boolean isExpanded = getBooleanField(mNotificationPanelViewController, "mQsExpanded");
                                boolean isPulsing = getBooleanField(mNotificationPanelViewController, "mPulsing");
                                boolean isDozing = getBooleanField(mNotificationPanelViewController, "mDozing");
                                boolean isKeyguard = getIntField(mNotificationPanelViewController, "mBarState") == 1
                                        && !isPulsing && !isDozing;
                                boolean isStatusBar = event.getY() < mStatusBarHeight && !isExpanded;
                                if ((isKeyguard && pref.getBoolean("trick_doubleTapLockScreen", false))
                                        || (isStatusBar && pref.getBoolean("trick_doubleTapStatusBar", false)))
                                    mDoubleTapGesture.onTouchEvent(event);
                            }
                        }
                    });

                    if (pref.getBoolean("trick_doubleTapLockScreen", false)) {
                        findAndHookMethod("com.android.systemui.statusbar.DragDownHelper", param.classLoader, "onInterceptTouchEvent", MotionEvent.class, new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) {
                                MotionEvent event = (MotionEvent) param.args[0];
                                long time = event.getEventTime();
                                View host = (View) getObjectField(param.thisObject, "host");
                                if (mPowerManager == null)
                                    mPowerManager = (PowerManager) host.getContext().getSystemService(Context.POWER_SERVICE);
                                if (event.getActionMasked() == MotionEvent.ACTION_DOWN
                                        && event.getY() < mStatusBarHeaderHeight) {
                                    if (time - mLastDownEvent < 300) {
                                        callMethod(mPowerManager, "goToSleep", time);
                                    }
                                    mLastDownEvent = event.getEventTime();
                                }
                            }
                        });
                    }

                    if (pref.getBoolean("trick_doubleTapStatusBar", false)) {
                        findAndHookMethod("com.android.systemui.statusbar.phone.PanelViewController", param.classLoader, "startOpening", MotionEvent.class, new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) {
                                param.setResult(null);
                            }
                        });
                    }
                }
            }

            if (pref.getBoolean("trick_quickUnlock", false) && Build.VERSION.SDK_INT >= 31) {
                findAndHookMethod("com.android.keyguard.KeyguardPasswordViewController", param.classLoader, "onViewAttached", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        pref.reload();
                        mLockPatterUtils = getObjectField(param.thisObject, "mLockPatternUtils");
                        mLockCallback = getObjectField(param.thisObject, "mKeyguardSecurityCallback");
                        mKeyguardMonitor = getObjectField(param.thisObject, "mKeyguardUpdateMonitor");
                    }
                });

                findAndHookMethod("com.android.keyguard.KeyguardPinViewController", param.classLoader, "onViewAttached", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        pref.reload();
                        mLockPatterUtils = getObjectField(param.thisObject, "mLockPatternUtils");
                        mLockCallback = getObjectField(param.thisObject, "mKeyguardSecurityCallback");
                        mKeyguardMonitor = getObjectField(param.thisObject, "mKeyguardUpdateMonitor");
                    }
                });

                findAndHookMethod("com.android.keyguard.KeyguardPasswordView", param.classLoader, "onFinishInflate", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        TextView passwordEntry = (TextView) getObjectField(param.thisObject, "mPasswordEntry");
                        passwordEntry.addTextChangedListener(new TextWatcher() {
                            @Override
                            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                            }

                            @Override
                            public void onTextChanged(CharSequence s, int start, int before, int count) {
                            }

                            @Override
                            public void afterTextChanged(Editable s) {
                                String entry = passwordEntry.getText().toString();
                                int passwordLength = pref.getInt("passwordLength", -1);
                                if (entry.length() == passwordLength) {
                                    int userId = (int) callMethod(mKeyguardMonitor, "getCurrentUser");
                                    Class<?> credential = findClass("com.android.internal.widget.LockscreenCredential", classLoader);
                                    Object password = callStaticMethod(credential, "createPassword", entry);
                                    boolean valid = (boolean) callMethod(mLockPatterUtils,
                                            "checkCredential", password, userId, (Object) null);
                                    if (valid) {
                                        callMethod(mLockCallback, "reportUnlockAttempt", userId, true, 0);
                                        callMethod(mLockCallback, "dismiss", true, userId);
                                    }
                                }
                            }
                        });
                    }
                });

                if (Build.VERSION.SDK_INT >= 33) {
                    //TODO
                } else {
                    findAndHookMethod("com.android.keyguard.PasswordTextView", param.classLoader, "append", char.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            String entry = (String) getObjectField(param.thisObject, "mText");
                            int passwordLength = pref.getInt("passwordLength", -1);
                            if (entry.length() == passwordLength) {
                                int userId = (int) callMethod(mKeyguardMonitor, "getCurrentUser");
                                Class<?> credential = findClass("com.android.internal.widget.LockscreenCredential", classLoader);
                                Object pin = callStaticMethod(credential, "createPin", entry);
                                boolean valid = (boolean) callMethod(mLockPatterUtils,
                                        "checkCredential", pin, userId, (Object) null);
                                if (valid) {
                                    callMethod(mLockCallback, "reportUnlockAttempt", userId, true, 0);
                                    callMethod(mLockCallback, "dismiss", true, userId);
                                }
                            }
                        }
                    });
                }

                findAndHookMethod("com.android.internal.widget.LockPatternUtils", param.classLoader, "throwIfCalledOnMainThread", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        param.setResult(null);
                    }
                });
            }

            if (pref.getBoolean("trick_batteryEstimate", false) && Build.VERSION.SDK_INT >= 31) {
                if (Build.VERSION.SDK_INT >= 33) {
                    findAndHookMethod("com.android.systemui.qs.QuickStatusBarHeader", param.classLoader, "onMeasure", int.class, int.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            Object mBattery = getObjectField(param.thisObject, "mBatteryRemainingIcon");
                            float expansion = getFloatField(param.thisObject, "mKeyguardExpansionFraction");
                            int mode = getIntField(mBattery, "mShowPercentMode");
                            if ((expansion == 0f && mode != 1) || (expansion == 1f && mode != 3)) {
                                callMethod(mBattery, "setPercentShowMode", expansion == 0f ? 1 : 3);
                                callMethod(mBattery, "updatePercentText");
                            }
                        }
                    });
                } else {
                    findAndHookMethod("com.android.systemui.qs.QuickStatusBarHeader", param.classLoader, "updateBatteryMode", new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            param.setResult(null);
                        }
                    });

                    findAndHookMethod("com.android.systemui.qs.QuickStatusBarHeader", param.classLoader, "setExpansion", boolean.class, float.class, float.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            Object mBattery = getObjectField(param.thisObject, "mBatteryRemainingIcon");
                            int mode = getIntField(mBattery, "mShowPercentMode");
                            float expansion = (boolean) param.args[0] ? 1f : (float) param.args[1];
                            if ((expansion == 0f && mode != 1) || (expansion == 1f && mode != 3)) {
                                callMethod(mBattery, "setPercentShowMode", expansion == 0f ? 1 : 3);
                                callMethod(mBattery, "updatePercentText");
                            }
                        }
                    });
                }
            }

            if (pref.getBoolean("trick_smallClock", false) && Build.VERSION.SDK_INT == 31) {
                findAndHookMethod("com.android.keyguard.KeyguardClockSwitch", param.classLoader, "animateClockChange", boolean.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        param.args[0] = false;
                    }
                });
            }

            int gestureHeight = Integer.parseInt(pref.getString("trick_gestureHeight", "0"));
            if (gestureHeight != 0) {
                if (Build.VERSION.SDK_INT >= 33) {
                    //TODO
                } else {
                    findAndHookMethod("com.android.systemui.navigationbar.gestural.EdgeBackGestureHandler", param.classLoader, "isWithinInsets", int.class, int.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            float bottom = getFloatField(param.thisObject, "mBottomGestureHeight");
                            Point displaySize = (Point) getObjectField(param.thisObject, "mDisplaySize");
                            int height;
                            if (gestureHeight == 1)
                                height = (2 * displaySize.y) / 3;
                            else
                                height = displaySize.y / 3;

                            if ((int) param.args[1] < (displaySize.y - bottom - height))
                                param.setResult(false);
                        }
                    });
                }
            }

            if (pref.getBoolean("trick_quickPulldown", true) && Build.VERSION.SDK_INT >= 31) {
                if (Build.VERSION.SDK_INT >= 33) {
                    //TODO
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

        } else if (param.packageName.equals("android")) {
            if (pref.getBoolean("trick_navbarAlwaysRight", true) && Build.VERSION.SDK_INT < 29) {
                if (Build.VERSION.SDK_INT == 28) {

                    findAndHookMethod("com.android.server.wm.DisplayFrames", param.classLoader, "onDisplayInfoUpdated", "android.view.DisplayInfo", "com.android.server.wm.utils.WmDisplayCutout", new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            mObject = param.thisObject;
                            mRotation = getIntField(mObject, "mRotation");
                        }
                    });

                    findAndHookMethod("com.android.server.policy.PhoneWindowManager", param.classLoader, "layoutNavigationBar", "com.android.server.wm.DisplayFrames", int.class, "android.graphics.Rect", boolean.class, boolean.class, boolean.class, boolean.class, new XC_MethodHook() {
                        boolean tampered = false;

                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (mRotation == 3) {
                                setIntField(mObject, "mRotation", 1);
                                tampered = true;
                            }
                        }

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (tampered) {
                                setIntField(mObject, "mRotation", 3);
                                tampered = false;
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

                } else {
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

            if (pref.getBoolean("trick_skipTrack", true) || pref.getBoolean("trick_powerTorch", false)) {
                findAndHookMethod("com.android.server.policy.PhoneWindowManager", param.classLoader, "init", Context.class, "android.view.IWindowManager", "com.android.server.policy.WindowManagerPolicy.WindowManagerFuncs", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (pref.getBoolean("trick_skipTrack", true)) {
                            Runnable mVolumeUpLongPress = () -> {
                                mVolumeLongPress = true;
                                Intent keyIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, null);
                                KeyEvent keyEvent = new KeyEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT, 0);
                                keyIntent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent);
                                mAudioManager.dispatchMediaKeyEvent(keyEvent);

                                keyEvent = KeyEvent.changeAction(keyEvent, KeyEvent.ACTION_UP);
                                keyIntent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent);
                                mAudioManager.dispatchMediaKeyEvent(keyEvent);
                                callMethod(param.thisObject, "performHapticFeedback", new Class<?>[]{int.class, boolean.class, String.class}, HapticFeedbackConstants.LONG_PRESS, false, null);
                            };

                            Runnable mVolumeDownLongPress = () -> {
                                mVolumeLongPress = true;
                                Intent keyIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, null);
                                KeyEvent keyEvent = new KeyEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS, 0);
                                keyIntent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent);
                                mAudioManager.dispatchMediaKeyEvent(keyEvent);

                                keyEvent = KeyEvent.changeAction(keyEvent, KeyEvent.ACTION_UP);
                                keyIntent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent);
                                mAudioManager.dispatchMediaKeyEvent(keyEvent);
                                callMethod(param.thisObject, "performHapticFeedback", new Class<?>[]{int.class, boolean.class, String.class}, HapticFeedbackConstants.LONG_PRESS, false, null);
                            };

                            setAdditionalInstanceField(param.thisObject, "mVolumeUpLongPress", mVolumeUpLongPress);
                            setAdditionalInstanceField(param.thisObject, "mVolumeDownLongPress", mVolumeDownLongPress);
                        }

                        if (pref.getBoolean("trick_powerTorch", false)) {
                            if (mTorchCallback == null) {
                                mTorchCallback = new CameraManager.TorchCallback() {
                                    @Override
                                    public void onTorchModeChanged(String cameraId, boolean enabled) {
                                        mTorchEnabled = enabled;
                                        if (mProximityListener != null) {
                                            mSensorManager.unregisterListener(mProximityListener);
                                            mProximityListener = null;
                                        }
                                    }

                                    @Override
                                    public void onTorchModeUnavailable(String cameraId) {
                                        mTorchEnabled = false;
                                        if (mProximityListener != null) {
                                            mSensorManager.unregisterListener(mProximityListener);
                                            mProximityListener = null;
                                        }
                                    }
                                };
                            }

                            Runnable mPowerDownLongPress = () -> {
                                if (!mTorchEnabled) {
                                    synchronized (mProximityWakeLock) {
                                        mProximityListener = new SensorEventListener() {
                                            @Override
                                            public void onSensorChanged(SensorEvent event) {
                                                if (mProximityWakeLock.isHeld())
                                                    mProximityWakeLock.release();
                                                if (mProximityListener != null) {
                                                    mSensorManager.unregisterListener(mProximityListener);
                                                    mProximityListener = null;
                                                }
                                                if (event.values[0] >= mProximitySensor.getMaximumRange()) {
                                                    mPowerLongPress = true;
                                                    callMethod(param.thisObject, "performHapticFeedback", new Class<?>[]{int.class, boolean.class, String.class}, HapticFeedbackConstants.LONG_PRESS, false, null);

                                                    try {
                                                        mCameraManager.setTorchMode(mCameraId, !mTorchEnabled);
                                                        mTorchEnabled = !mTorchEnabled;
                                                    } catch (Exception e) {
                                                    }
                                                }
                                            }

                                            @Override
                                            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                                            }
                                        };
                                    }
                                    mSensorManager.registerListener(mProximityListener,
                                            mProximitySensor, SensorManager.SENSOR_DELAY_FASTEST);
                                } else {
                                    mPowerLongPress = true;
                                    callMethod(param.thisObject, "performHapticFeedback", new Class<?>[]{int.class, boolean.class, String.class}, HapticFeedbackConstants.LONG_PRESS, false, null);

                                    try {
                                        mCameraManager.setTorchMode(mCameraId, !mTorchEnabled);
                                        mTorchEnabled = !mTorchEnabled;
                                    } catch (Exception e) {
                                    }
                                }
                                if (mProximityWakeLock.isHeld())
                                    mProximityWakeLock.release();
                            };

                            setAdditionalInstanceField(param.thisObject, "mPowerDownLongPress", mPowerDownLongPress);
                        }
                    }
                });

                findAndHookMethod("com.android.server.policy.PhoneWindowManager", param.classLoader, "interceptKeyBeforeQueueing", KeyEvent.class, int.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        KeyEvent event = (KeyEvent) param.args[0];
                        int keyCode = event.getKeyCode();
                        mHandler = (Handler) getObjectField(param.thisObject, "mHandler");
                        mContext = (Context) getObjectField(param.thisObject, "mContext");

                        if (mPowerManager == null)
                            mPowerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);

                        if (pref.getBoolean("trick_skipTrack", true)) {
                            Runnable mVolumeUpLongPress = (Runnable) getAdditionalInstanceField(param.thisObject, "mVolumeUpLongPress");
                            Runnable mVolumeDownLongPress = (Runnable) getAdditionalInstanceField(param.thisObject, "mVolumeDownLongPress");
                            if (mAudioManager == null)
                                mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);

                            if ((keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
                                    keyCode == KeyEvent.KEYCODE_VOLUME_UP) &&
                                    (event.getFlags() & KeyEvent.FLAG_FROM_SYSTEM) != 0 &&
                                    !mPowerManager.isInteractive() &&
                                    (mAudioManager.isMusicActive() || (boolean) callMethod(mAudioManager, "isMusicActiveRemotely"))) {
                                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                                    if (event.getRepeatCount() == 0) {
                                        mVolumeLongPress = false;
                                        mHandler.postDelayed(keyCode == KeyEvent.KEYCODE_VOLUME_UP ? mVolumeUpLongPress :
                                                mVolumeDownLongPress, ViewConfiguration.getLongPressTimeout());
                                    }
                                } else {
                                    mHandler.removeCallbacks(mVolumeUpLongPress);
                                    mHandler.removeCallbacks(mVolumeDownLongPress);

                                    if (!mVolumeLongPress) {
                                        mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                                                keyCode == KeyEvent.KEYCODE_VOLUME_UP ?
                                                        AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER, 0);
                                    }
                                }
                                param.setResult(0);
                            }
                        }

                        if (pref.getBoolean("trick_powerTorch", false)) {
                            if (mCameraManager == null) {
                                mCameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
                                mCameraManager.registerTorchCallback(mTorchCallback, mHandler);
                                try {
                                    String[] ids = mCameraManager.getCameraIdList();
                                    for (String id : ids) {
                                        CameraCharacteristics c = mCameraManager.getCameraCharacteristics(id);
                                        Boolean flashAvailable = c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                                        Integer lensFacing = c.get(CameraCharacteristics.LENS_FACING);
                                        if (flashAvailable != null && flashAvailable && lensFacing != null &&
                                                lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                                            mCameraId = id;
                                        }
                                    }
                                } catch (Exception e) {
                                }
                            }

                            if (keyCode == KeyEvent.KEYCODE_POWER && mPowerManager.isInteractive()) {
                                mDownTime = event.getEventTime();
                            }

                            if (keyCode == KeyEvent.KEYCODE_POWER && ((!mPowerManager.isInteractive() &&
                                    (event.getEventTime() - mDownTime > 300)) || mTorchEnabled) && event.getSource() != InputDevice.SOURCE_UNKNOWN) {
                                if (mSensorManager == null)
                                    mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
                                if (mProximitySensor == null)
                                    mProximitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
                                if (mProximityWakeLock == null)
                                    mProximityWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DarkTricks:TorchWakeLock");

                                Runnable mPowerDownLongPress = (Runnable) getAdditionalInstanceField(param.thisObject, "mPowerDownLongPress");
                                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                                    if (event.getRepeatCount() == 0) {
                                        mPowerLongPress = false;
                                        mProximityWakeLock.acquire(5000L /*5 seconds*/);
                                        mHandler.postDelayed(mPowerDownLongPress, ViewConfiguration.getLongPressTimeout());
                                    }
                                } else {
                                    mHandler.removeCallbacks(mPowerDownLongPress);

                                    if (mPowerLongPress) {
                                        mPowerLongPress = false;
                                    } else {
                                        mHandler.post(() -> {
                                            callMethod(mContext.getSystemService(Context.INPUT_SERVICE), "injectInputEvent", new KeyEvent(event.getEventTime(), event.getEventTime(), KeyEvent.ACTION_DOWN,
                                                    KeyEvent.KEYCODE_POWER, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, KeyEvent.FLAG_FROM_SYSTEM, InputDevice.SOURCE_UNKNOWN), 0);
                                            callMethod(mContext.getSystemService(Context.INPUT_SERVICE), "injectInputEvent", new KeyEvent(event.getEventTime(), event.getEventTime(), KeyEvent.ACTION_UP,
                                                    KeyEvent.KEYCODE_POWER, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, KeyEvent.FLAG_FROM_SYSTEM, InputDevice.SOURCE_UNKNOWN), 0);
                                        });
                                    }
                                    if (mProximityWakeLock.isHeld())
                                        mProximityWakeLock.release();
                                }
                                param.setResult(0);
                            }
                        }

                    }
                });
            }

            if (pref.getBoolean("trick_proximityWakeUp", true)) {
                if (Build.VERSION.SDK_INT >= 33) {
                    findAndHookMethod("com.android.server.power.PowerManagerService", param.classLoader, "systemReady", new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            mWakeUpContext = (Context) getObjectField(param.thisObject, "mContext");
                            mWakeUpHandler = (Handler) getObjectField(param.thisObject, "mHandler");
                            if (mSensorManager == null)
                                mSensorManager = (SensorManager) mWakeUpContext.getSystemService(Context.SENSOR_SERVICE);
                            if (mProximitySensor == null)
                                mProximitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
                            if (mPowerManager == null)
                                mPowerManager = (PowerManager) mWakeUpContext.getSystemService(Context.POWER_SERVICE);
                            if (mTelephonyManager == null)
                                mTelephonyManager = (TelephonyManager) mWakeUpContext.getSystemService(Context.TELEPHONY_SERVICE);
                            if (mWakeUpWakeLock == null)
                                mWakeUpWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DarkTricks:WakeUpWakelock");
                        }
                    });
                } else {
                    findAndHookMethod("com.android.server.power.PowerManagerService", param.classLoader, "systemReady", "com.android.internal.app.IAppOpsService", new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            mWakeUpContext = (Context) getObjectField(param.thisObject, "mContext");
                            mWakeUpHandler = (Handler) getObjectField(param.thisObject, "mHandler");
                            if (mSensorManager == null)
                                mSensorManager = (SensorManager) mWakeUpContext.getSystemService(Context.SENSOR_SERVICE);
                            if (mProximitySensor == null)
                                mProximitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
                            if (mPowerManager == null)
                                mPowerManager = (PowerManager) mWakeUpContext.getSystemService(Context.POWER_SERVICE);
                            if (mTelephonyManager == null)
                                mTelephonyManager = (TelephonyManager) mWakeUpContext.getSystemService(Context.TELEPHONY_SERVICE);
                            if (mWakeUpWakeLock == null)
                                mWakeUpWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DarkTricks:WakeUpWakelock");
                        }
                    });
                }

                if (Build.VERSION.SDK_INT <= 30) {
                    findAndHookMethod("com.android.server.power.PowerManagerService", param.classLoader, "wakeUpInternal", long.class, int.class, String.class, int.class, String.class, int.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {

                            mWakeUp = () -> {
                                long ident = Binder.clearCallingIdentity();
                                try {
                                    invokeOriginalMethod(param.method, param.thisObject, param.args);
                                } catch (Throwable t) {
                                } finally {
                                    Binder.restoreCallingIdentity(ident);
                                }
                            };

                            if (mTelephonyManager.getCallState() != TelephonyManager.CALL_STATE_RINGING && !param.args[2].equals("android.policy:BIOMETRIC")) {
                                if (mWakeUpHandler.hasMessages(MSG_WAKE_UP)) {
                                    param.setResult(null);
                                    return;
                                }
                                Message msg = mWakeUpHandler.obtainMessage(MSG_WAKE_UP);
                                mWakeUpHandler.sendMessageDelayed(msg, 100);

                                synchronized (mWakeUpWakeLock) {
                                    mWakeUpWakeLock.acquire(5000L /*5 seconds*/);
                                    mWakeUpListener = new SensorEventListener() {
                                        @Override
                                        public void onSensorChanged(SensorEvent event) {
                                            if (mWakeUpWakeLock.isHeld())
                                                mWakeUpWakeLock.release();
                                            if (mWakeUpListener != null) {
                                                mSensorManager.unregisterListener(mWakeUpListener);
                                                mWakeUpListener = null;
                                            }
                                            if (!mWakeUpHandler.hasMessages(MSG_WAKE_UP)) {
                                                param.setResult(null);
                                                return;
                                            }
                                            mWakeUpHandler.removeMessages(MSG_WAKE_UP);
                                            if (event.values[0] >= mProximitySensor.getMaximumRange()) {
                                                mCameraGesture = true;
                                                mWakeUp.run();
                                            } else
                                                mCameraGesture = false;
                                        }

                                        @Override
                                        public void onAccuracyChanged(Sensor sensor, int accuracy) {
                                        }
                                    };
                                }
                                mSensorManager.registerListener(mWakeUpListener,
                                        mProximitySensor, SensorManager.SENSOR_DELAY_FASTEST);
                                param.setResult(null);
                            }
                        }
                    });
                } else if (Build.VERSION.SDK_INT <= 32) {
                    findAndHookMethod("com.android.server.power.PowerManagerService", param.classLoader, "wakeDisplayGroup", int.class, long.class, int.class, String.class, int.class, String.class, int.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {

                            mWakeUp = () -> {
                                long ident = Binder.clearCallingIdentity();
                                try {
                                    invokeOriginalMethod(param.method, param.thisObject, param.args);
                                } catch (Throwable ignored) {
                                } finally {
                                    Binder.restoreCallingIdentity(ident);
                                }
                            };

                            if (mTelephonyManager.getCallState() != TelephonyManager.CALL_STATE_RINGING && !param.args[2].equals("android.policy:BIOMETRIC")) {
                                if (mWakeUpHandler.hasMessages(MSG_WAKE_UP)) {
                                    param.setResult(null);
                                    return;
                                }
                                Message msg = mWakeUpHandler.obtainMessage(MSG_WAKE_UP);
                                mWakeUpHandler.sendMessageDelayed(msg, 100);

                                synchronized (mWakeUpWakeLock) {
                                    mWakeUpWakeLock.acquire(5000L /*5 seconds*/);
                                    mWakeUpListener = new SensorEventListener() {
                                        @Override
                                        public void onSensorChanged(SensorEvent event) {
                                            if (mWakeUpWakeLock.isHeld())
                                                mWakeUpWakeLock.release();
                                            if (mWakeUpListener != null) {
                                                mSensorManager.unregisterListener(mWakeUpListener);
                                                mWakeUpListener = null;
                                            }
                                            if (!mWakeUpHandler.hasMessages(MSG_WAKE_UP)) {
                                                param.setResult(null);
                                                return;
                                            }
                                            mWakeUpHandler.removeMessages(MSG_WAKE_UP);
                                            if (event.values[0] >= mProximitySensor.getMaximumRange()) {
                                                mCameraGesture = true;
                                                mWakeUp.run();
                                            } else
                                                mCameraGesture = false;
                                        }

                                        @Override
                                        public void onAccuracyChanged(Sensor sensor, int accuracy) {
                                        }
                                    };
                                }
                                mSensorManager.registerListener(mWakeUpListener,
                                        mProximitySensor, SensorManager.SENSOR_DELAY_FASTEST);
                                param.setResult(null);
                            }
                        }
                    });
                } else {
                    findAndHookMethod("com.android.server.power.PowerGroup", param.classLoader, "wakeUpLocked", long.class, int.class, String.class, int.class, String.class, int.class, "com.android.internal.util.LatencyTracker", new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            mWakeUp = () -> {
                                long ident = Binder.clearCallingIdentity();
                                try {
                                    invokeOriginalMethod(param.method, param.thisObject, param.args);
                                } catch (Throwable ignored) {
                                } finally {
                                    Binder.restoreCallingIdentity(ident);
                                }
                            };

                            if (mTelephonyManager.getCallState() != TelephonyManager.CALL_STATE_RINGING && !param.args[1].equals("android.policy:BIOMETRIC")) {
                                if (mWakeUpHandler.hasMessages(MSG_WAKE_UP)) {
                                    param.setResult(null);
                                    return;
                                }
                                Message msg = mWakeUpHandler.obtainMessage(MSG_WAKE_UP);
                                mWakeUpHandler.sendMessageDelayed(msg, 100);

                                synchronized (mWakeUpWakeLock) {
                                    mWakeUpWakeLock.acquire(5000L /*5 seconds*/);
                                    mWakeUpListener = new SensorEventListener() {
                                        @Override
                                        public void onSensorChanged(SensorEvent event) {
                                            if (mWakeUpWakeLock.isHeld())
                                                mWakeUpWakeLock.release();
                                            if (mWakeUpListener != null) {
                                                mSensorManager.unregisterListener(mWakeUpListener);
                                                mWakeUpListener = null;
                                            }
                                            if (!mWakeUpHandler.hasMessages(MSG_WAKE_UP)) {
                                                param.setResult(null);
                                                return;
                                            }
                                            mWakeUpHandler.removeMessages(MSG_WAKE_UP);
                                            if (event.values[0] >= mProximitySensor.getMaximumRange()) {
                                                mCameraGesture = true;
                                                mWakeUp.run();
                                            } else
                                                mCameraGesture = false;
                                        }

                                        @Override
                                        public void onAccuracyChanged(Sensor sensor, int accuracy) {
                                        }
                                    };
                                }
                                mSensorManager.registerListener(mWakeUpListener,
                                        mProximitySensor, SensorManager.SENSOR_DELAY_FASTEST);
                                param.setResult(null);
                            }
                        }
                    });
                }

                String CLASS = Build.VERSION.SDK_INT >= 30 ? "com.android.server.power.PowerManagerService$PowerManagerHandlerCallback" : "com.android.server.power.PowerManagerService$PowerManagerHandler";
                findAndHookMethod(CLASS, param.classLoader, "handleMessage", Message.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        Message msg = (Message) param.args[0];

                        if (msg.what == MSG_WAKE_UP) {
                            synchronized (mWakeUpWakeLock) {
                                if (mWakeUpWakeLock.isHeld())
                                    mWakeUpWakeLock.release();
                                if (mWakeUpListener != null) {
                                    mSensorManager.unregisterListener(mWakeUpListener);
                                    mWakeUpListener = null;
                                }
                            }
                            mCameraGesture = true;
                            mWakeUp.run();
                        }
                    }
                });

                if (Build.VERSION.SDK_INT <= 30) {
                    findAndHookMethod("com.android.server.policy.PhoneWindowManager", param.classLoader, "powerLongPress", new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (!mPowerManager.isInteractive())
                                param.setResult(null);
                        }
                    });
                } else {
                    findAndHookMethod("com.android.server.policy.PhoneWindowManager", param.classLoader, "powerLongPress", long.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (!mPowerManager.isInteractive())
                                param.setResult(null);
                        }
                    });
                }

                findAndHookMethod("com.android.server.GestureLauncherService", param.classLoader, "handleCameraGesture", boolean.class, int.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (!mCameraGesture)
                            param.setResult(false);
                    }
                });
            }

            int timeout = Integer.parseInt(pref.getString("trick_lessNotifications", "0"));
            if ((Build.VERSION.SDK_INT >= 29 && timeout != 0) || pref.getBoolean("trick_screenOffNotifications", false)) {
                findAndHookMethod("com.android.server.notification.NotificationManagerService", param.classLoader, "shouldMuteNotificationLocked", "com.android.server.notification.NotificationRecord", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if ((boolean) param.getResult() == false) {
                            Object mNotificationLock = getObjectField(param.thisObject, "mNotificationLock");
                            synchronized (mNotificationLock) {
                                if (pref.getBoolean("trick_screenOffNotifications", false)) {
                                    if (getBooleanField(param.thisObject, "mScreenOn"))
                                        param.setResult(true);
                                }
                                if (Build.VERSION.SDK_INT >= 29 && timeout != 0) {
                                    StatusBarNotification sbn = (StatusBarNotification) getObjectField(param.args[0], "sbn");
                                    Long lastTime = mLastTimestamps.get(sbn.getPackageName() + "|" + sbn.getUid());
                                    long currentTime = SystemClock.elapsedRealtime();

                                    if (lastTime == null || currentTime - lastTime > timeout) {
                                        mLastTimestamps.put(sbn.getPackageName() + "|" + sbn.getUid(), currentTime);
                                    } else {
                                        param.setResult(true);
                                    }
                                }
                            }
                        }
                    }

                });
            }

        } else if (param.packageName.equals("com.google.android.apps.nexuslauncher") && Build.VERSION.SDK_INT < 29) {
            if (pref.getBoolean("trick_navbarAlwaysRight", true) && Build.VERSION.SDK_INT == 28) {
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

            if (pref.getBoolean("trick_forceDarkTheme", true) && Build.VERSION.SDK_INT == 27) {
                findAndHookMethod("android.app.WallpaperColors", param.classLoader, "getColorHints", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        int colorHints = getIntField(param.thisObject, "mColorHints");
                        colorHints |= 1<<1;
                        param.setResult(colorHints);
                    }
                });
            }

        } else if (param.packageName.equals("com.android.settings")) {
            findAndHookMethod("com.android.settings.password.ChooseLockGeneric$ChooseLockGenericFragment", param.classLoader, "setUnlockMethod", String.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    String lock = (String) param.args[0];
                    Context context = getContext();
                    if (lock.equals("unlock_set_off") || lock.equals("unlock_set_none")) {
                        if (context != null) {
                            Intent password = new Intent("com.darkeyes.tricks.SET_INTEGER");
                            password.setFlags(FLAG_RECEIVER_FOREGROUND);
                            password.setPackage("com.darkeyes.tricks");
                            password.putExtra("preference", "passwordLength");
                            password.putExtra("value", -1);
                            context.sendBroadcast(password);

                            Intent unlock = new Intent("com.darkeyes.tricks.SET_BOOLEAN");
                            unlock.setFlags(FLAG_RECEIVER_FOREGROUND);
                            unlock.setPackage("com.darkeyes.tricks");
                            unlock.putExtra("preference", "trick_quickUnlock");
                            unlock.putExtra("value", false);
                            context.sendBroadcast(unlock);
                        }
                    }
                }
            });

            findAndHookMethod("com.android.settings.password.ChooseLockPattern$ChooseLockPatternFragment", param.classLoader, "startSaveAndFinish", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    Context context = getContext();
                    if (context != null) {
                        Intent password = new Intent("com.darkeyes.tricks.SET_INTEGER");
                        password.setFlags(FLAG_RECEIVER_FOREGROUND);
                        password.setPackage("com.darkeyes.tricks");
                        password.putExtra("preference", "passwordLength");
                        password.putExtra("value", -1);
                        context.sendBroadcast(password);

                        Intent unlock = new Intent("com.darkeyes.tricks.SET_BOOLEAN");
                        unlock.setFlags(FLAG_RECEIVER_FOREGROUND);
                        unlock.setPackage("com.darkeyes.tricks");
                        unlock.putExtra("preference", "trick_quickUnlock");
                        unlock.putExtra("value", false);
                        context.sendBroadcast(unlock);
                    }
                }
            });

            findAndHookMethod("com.android.settings.password.ChooseLockPassword$ChooseLockPasswordFragment", param.classLoader, "startSaveAndFinish", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    Object mChosenPassword = getObjectField(param.thisObject, "mChosenPassword");
                    int length = (int) callMethod(mChosenPassword, "size");

                    Context context = getContext();
                    if (context != null) {
                        Intent password = new Intent("com.darkeyes.tricks.SET_INTEGER");
                        password.setFlags(FLAG_RECEIVER_FOREGROUND);
                        password.setPackage("com.darkeyes.tricks");
                        password.putExtra("preference", "passwordLength");
                        password.putExtra("value", length);
                        context.sendBroadcast(password);
                    }
                }
            });

        } else if (param.packageName.equals("com.microsoft.office.outlook")) {
            if (pref.getBoolean("trick_OutlookPolicy", false)) {
                findAndHookMethod("com.microsoft.office.outlook.olmcore.managers.mdm.DevicePolicy", param.classLoader, "requiresDeviceManagement", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        param.setResult(false);
                    }
                });

                findAndHookMethod("com.microsoft.office.outlook.olmcore.managers.mdm.DevicePolicy", param.classLoader, "isPolicyApplied", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        param.setResult(true);
                    }
                });
            }
        }
    }

    private Context getContext() {
        Context context = null;
        Application app = AndroidAppHelper.currentApplication();
        try {
            context = app.createPackageContext("com.darkeyes.tricks", 0);
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        return context;
    }
}
