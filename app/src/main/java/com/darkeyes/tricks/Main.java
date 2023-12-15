package com.darkeyes.tricks;

import static androidx.core.content.ContextCompat.registerReceiver;
import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedBridge.invokeOriginalMethod;
import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getBooleanField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setBooleanField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;

import android.app.Instrumentation;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.inputmethodservice.InputMethodService;
import android.media.AudioManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.service.notification.StatusBarNotification;
import android.telephony.TelephonyManager;
import android.util.ArrayMap;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.ViewConfiguration;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.Map;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class Main implements IXposedHookZygoteInit, IXposedHookLoadPackage {
    private InputMethodService mService;
    private final ArrayMap<String, Long> mLastTimestamps = new ArrayMap<>();
    private boolean mVolumeLongPress;
    private boolean mPowerLongPress;
    private Handler mHandler;
    private AudioManager mAudioManager;
    private PowerManager mPowerManager;
    private CameraManager mCameraManager;
    private String mCameraId;
    private CameraManager.TorchCallback mTorchCallback;
    private SensorEventListener mListenerTorch;
    private SensorEventListener mListenerPower;
    private SensorManager mSensorManager;
    private Sensor mProximitySensor;
    private TelephonyManager mTelephonyManager;
    private PowerManager.WakeLock mWakeLockPower;
    private boolean mTorchEnabled;
    private boolean mCameraGesture;
    private long mDownTime = 0L;
    private final int MSG_WAKE_UP = 100;
    private Runnable mWakeUp;
    private Runnable mVolumeDownLongPress;
    private Runnable mVolumeUpLongPress;
    private Runnable mPowerDownLongPress;
    private Context mContext;
    private XSharedPreferences prefs;
    private final IntentFilter mFilter = new IntentFilter("com.darkeyes.tricks.PREFERENCES");
    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updatePreferences(intent);
        }
    };
    private int mCursorControl;
    private boolean mHideAdbNotification;
    private Object mUsbHandler;
    private boolean mSkipTrack;
    private boolean mPowerTorch;
    private boolean mProximityWakeUp;
    private int mLessNotifications;
    private boolean mScreenOffNotifications;
    private boolean mHideLtePlus;
    private boolean mShow4gForLte;
    private boolean mHideNextAlarm;
    private Object mPhoneStatusBarPolicy;
    private Object mKeyguardZenAlarmViewController;
    private boolean mHideVpn;
    private boolean mHideCert;
    private Object mSecurityControllerImpl;
    private boolean mHideAdGuard;
    private Object FgsManagerControllerImpl;
    private boolean mCircleActiveApps;
    private boolean mHideBuildVersion;
    private Object mQSFooterView;
    private String mCustomCarrierText;
    private Object mCarrierTextManager;
    private Object mCarrierTextCallback;
    private boolean mOutlookPolicy;

    public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) {
        if (prefs == null) {
            prefs = new XSharedPreferences("com.darkeyes.tricks", "com.darkeyes.tricks_shared");
            mCursorControl = Integer.parseInt(prefs.getString("trick_cursorControl", "0"));
            mHideAdbNotification = prefs.getBoolean("trick_hideAdbNotification", false);
            mSkipTrack = prefs.getBoolean("trick_skipTrack", false);
            mPowerTorch = prefs.getBoolean("trick_powerTorch", false);
            mProximityWakeUp = prefs.getBoolean("trick_proximityWakeUp", false);
            mLessNotifications = Integer.parseInt(prefs.getString("trick_lessNotifications", "0"));
            mScreenOffNotifications = prefs.getBoolean("trick_screenOffNotifications", false);
            mHideLtePlus = prefs.getBoolean("trick_hideLtePlus", false);
            mShow4gForLte = prefs.getBoolean("trick_show4gForLte", false);
            mHideNextAlarm = prefs.getBoolean("trick_hideNextAlarm", false);
            mHideVpn = prefs.getBoolean("trick_hideVpn", false);
            mHideCert = prefs.getBoolean("trick_hideCert", false);
            mHideAdGuard = prefs.getBoolean("trick_hideAdGuard", false);
            mCircleActiveApps = prefs.getBoolean("trick_circleActiveApps", false);
            mHideBuildVersion = prefs.getBoolean("trick_hideBuildVersion", false);
            mCustomCarrierText = prefs.getString("trick_customCarrierText", "");
            mOutlookPolicy = prefs.getBoolean("trick_OutlookPolicy", false);
        }
        findAndHookMethod("android.inputmethodservice.InputMethodService", null, "onCreate", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                mService = (InputMethodService) param.thisObject;
                if (mContext == null) {
                    mContext = (Context) param.thisObject;
                    registerReceiver(mContext, mReceiver, mFilter, ContextCompat.RECEIVER_EXPORTED);
                }
            }
        });
        findAndHookMethod("android.inputmethodservice.InputMethodService", null, "onKeyDown", int.class, KeyEvent.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                int keyCode = ((KeyEvent) param.args[1]).getKeyCode();
                if (mCursorControl > 0 && mService.isInputViewShown()) {
                    if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                        int newKeyCode = (mCursorControl == 1) ? KeyEvent.KEYCODE_DPAD_LEFT : KeyEvent.KEYCODE_DPAD_RIGHT;
                        mService.sendDownUpKeyEvents(newKeyCode);
                        param.setResult(true);
                    } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                        int newKeyCode = (mCursorControl == 1) ? KeyEvent.KEYCODE_DPAD_RIGHT : KeyEvent.KEYCODE_DPAD_LEFT;
                        mService.sendDownUpKeyEvents(newKeyCode);
                        param.setResult(true);
                    }
                }
            }
        });
        findAndHookMethod("android.inputmethodservice.InputMethodService", null, "onKeyUp", int.class, KeyEvent.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                int keyCode = ((KeyEvent) param.args[1]).getKeyCode();
                if (mCursorControl > 0 && mService.isInputViewShown() && (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP))
                    param.setResult(true);
            }
        });

    }

    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam param) {
        if (param.packageName.equals("android")) {
            findAndHookMethod("com.android.server.policy.PhoneWindowManager", param.classLoader, "initKeyCombinationRules", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (mContext == null) {
                        mContext = (Context) getObjectField(param.thisObject, "mContext");
                        registerReceiver(mContext, mReceiver, mFilter, ContextCompat.RECEIVER_EXPORTED);
                    }

                    if (mPowerManager == null)
                        mPowerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
                    if (mSensorManager == null)
                        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
                    if (mProximitySensor == null)
                        mProximitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
                    if (mAudioManager == null)
                        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
                    if (mTelephonyManager == null)
                        mTelephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);

                    if (mTorchCallback == null) {
                        mTorchCallback = new CameraManager.TorchCallback() {
                            @Override
                            public void onTorchModeChanged(String cameraId, boolean enabled) {
                                mTorchEnabled = enabled;
                                if (mListenerTorch != null) {
                                    mSensorManager.unregisterListener(mListenerTorch);
                                    mListenerTorch = null;
                                }
                            }

                            @Override
                            public void onTorchModeUnavailable(String cameraId) {
                                mTorchEnabled = false;
                                if (mListenerTorch != null) {
                                    mSensorManager.unregisterListener(mListenerTorch);
                                    mListenerTorch = null;
                                }
                            }
                        };
                    }

                    if (mVolumeUpLongPress == null) {
                        mVolumeUpLongPress = () -> {
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
                    }

                    if (mVolumeDownLongPress == null) {
                        mVolumeDownLongPress = () -> {
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
                    }
                }
            });

            findAndHookMethod("com.android.server.usb.UsbDeviceManager$UsbHandler", param.classLoader, "updateAdbNotification", boolean.class, new XC_MethodHook() {
                boolean connected;
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    mUsbHandler = param.thisObject;
                    connected = getBooleanField(param.thisObject, "mConnected");
                    if (mHideAdbNotification)
                        setBooleanField(param.thisObject, "mConnected", false);
                }
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (mHideAdbNotification)
                        setBooleanField(param.thisObject, "mConnected", connected);
                }
            });

            findAndHookMethod("com.android.server.policy.PhoneWindowManager", param.classLoader, "interceptKeyBeforeQueueing", KeyEvent.class, int.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    KeyEvent event = (KeyEvent) param.args[0];
                    int keyCode = event.getKeyCode();
                    Context context = (Context) getObjectField(param.thisObject, "mContext");
                    Handler handler = (Handler) getObjectField(param.thisObject, "mHandler");
                    final PowerManager.WakeLock wakeLockTorch = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DarkTricks:Torch");

                    if (mPowerDownLongPress == null) {
                        mPowerDownLongPress = () -> {
                            if (!mTorchEnabled) {
                                synchronized (wakeLockTorch) {
                                    mListenerTorch = new SensorEventListener() {
                                        @Override
                                        public void onSensorChanged(SensorEvent event) {
                                            if (wakeLockTorch.isHeld())
                                                wakeLockTorch.release();
                                            if (mListenerTorch != null) {
                                                mSensorManager.unregisterListener(mListenerTorch);
                                                mListenerTorch = null;
                                            }
                                            if (event.values[0] >= mProximitySensor.getMaximumRange()) {
                                                mPowerLongPress = true;
                                                callMethod(param.thisObject, "performHapticFeedback", new Class<?>[]{int.class, boolean.class, String.class}, HapticFeedbackConstants.LONG_PRESS, false, null);

                                                try {
                                                    mCameraManager.setTorchMode(mCameraId, !mTorchEnabled);
                                                    mTorchEnabled = !mTorchEnabled;
                                                } catch (Exception ignored) {
                                                }
                                            }
                                        }

                                        @Override
                                        public void onAccuracyChanged(Sensor sensor, int accuracy) {
                                        }

                                    };
                                }
                                mSensorManager.registerListener(mListenerTorch,
                                        mProximitySensor, SensorManager.SENSOR_DELAY_FASTEST);
                            } else {
                                mPowerLongPress = true;
                                callMethod(param.thisObject, "performHapticFeedback", new Class<?>[]{int.class, boolean.class, String.class}, HapticFeedbackConstants.LONG_PRESS, false, null);

                                try {
                                    mCameraManager.setTorchMode(mCameraId, !mTorchEnabled);
                                    mTorchEnabled = !mTorchEnabled;
                                } catch (Exception ignored) {
                                }
                            }
                            if (wakeLockTorch.isHeld())
                                wakeLockTorch.release();
                        };
                    }

                    if (mCameraManager == null) {
                        mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
                        mCameraManager.registerTorchCallback(mTorchCallback, handler);
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
                        } catch (Exception ignored) {
                        }
                    }

                    if (mSkipTrack && (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP)) {
                        if ((event.getFlags() & KeyEvent.FLAG_FROM_SYSTEM) != 0 &&
                                !mPowerManager.isInteractive() && mAudioManager.isMusicActive()) {

                            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                                if (event.getRepeatCount() == 0) {
                                    mVolumeLongPress = false;
                                    handler.postDelayed(keyCode == KeyEvent.KEYCODE_VOLUME_UP ? mVolumeUpLongPress :
                                            mVolumeDownLongPress, ViewConfiguration.getLongPressTimeout());
                                }
                            } else {
                                handler.removeCallbacks(mVolumeUpLongPress);
                                handler.removeCallbacks(mVolumeDownLongPress);

                                if (!mVolumeLongPress) {
                                    mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                                            keyCode == KeyEvent.KEYCODE_VOLUME_UP ?
                                                    AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER, 0);
                                }
                            }
                            param.setResult(0);
                        }
                    } else if (mPowerTorch && keyCode == KeyEvent.KEYCODE_POWER) {
                        if (mPowerManager.isInteractive())
                            mDownTime = event.getEventTime();

                        if ((!mPowerManager.isInteractive() && (event.getEventTime() - mDownTime > ViewConfiguration.getMultiPressTimeout()) || mTorchEnabled) &&
                                event.getSource() != InputDevice.SOURCE_UNKNOWN) {

                            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                                if (event.getRepeatCount() == 0) {
                                    mPowerLongPress = false;
                                    wakeLockTorch.acquire(1000L);
                                    handler.postDelayed(mPowerDownLongPress, ViewConfiguration.getLongPressTimeout());
                                }
                            } else {
                                handler.removeCallbacks(mPowerDownLongPress);

                                if (mPowerLongPress) {
                                    mPowerLongPress = false;
                                } else {
                                    handler.post(() -> {
                                        callMethod(context.getSystemService(Context.INPUT_SERVICE), "injectInputEvent", new KeyEvent(event.getEventTime(), event.getEventTime(), KeyEvent.ACTION_DOWN,
                                                KeyEvent.KEYCODE_POWER, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, KeyEvent.FLAG_FROM_SYSTEM, InputDevice.SOURCE_UNKNOWN), 0);
                                        callMethod(context.getSystemService(Context.INPUT_SERVICE), "injectInputEvent", new KeyEvent(event.getEventTime(), event.getEventTime(), KeyEvent.ACTION_UP,
                                                KeyEvent.KEYCODE_POWER, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, KeyEvent.FLAG_FROM_SYSTEM, InputDevice.SOURCE_UNKNOWN), 0);
                                    });
                                }
                                if (wakeLockTorch.isHeld())
                                    wakeLockTorch.release();
                            }
                            param.setResult(0);
                        }
                    }
                }
            });
            findAndHookMethod("com.android.server.power.PowerManagerService", param.classLoader, "systemReady", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    Context context = (Context) getObjectField(param.thisObject, "mContext");
                    mHandler = (Handler) getObjectField(param.thisObject, "mHandler");

                    if (mPowerManager == null)
                        mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                    if (mSensorManager == null)
                        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
                    if (mProximitySensor == null)
                        mProximitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
                    if (mTelephonyManager == null)
                        mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                    if (mWakeLockPower == null)
                        mWakeLockPower = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DarkTricks:WakeUp");
                }
            });
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

                    if (mProximityWakeUp && mHandler != null && mTelephonyManager.getCallState() != TelephonyManager.CALL_STATE_RINGING && !param.args[1].equals("android.policy:BIOMETRIC")) {
                        if (mHandler.hasMessages(MSG_WAKE_UP)) {
                            param.setResult(null);
                            return;
                        }
                        Message msg = mHandler.obtainMessage(MSG_WAKE_UP);
                        mHandler.sendMessageDelayed(msg, 100);

                        synchronized (mWakeLockPower) {
                            mWakeLockPower.acquire(1000L);
                            mListenerPower = new SensorEventListener() {
                                @Override
                                public void onSensorChanged(SensorEvent event) {
                                    if (mWakeLockPower.isHeld())
                                        mWakeLockPower.release();
                                    if (mListenerPower != null) {
                                        mSensorManager.unregisterListener(mListenerPower);
                                        mListenerPower = null;
                                    }
                                    if (!mHandler.hasMessages(MSG_WAKE_UP)) {
                                        param.setResult(null);
                                        return;
                                    }
                                    mHandler.removeMessages(MSG_WAKE_UP);
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
                        mSensorManager.registerListener(mListenerPower,
                                mProximitySensor, SensorManager.SENSOR_DELAY_FASTEST);
                        param.setResult(null);
                    }
                }
            });
            findAndHookMethod("com.android.server.power.PowerManagerService$PowerManagerHandlerCallback", param.classLoader, "handleMessage", Message.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    Message msg = (Message) param.args[0];

                    if (mProximityWakeUp && msg.what == MSG_WAKE_UP) {
                        synchronized (mWakeLockPower) {
                            if (mWakeLockPower.isHeld())
                                mWakeLockPower.release();
                            if (mListenerPower != null) {
                                mSensorManager.unregisterListener(mListenerPower);
                                mListenerPower = null;
                            }
                        }
                        mCameraGesture = true;
                        mWakeUp.run();
                    }
                }
            });
            findAndHookMethod("com.android.server.policy.PhoneWindowManager", param.classLoader, "powerLongPress", long.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (mProximityWakeUp && !mPowerManager.isInteractive())
                        param.setResult(null);
                }
            });
            findAndHookMethod("com.android.server.GestureLauncherService", param.classLoader, "handleCameraGesture", boolean.class, int.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (mProximityWakeUp && !mCameraGesture)
                        param.setResult(false);
                }
            });
            findAndHookMethod("com.android.server.notification.NotificationManagerService", param.classLoader, "shouldMuteNotificationLocked", "com.android.server.notification.NotificationRecord", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if ((boolean) param.getResult() == false) {
                        final Object notificationLock = getObjectField(param.thisObject, "mNotificationLock");
                        synchronized (notificationLock) {
                            if (mScreenOffNotifications && getBooleanField(param.thisObject, "mScreenOn"))
                                param.setResult(true);
                            if (mLessNotifications > 0) {
                                StatusBarNotification sbn = (StatusBarNotification) getObjectField(param.args[0], "sbn");
                                Long lastTime = mLastTimestamps.get(sbn.getPackageName() + "|" + sbn.getUid());
                                long currentTime = SystemClock.elapsedRealtime();

                                if (lastTime == null || currentTime - lastTime > mLessNotifications) {
                                    mLastTimestamps.put(sbn.getPackageName() + "|" + sbn.getUid(), currentTime);
                                } else {
                                    param.setResult(true);
                                }
                            }
                        }
                    }
                }

            });
        } else if (param.packageName.equals("com.android.systemui")) {
            findAndHookMethod(Instrumentation.class, "newApplication", ClassLoader.class, String.class, Context.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (mContext == null) {
                        mContext = (Context) param.args[2];
                        registerReceiver(mContext, mReceiver, mFilter, ContextCompat.RECEIVER_EXPORTED);
                    }
                }
            });
            findAndHookMethod("com.android.settingslib.mobile.MobileMappings$Config", param.classLoader, "readConfig", "android.content.Context", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    setBooleanField(param.getResult(), "hideLtePlus", mHideLtePlus);
                    setBooleanField(param.getResult(), "show4gForLte", mShow4gForLte);
                }
            });
            findAndHookMethod("com.android.systemui.statusbar.phone.PhoneStatusBarPolicy", param.classLoader, "updateAlarm", new XC_MethodHook() {
                boolean currentUserSetup;
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    mPhoneStatusBarPolicy = param.thisObject;
                    currentUserSetup = getBooleanField(mPhoneStatusBarPolicy, "mCurrentUserSetup");
                    if (mHideNextAlarm)
                        setBooleanField(param.thisObject, "mCurrentUserSetup", false);
                }
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (mHideNextAlarm)
                        setBooleanField(param.thisObject, "mCurrentUserSetup", currentUserSetup);
                }
            });
            findAndHookMethod("com.google.android.systemui.smartspace.KeyguardZenAlarmViewController", param.classLoader, "showAlarm", new XC_MethodHook() {
                Drawable alarmImage;
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    mKeyguardZenAlarmViewController = param.thisObject;
                    alarmImage = (Drawable) getObjectField(param.thisObject, "alarmImage");
                    if (mHideNextAlarm)
                        setObjectField(param.thisObject, "alarmImage", null);
                }
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (mHideNextAlarm)
                        setObjectField(param.thisObject, "alarmImage", alarmImage);
                }
            });
            findAndHookMethod("com.android.systemui.statusbar.policy.SecurityControllerImpl", param.classLoader, "fireCallbacks", new XC_MethodHook() {
                Object currentVpns;
                Object hasCACerts;
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    mSecurityControllerImpl = param.thisObject;
                    StackTraceElement[] stacktrace = Thread.currentThread().getStackTrace();
                    if (stacktrace[6].getMethodName().equals("onAvailable") || stacktrace[6].getMethodName().equals("onLost"))
                        currentVpns = getObjectField(param.thisObject, "mCurrentVpns");
                    if (stacktrace[6].getMethodName().equals("run"))
                        hasCACerts = getObjectField(param.thisObject, "mHasCACerts");
                    setObjectField(param.thisObject, "mCurrentVpns", mHideVpn ? new SparseArray<>() : currentVpns);
                    setObjectField(param.thisObject, "mHasCACerts", mHideCert ? new ArrayMap<Integer, Boolean>() : hasCACerts);
                }
            });
            findAndHookMethod("com.android.systemui.qs.FgsManagerControllerImpl", param.classLoader, "getNumVisiblePackagesLocked", new XC_MethodHook() {
                final Object[] UIControl = findClass("com.android.systemui.qs.FgsManagerControllerImpl$UIControl", param.classLoader).getEnumConstants();
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    FgsManagerControllerImpl = param.thisObject;
                    Map<Object, Object> runningTaskIdentifiers = (Map<Object, Object>) getObjectField(param.thisObject, "runningTaskIdentifiers");
                    for (Object key : runningTaskIdentifiers.keySet()) {
                        String packageName = (String) getObjectField(key, "packageName");
                        if (packageName.contains("com.adguard"))
                            setObjectField(key, "uiControl", mHideAdGuard ? UIControl[2] : UIControl[0]);
                    }
                }
            });
            findAndHookMethod("com.android.systemui.qs.FgsManagerControllerImpl$UserPackage", param.classLoader, "updateUiControl", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    String packageName = (String) getObjectField(param.thisObject, "packageName");
                    if (mHideAdGuard && packageName.contains("com.adguard"))
                        param.setResult(null);
                }
            });
            hookAllConstructors(findClass("com.android.systemui.qs.footer.ui.viewmodel.FooterActionsForegroundServicesButtonViewModel", param.classLoader), new XC_MethodHook() {
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (mCircleActiveApps)
                        param.args[2] = false;
                }
            });
            findAndHookMethod("com.android.systemui.qs.QSFooterView", param.classLoader, "setBuildText", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    mQSFooterView = param.thisObject;
                    if (mHideBuildVersion)
                        setObjectField(mQSFooterView, "mShouldShowBuildText", false);
                }
            });
            findAndHookMethod("com.android.systemui.shade.carrier.ShadeCarrier", param.classLoader, "updateState", "com.android.systemui.shade.carrier.CellSignalState", boolean.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    TextView mCarrierText = (TextView) getObjectField(param.thisObject, "mCarrierText");
                    String carrierText = String.valueOf(mCarrierText.getText());
                    carrierText = !mCustomCarrierText.isEmpty() ? mCustomCarrierText : carrierText.replace("Calling", "");
                    mCarrierText.setText(carrierText.trim());
                    mCarrierText.setGravity(Gravity.END);
                }
            });
            findAndHookMethod("com.android.keyguard.CarrierTextManager", param.classLoader, "postToCallback", "com.android.keyguard.CarrierTextManager.CarrierTextCallbackInfo", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    mCarrierTextManager = param.thisObject;
                    Object info = param.args[0];
                    String carrierText = (String) getObjectField(info, "carrierText");
                    String customText = !mCustomCarrierText.isEmpty() ? mCustomCarrierText : carrierText.replace("Calling", "");
                    setObjectField(info, "carrierText", customText.trim());
                    param.args[0] = info;
                }
            });
            findAndHookMethod("com.android.keyguard.CarrierTextController", param.classLoader, "onInit", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    mCarrierTextCallback = getObjectField(param.thisObject, "mCarrierTextCallback");
                }
            });
        } else if (param.packageName.equals("com.microsoft.office.outlook")) {
            findAndHookMethod(Instrumentation.class, "newApplication", ClassLoader.class, String.class, Context.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (mContext == null) {
                        mContext = (Context) param.args[2];
                        registerReceiver(mContext, mReceiver, mFilter, ContextCompat.RECEIVER_EXPORTED);
                    }
                }
            });
            findAndHookMethod("com.microsoft.office.outlook.olmcore.managers.mdm.DevicePolicy", param.classLoader, "requiresDeviceManagement", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (mOutlookPolicy)
                        param.setResult(false);
                }
            });
            findAndHookMethod("com.microsoft.office.outlook.olmcore.managers.mdm.DevicePolicy", param.classLoader, "isPolicyApplied", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (mOutlookPolicy)
                        param.setResult(true);
                }
            });
        }
    }
    private void updatePreferences(Intent intent) {
        Bundle extras = intent.getExtras();
        String key = extras.getString("preference");
        if ("trick_cursorControl".equals(key))
            mCursorControl = Integer.parseInt(extras.getString("value"));
        else if ("trick_hideAdbNotification".equals(key)) {
            mHideAdbNotification = extras.getBoolean("value");
            if (mUsbHandler != null)
                callMethod(mUsbHandler, "updateAdbNotification", false);
        }
        else if ("trick_skipTrack".equals(key))
            mSkipTrack = extras.getBoolean("value");
        else if ("trick_powerTorch".equals(key))
            mPowerTorch = extras.getBoolean("value");
        else if ("trick_proximityWakeUp".equals(key))
            mProximityWakeUp = extras.getBoolean("value");
        else if ("trick_lessNotifications".equals(key))
            mLessNotifications = Integer.parseInt(extras.getString("value"));
        else if ("trick_screenOffNotifications".equals(key))
            mScreenOffNotifications = extras.getBoolean("value");
        else if ("trick_hideLtePlus".equals(key))
            mHideLtePlus = extras.getBoolean("value");
        else if ("trick_show4gForLte".equals(key))
            mShow4gForLte = extras.getBoolean("value");
        else if ("trick_hideNextAlarm".equals(key)) {
            mHideNextAlarm = extras.getBoolean("value");
            if (mPhoneStatusBarPolicy != null)
                callMethod(mPhoneStatusBarPolicy, "updateAlarm");
            if (mKeyguardZenAlarmViewController != null)
                callMethod(mKeyguardZenAlarmViewController, "showAlarm");
        } else if ("trick_hideVpn".equals(key)) {
            mHideVpn = extras.getBoolean("value");
            if (mPhoneStatusBarPolicy != null)
                callMethod(mSecurityControllerImpl, "fireCallbacks");
        } else if ("trick_hideCert".equals(key)) {
            mHideCert = extras.getBoolean("value");
            if (mPhoneStatusBarPolicy != null)
                callMethod(mSecurityControllerImpl, "fireCallbacks");
        } else if ("trick_hideAdGuard".equals(key)) {
            mHideAdGuard = extras.getBoolean("value");
            if (FgsManagerControllerImpl != null) {
                callMethod(FgsManagerControllerImpl, "updateNumberOfVisibleRunningPackagesLocked");
                callMethod(FgsManagerControllerImpl, "updateAppItemsLocked", false);
            }
        } else if ("trick_circleActiveApps".equals(key))
            mCircleActiveApps = extras.getBoolean("value");
        else if ("trick_hideBuildVersion".equals(key)) {
            mHideBuildVersion = extras.getBoolean("value");
            if (mQSFooterView != null)
                callMethod(mQSFooterView, "setBuildText");
        } else if ("trick_customCarrierText".equals(key)) {
            mCustomCarrierText = extras.getString("value");
            if (mCarrierTextManager != null) {
                callMethod(mCarrierTextManager, "updateCarrierText");
                callMethod(mCarrierTextManager, "handleSetListening", mCarrierTextCallback);
                callMethod(mCarrierTextManager, "updateCarrierText");
            }
        } else if ("trick_OutlookPolicy".equals(key))
            mOutlookPolicy = extras.getBoolean("value");
    }
}
