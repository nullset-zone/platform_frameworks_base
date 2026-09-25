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

import android.os.SystemProperties;

import com.android.internal.widget.LockPatternUtils;

/**
 * GuardTalk sensor privacy + lockdown policy (T-SEC-P2-SENSOR / T-OS-CAMMIC-TOGGLE).
 *
 * <p>Product properties drive fail-closed mic/camera denial when the lock screen
 * is showing or before first unlock after boot, and lockdown sensor/network
 * fail-closed. See {@code vendor/guardtalk/docs/SENSOR_PRIVACY_LOCKDOWN_POLICY.md}.
 *
 * <p><strong>DEC-OS-UX-001 Gate 0:</strong> user toggle while interactively
 * unlocked is split from locked / pre-unlock / lockdown fail-closed. Lockdown
 * is not weakened: sensors cannot be forced ON while locked, CE-locked, or in
 * user lockdown. While the keyguard is dismissed and CE is unlocked, the user
 * may persist camera/microphone privacy ON or OFF.
 *
 * @hide
 */
public final class GuardTalkSensorPrivacyPolicy {

    /** When true, mic/camera AppOps are restricted while locked or pre-first-unlock. */
    public static final String PROP_SENSOR_PRIVACY_WHEN_LOCKED =
            "ro.guardtalk.sensor_privacy_when_locked";

    /**
     * When true, user lockdown ({@code STRONG_AUTH_REQUIRED_AFTER_USER_LOCKDOWN})
     * forces sensors denied and network fail-closed (airplane).
     */
    public static final String PROP_LOCKDOWN_FAIL_CLOSED = "ro.guardtalk.lockdown_fail_closed";

    private GuardTalkSensorPrivacyPolicy() {}

    /** Mic/camera denied when device locked or CE not unlocked yet. */
    public static boolean isSensorPrivacyWhenLockedEnabled() {
        return SystemProperties.getBoolean(PROP_SENSOR_PRIVACY_WHEN_LOCKED, false);
    }

    /** Lockdown path forces sensors + network fail-closed. */
    public static boolean isLockdownFailClosedEnabled() {
        // Implied by sensor-when-locked unless explicitly disabled.
        if (SystemProperties.get(PROP_LOCKDOWN_FAIL_CLOSED, "").isEmpty()) {
            return isSensorPrivacyWhenLockedEnabled();
        }
        return SystemProperties.getBoolean(PROP_LOCKDOWN_FAIL_CLOSED, false);
    }

    /** True when strong-auth flags indicate user lockdown. */
    public static boolean isLockdownActive(int strongAuthFlags) {
        return (strongAuthFlags
                & LockPatternUtils.StrongAuthTracker.STRONG_AUTH_REQUIRED_AFTER_USER_LOCKDOWN) != 0;
    }

    /**
     * Returns true when mic/camera must be AppOps-denied under GuardTalk policy.
     *
     * <p>Legacy 3-arg form: {@code deviceLocked} is treated as the lock-screen
     * signal (Settings helper still uses {@code KeyguardManager#isDeviceLocked}).
     *
     * @param deviceLocked {@link android.app.KeyguardManager#isDeviceLocked(int)}
     * @param userUnlocked {@link android.os.UserManager#isUserUnlocked(int)}
     * @param strongAuthFlags current strong-auth flags for the user (0 if unknown)
     */
    public static boolean mustDenySensors(
            boolean deviceLocked, boolean userUnlocked, int strongAuthFlags) {
        return mustDenySensors(deviceLocked, deviceLocked, userUnlocked, strongAuthFlags);
    }

    /**
     * Returns true when mic/camera must be AppOps-denied under GuardTalk policy.
     *
     * <p>Gate 0 (DEC-OS-UX-001): force-deny is lockscreen / pre-unlock /
     * lockdown only. A stale TrustManager {@code isDeviceLocked=true} while the
     * keyguard is dismissed does <em>not</em> force-deny (that blocked Settings
     * toggles while the user was interactively unlocked).
     *
     * @param keyguardShowing {@link android.app.KeyguardManager#isKeyguardLocked()}
     * @param deviceLocked {@link android.app.KeyguardManager#isDeviceLocked(int)}
     *        (kept for callers / dumpsys; unused once keyguard is dismissed)
     * @param userUnlocked {@link android.os.UserManager#isUserUnlocked(int)}
     * @param strongAuthFlags current strong-auth flags for the user (0 if unknown)
     */
    public static boolean mustDenySensors(
            boolean keyguardShowing,
            boolean deviceLocked,
            boolean userUnlocked,
            int strongAuthFlags) {
        if (isLockdownFailClosedEnabled() && isLockdownActive(strongAuthFlags)) {
            return true;
        }
        if (!isSensorPrivacyWhenLockedEnabled()) {
            return false;
        }
        if (!userUnlocked) {
            return true; // pre-first-unlock after boot
        }
        // Interactively unlocked: CE up and lockscreen not showing.
        // deviceLocked is intentionally not OR-ed here (DEC-OS-UX-001).
        return keyguardShowing;
    }

    /**
     * True when the user may persist a software sensor-privacy toggle.
     *
     * <p>Privacy ON (sensors muted) is always allowed so Settings/QS can turn
     * camera and microphone <em>off</em>. Privacy OFF (sensors live) is rejected
     * while {@link #mustDenySensors} applies.
     *
     * @param enablePrivacy true = privacy ON (block capture)
     */
    public static boolean allowUserToggle(
            boolean enablePrivacy,
            boolean keyguardShowing,
            boolean deviceLocked,
            boolean userUnlocked,
            int strongAuthFlags) {
        if (enablePrivacy) {
            return true;
        }
        return !mustDenySensors(
                keyguardShowing, deviceLocked, userUnlocked, strongAuthFlags);
    }
}
