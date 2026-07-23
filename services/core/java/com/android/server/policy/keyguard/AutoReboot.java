package com.android.server.policy.keyguard;

import android.content.Context;
import android.ext.settings.ExtSettings;
import android.guardtalk.GuardTalkAutoRebootPolicy;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemProperties;
import android.util.Slog;

/** @hide */
class AutoReboot {
    private static final String TAG = AutoReboot.class.getSimpleName();

    // writes to this system property are special-cased in init
    private static final String SYS_PROP = "sys.auto_reboot_ctl";

    private static final Handler sHandler = new Handler(Looper.getMainLooper());
    private static final Object sLock = new Object();
    private static boolean sKeyguardShowing;
    private static Context sAppContext;

    private static final Runnable sExclusionRecheck = () -> {
        synchronized (sLock) {
            if (!sKeyguardShowing || sAppContext == null) {
                return;
            }
            applyLockedState(sAppContext);
        }
    };

    // This callback is invoked:
    // - when keyguard becomes active (i.e. when device gets locked, including at boot-time)
    // - when keyguard is dismissed by unlocking the device
    // - when keyguard is dismissed by switching to a user that doesn't have a secure lockscreen,
    // but not when switching to a user that does have a secure lockscreen
    // - showing=false callback is invoked at boot-time when there's no lock screen, i.e. when the
    // device boots straight into the home screen or initial setup wizard
    //
    // Note that "swipe-to-unlock" lockscreen is considered to be a keyguard.
    //
    // GuardTalk (T-SEC-P2-AUTOREBOOT): inactivity is measured from lock (keyguard
    // showing). Profiles clamp ExtSettings.AUTO_REBOOT_TIMEOUT. Exclusion windows
    // pause/resume the init timer via sys.auto_reboot_ctl.
    static void onKeyguardShowingStateChanged(Context ctx, boolean showing, int userId) {
        Slog.d(TAG, "onKeyguardShowingStateChanged, showing: " + showing + ", userId: " + userId);

        synchronized (sLock) {
            sKeyguardShowing = showing;
            if (ctx != null) {
                sAppContext = ctx.getApplicationContext() != null
                        ? ctx.getApplicationContext() : ctx;
            }
            sHandler.removeCallbacks(sExclusionRecheck);

            if (!showing) {
                SystemProperties.set(SYS_PROP, "on_device_unlocked");
                return;
            }

            applyLockedState(sAppContext != null ? sAppContext : ctx);
        }
    }

    private static void applyLockedState(Context ctx) {
        final int rawTimeoutMillis = ExtSettings.AUTO_REBOOT_TIMEOUT.get(ctx);
        final int timeoutMillis =
                GuardTalkAutoRebootPolicy.getEffectiveTimeoutMillis(rawTimeoutMillis);
        // Persist snapped profile so Settings UI and ExtSettings stay aligned.
        if (GuardTalkAutoRebootPolicy.isProfilesEnabled()
                && timeoutMillis != rawTimeoutMillis) {
            ExtSettings.AUTO_REBOOT_TIMEOUT.put(ctx, timeoutMillis);
        }
        final int timeoutSeconds = timeoutMillis / 1000;

        if (timeoutSeconds <= 0) {
            Slog.d(TAG, "timeoutSeconds: 0 (Off)");
            return;
        }

        if (GuardTalkAutoRebootPolicy.isProfilesEnabled()
                && GuardTalkAutoRebootPolicy.isInExclusionWindow(ctx)) {
            Slog.i(TAG, "exclusion window active; pausing auto-reboot timer");
            SystemProperties.set(SYS_PROP, GuardTalkAutoRebootPolicy.CTL_PAUSE);
            sHandler.postDelayed(sExclusionRecheck,
                    GuardTalkAutoRebootPolicy.getExclusionRecheckMillis());
            return;
        }

        if (GuardTalkAutoRebootPolicy.isProfilesEnabled()) {
            // Resume any paused remaining time first; if none, arm full timeout.
            SystemProperties.set(SYS_PROP, GuardTalkAutoRebootPolicy.CTL_RESUME);
        }

        SystemProperties.set(SYS_PROP, Integer.toString(timeoutSeconds));
        Slog.d(TAG, "timeoutSeconds: " + timeoutSeconds
                + " (rawMs=" + rawTimeoutMillis + ", effectiveMs=" + timeoutMillis + ")");

        if (GuardTalkAutoRebootPolicy.isProfilesEnabled()) {
            // Keep watching for exclusion windows that start after arming.
            sHandler.postDelayed(sExclusionRecheck,
                    GuardTalkAutoRebootPolicy.getExclusionRecheckMillis());
        }
    }
}
