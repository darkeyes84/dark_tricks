package com.darkeyes.tricks;

import android.content.Context;
import android.content.Intent;
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
import android.util.ArrayMap;
import android.view.HapticFeedbackConstants;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.ViewConfiguration;

import java.io.File;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedBridge.invokeOriginalMethod;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.getAdditionalInstanceField;
import static de.robv.android.xposed.XposedHelpers.getBooleanField;
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
    private boolean mTorchAvailable;
    private AudioManager mAudioManager;
    private CameraManager mCameraManager;
    private CameraManager.TorchCallback mTorchCallback;
    private PowerManager mPowerManager;
    private PowerManager.WakeLock mWakeLock;
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

    public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) {

        pref = new XSharedPreferences(prefFile);

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
                            return;
                        }
                        param.setResult(false);
                        return;
                    }

                    if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                        if (mService.isInputViewShown()) {
                            int newKeyCode = (cursorControl == 1) ?
                                    KeyEvent.KEYCODE_DPAD_RIGHT : KeyEvent.KEYCODE_DPAD_LEFT;
                            mService.sendDownUpKeyEvents(newKeyCode);
                            param.setResult(true);
                            return;
                        }
                        param.setResult(false);
                        return;
                    }
                }
            });

            findAndHookMethod("android.inputmethodservice.InputMethodService", null, "onKeyUp", int.class, KeyEvent.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    int keyCode = ((KeyEvent) param.args[1]).getKeyCode();
                    if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                        if (mService.isInputViewShown()) {
                            param.setResult(true);
                        }
                    }
                }
            });
        }
    }

    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam param) {

        if (param.packageName.equals("com.android.systemui")) {

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

            carrierText = pref.getString("trick_customCarrierText", "");

            if (carrierText != null && !carrierText.isEmpty()) {
                findAndHookMethod("com.android.systemui.qs.QSCarrier", param.classLoader, "setCarrierText", CharSequence.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {

                        param.args[0] = carrierText.trim().isEmpty() ? "" : carrierText;
                    }
                });

                findAndHookMethod("com.android.keyguard.CarrierTextController", param.classLoader, "postToCallback", "com.android.keyguard.CarrierTextController.CarrierTextCallbackInfo", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {

                        setObjectField(param.args[0], "carrierText", carrierText.trim().isEmpty() ? "" : carrierText);
                    }
                });
            }

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

            if (pref.getBoolean("trick_skipTrack", true) || pref.getBoolean("trick_powerTorch", true)) {

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
                            };

                            setAdditionalInstanceField(param.thisObject, "mVolumeUpLongPress", mVolumeUpLongPress);
                            setAdditionalInstanceField(param.thisObject, "mVolumeDownLongPress", mVolumeDownLongPress);

                        }

                        if (pref.getBoolean("trick_powerTorch", true)) {

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
                                        if (!mProximityWakeLock.isHeld()) mProximityWakeLock.acquire();
                                        if (mProximityListener != null) {
                                            mSensorManager.unregisterListener(mProximityListener);
                                            mProximityListener = null;
                                        }
                                        mProximityListener = new SensorEventListener() {
                                            @Override
                                            public void onSensorChanged(SensorEvent event) {
                                                if (mProximityWakeLock.isHeld())
                                                    mProximityWakeLock.release();
                                                if (mProximityListener != null) {
                                                    mSensorManager.unregisterListener(mProximityListener, mProximitySensor);
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
                        if (mPowerManager == null) mPowerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);

                        if (pref.getBoolean("trick_skipTrack", true)) {
                            Runnable mVolumeUpLongPress = (Runnable) getAdditionalInstanceField(param.thisObject, "mVolumeUpLongPress");
                            Runnable mVolumeDownLongPress = (Runnable) getAdditionalInstanceField(param.thisObject, "mVolumeDownLongPress");
                            if (mAudioManager == null) mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);

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

                        if (pref.getBoolean("trick_powerTorch", true)) {

                            if (mCameraManager == null) {
                                mTorchAvailable = false;
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
                                            mTorchAvailable = true;
                                        }
                                    }
                                } catch (Exception e) {
                                    mTorchAvailable = false;
                                }
                            }

                            if (keyCode == KeyEvent.KEYCODE_POWER && mPowerManager.isInteractive()) {
                                mDownTime = event.getEventTime();
                            }

                            if (keyCode == KeyEvent.KEYCODE_POWER && !mPowerManager.isInteractive() && (event.getEventTime() - mDownTime > 300) && (event.getSource() != InputDevice.SOURCE_UNKNOWN) && mTorchAvailable) {
                                if (mSensorManager == null) mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
                                if (mProximitySensor == null) mProximitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
                                if (mProximityWakeLock == null) mProximityWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DarkTricks:TorchWakeLock");
                                if (mWakeLock == null) mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DarkTricks:PowerTorch");

                                Runnable mPowerDownLongPress = (Runnable) getAdditionalInstanceField(param.thisObject, "mPowerDownLongPress");
                                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                                    if (event.getRepeatCount() == 0) {
                                        mPowerLongPress = false;
                                        if (!mWakeLock.isHeld()) mWakeLock.acquire();
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
                                    if (mWakeLock.isHeld()) mWakeLock.release();
                                }
                                param.setResult(0);
                            }
                        }

                    }
                });
            }

            if (pref.getBoolean("trick_proximityWakeUp", true)) {

                findAndHookMethod("com.android.server.power.PowerManagerService", param.classLoader, "systemReady", "com.android.internal.app.IAppOpsService", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        mWakeUpContext = (Context) getObjectField(param.thisObject, "mContext");
                        mWakeUpHandler = (Handler) getObjectField(param.thisObject, "mHandler");
                        if (mSensorManager == null) mSensorManager = (SensorManager) mWakeUpContext.getSystemService(Context.SENSOR_SERVICE);
                        if (mProximitySensor == null) mProximitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
                        if (mPowerManager == null) mPowerManager = (PowerManager) mWakeUpContext.getSystemService(Context.POWER_SERVICE);
                        if (mTelephonyManager == null) mTelephonyManager = (TelephonyManager) mWakeUpContext.getSystemService(Context.TELEPHONY_SERVICE);
                        if (mWakeUpWakeLock == null) mWakeUpWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DarkTricks:WakeUpWakelock");
                    }
                });

                findAndHookMethod("com.android.server.power.PowerManagerService", param.classLoader, "wakeUpInternal", long.class, int.class, String.class, int.class, String.class, int.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {

                        Runnable mWakeUp = () -> {
                            long ident = Binder.clearCallingIdentity();
                            try {
                                invokeOriginalMethod(param.method, param.thisObject, param.args);
                            } catch (Throwable t) {
                            } finally {
                                Binder.restoreCallingIdentity(ident);
                            }
                        };

                        setAdditionalInstanceField(param.thisObject, "mWakeUp", mWakeUp);

                        if (mTelephonyManager.getCallState() != TelephonyManager.CALL_STATE_RINGING && !param.args[2].equals("android.policy:BIOMETRIC") && (int)param.args[1] != 5) {

                            if (mWakeUpHandler.hasMessages(MSG_WAKE_UP)) {
                                param.setResult(null);
                                return;
                            }
                            Message msg = mWakeUpHandler.obtainMessage(MSG_WAKE_UP);
                            msg.obj = mWakeUp;
                            mWakeUpHandler.sendMessageDelayed(msg, 100);

                            synchronized (mWakeUpWakeLock) {
                                if (!mWakeUpWakeLock.isHeld()) mWakeUpWakeLock.acquire();
                                if (mWakeUpListener != null) {
                                    mSensorManager.unregisterListener(mWakeUpListener);
                                    mWakeUpListener = null;
                                }
                                mWakeUpListener = new SensorEventListener() {
                                    @Override
                                    public void onSensorChanged(SensorEvent event) {
                                        if (mWakeUpWakeLock.isHeld()) mWakeUpWakeLock.release();
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
                                            mWakeUp.run();
                                        }
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

                findAndHookMethod("com.android.server.power.PowerManagerService$PowerManagerHandler", param.classLoader, "handleMessage", Message.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        Message msg = (Message) param.args[0];
                        Runnable mWakeUp = (Runnable) getAdditionalInstanceField(param.thisObject, "mWakeUp");

                        if (msg.what == MSG_WAKE_UP) {
                            synchronized (mWakeUpWakeLock) {
                                if (mWakeUpWakeLock.isHeld()) mWakeUpWakeLock.release();
                                if (mWakeUpListener != null) {
                                    mSensorManager.unregisterListener(mWakeUpListener);
                                    mWakeUpListener = null;
                                }
                            }
                            mWakeUp.run();
                        }
                    }
                });

                findAndHookMethod("com.android.server.policy.PhoneWindowManager", param.classLoader, "powerLongPress", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (!mPowerManager.isInteractive()) {
                            param.setResult(null);
                        }
                    }
                });
            }

            int timeout = Integer.parseInt(pref.getString("trick_lessNotifications", "0"));
            if (timeout != 0 || pref.getBoolean("trick_screenOffNotifications", false)) {

                findAndHookMethod("com.android.server.notification.NotificationManagerService", param.classLoader, "shouldMuteNotificationLocked", "com.android.server.notification.NotificationRecord", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if ((boolean) param.getResult() == false) {
                            Object mNotificationLock = getObjectField(param.thisObject, "mNotificationLock");
                            synchronized (mNotificationLock) {
                                if (pref.getBoolean("trick_screenOffNotifications", false) && getBooleanField(param.thisObject, "mScreenOn")) {
                                    param.setResult(true);
                                } else if (timeout != 0) {
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
