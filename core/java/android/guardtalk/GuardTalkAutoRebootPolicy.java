/*
 * Copyright (C) 2026 The GuardTalkOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.guardtalk;

import android.app.AppOpsManager;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.SystemProperties;
import android.os.SystemUpdateManager;
import android.telecom.TelecomManager;
import android.util.Slog;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * GuardTalk Auto-reboot policy (T-SEC-P2-AUTOREBOOT).
 *
 * <p>Extends GrapheneOS {@code AutoReboot} / {@code ExtSettings.AUTO_REBOOT_TIMEOUT}
 * with fixed profiles (Off / 1h / 2h / 4h / 8h), inactivity measured from lock
 * (keyguard showing), and exclusion windows that pause the init timer.
 * See {@code vendor/guardtalk/docs/AUTO_REBOOT_POLICY.md}.
 *
 * @hide
 */
public final class GuardTalkAutoRebootPolicy {

    private static final String TAG = "GuardTalkAutoReboot";

    /** When true, clamp timeout to GuardTalk profiles and honor exclusions. */
    public static final String PROP_AUTO_REBOOT_PROFILES =
            "ro.guardtalk.auto_reboot_profiles";

    /** Default profile timeout when the Global setting is not a known profile. */
    public static final String PROP_AUTO_REBOOT_DEFAULT_MS =
            "ro.guardtalk.auto_reboot_default_ms";

    /** sys.auto_reboot_ctl command: pause armed timer (stores remaining). */
    public static final String CTL_PAUSE = "pause";

    /** sys.auto_reboot_ctl command: resume paused remaining time. */
    public static final String CTL_RESUME = "resume";

    /** Profile: Off. */
    public static final int PROFILE_OFF_MS = 0;

    /** Allowed inactivity profiles (milliseconds). */
    public static final int[] PROFILE_TIMEOUTS_MS = new int[] {
            PROFILE_OFF_MS,
            (int) TimeUnit.HOURS.toMillis(1),
            (int) TimeUnit.HOURS.toMillis(2),
            (int) TimeUnit.HOURS.toMillis(4),
            (int) TimeUnit.HOURS.toMillis(8),
    };

    /** Default when unset / non-profile value: 8 hours. */
    public static final int DEFAULT_PROFILE_MS = (int) TimeUnit.HOURS.toMillis(8);

    private static final long EXCLUSION_RECHECK_MS = TimeUnit.SECONDS.toMillis(30);

    private GuardTalkAutoRebootPolicy() {}

    /** Product policy on: GuardTalk Auto-reboot profiles + exclusions. */
    public static boolean isProfilesEnabled() {
        return SystemProperties.getBoolean(PROP_AUTO_REBOOT_PROFILES, false);
    }

    /** Recheck interval while locked and excluded (or waiting to re-arm). */
    public static long getExclusionRecheckMillis() {
        return EXCLUSION_RECHECK_MS;
    }

    /**
     * Maps a raw {@code AUTO_REBOOT_TIMEOUT} value to a GuardTalk profile.
     * Unknown values snap to the nearest allowed profile (default 8h).
     */
    public static int clampToProfileMillis(int timeoutMillis) {
        if (timeoutMillis <= 0) {
            return PROFILE_OFF_MS;
        }
        for (int allowed : PROFILE_TIMEOUTS_MS) {
            if (allowed == timeoutMillis) {
                return allowed;
            }
        }
        final int fallback = SystemProperties.getInt(
                PROP_AUTO_REBOOT_DEFAULT_MS, DEFAULT_PROFILE_MS);
        int best = fallback > 0 ? fallback : DEFAULT_PROFILE_MS;
        long bestDelta = Long.MAX_VALUE;
        for (int allowed : PROFILE_TIMEOUTS_MS) {
            if (allowed == PROFILE_OFF_MS) {
                continue;
            }
            final long delta = Math.abs((long) allowed - (long) timeoutMillis);
            if (delta < bestDelta) {
                bestDelta = delta;
                best = allowed;
            }
        }
        return best;
    }

    /**
     * Effective timeout for arming {@code sys.auto_reboot_ctl}.
     * When profiles are disabled, returns {@code rawTimeoutMillis} unchanged.
     */
    public static int getEffectiveTimeoutMillis(int rawTimeoutMillis) {
        if (!isProfilesEnabled()) {
            return rawTimeoutMillis;
        }
        return clampToProfileMillis(rawTimeoutMillis);
    }

    /**
     * True when a reboot must be deferred (call, camera/video, file copy,
     * system update). Recovery is out of band (no system_server).
     * Fail-open on missing services (do not block reboot forever).
     */
    public static boolean isInExclusionWindow(Context ctx) {
        if (!isProfilesEnabled() || ctx == null) {
            return false;
        }
        try {
            if (isInCall(ctx)) {
                return true;
            }
            if (isCameraOrVideoActive(ctx)) {
                return true;
            }
            if (isFileCopyActive(ctx)) {
                return true;
            }
            if (isSystemUpdateActive(ctx)) {
                return true;
            }
        } catch (Exception e) {
            Slog.w(TAG, "exclusion check failed; not excluding", e);
        }
        return false;
    }

    private static boolean isInCall(Context ctx) {
        final TelecomManager tm = ctx.getSystemService(TelecomManager.class);
        if (tm != null) {
            try {
                if (tm.isInCall()) {
                    return true;
                }
            } catch (SecurityException e) {
                Slog.w(TAG, "TelecomManager.isInCall denied", e);
            }
        }
        final AudioManager am = ctx.getSystemService(AudioManager.class);
        if (am != null) {
            final int mode = am.getMode();
            return mode == AudioManager.MODE_IN_CALL
                    || mode == AudioManager.MODE_IN_COMMUNICATION;
        }
        return false;
    }

    private static boolean isCameraOrVideoActive(Context ctx) {
        return hasRunningAppOp(ctx, AppOpsManager.OP_CAMERA)
                || hasRunningAppOp(ctx, AppOpsManager.OP_RECORD_AUDIO);
    }

    private static boolean isFileCopyActive(Context ctx) {
        // Best-effort: active legacy/storage write ops indicate bulk transfer.
        return hasRunningAppOp(ctx, AppOpsManager.OP_WRITE_EXTERNAL_STORAGE)
                || hasRunningAppOp(ctx, AppOpsManager.OP_MANAGE_EXTERNAL_STORAGE);
    }

    private static boolean isSystemUpdateActive(Context ctx) {
        final SystemUpdateManager sum =
                ctx.getSystemService(SystemUpdateManager.class);
        if (sum == null) {
            return false;
        }
        try {
            final Bundle info = sum.retrieveSystemUpdateInfo();
            if (info == null) {
                return false;
            }
            final int status = info.getInt(SystemUpdateManager.KEY_STATUS,
                    SystemUpdateManager.STATUS_UNKNOWN);
            return status == SystemUpdateManager.STATUS_WAITING_DOWNLOAD
                    || status == SystemUpdateManager.STATUS_IN_PROGRESS
                    || status == SystemUpdateManager.STATUS_WAITING_INSTALL
                    || status == SystemUpdateManager.STATUS_WAITING_REBOOT;
        } catch (SecurityException e) {
            Slog.w(TAG, "SystemUpdateManager read denied", e);
            return false;
        } catch (Exception e) {
            Slog.w(TAG, "SystemUpdateManager query failed", e);
            return false;
        }
    }

    private static boolean hasRunningAppOp(Context ctx, int op) {
        final AppOpsManager aom = ctx.getSystemService(AppOpsManager.class);
        if (aom == null) {
            return false;
        }
        try {
            final List<AppOpsManager.PackageOps> pkgs =
                    aom.getPackagesForOps(new int[] { op });
            if (pkgs == null) {
                return false;
            }
            for (AppOpsManager.PackageOps pkg : pkgs) {
                for (AppOpsManager.OpEntry entry : pkg.getOps()) {
                    if (entry.getOp() == op && entry.isRunning()) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            Slog.w(TAG, "AppOps query failed for op=" + op, e);
        }
        return false;
    }
}
