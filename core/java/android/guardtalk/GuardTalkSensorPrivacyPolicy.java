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
 * GuardTalk sensor privacy + lockdown policy (T-SEC-P2-SENSOR).
 *
 * <p>Product properties drive fail-closed mic/camera denial when the device is
 * locked or before first unlock after boot, and lockdown sensor/network
 * fail-closed. See {@code vendor/guardtalk/docs/SENSOR_PRIVACY_LOCKDOWN_POLICY.md}.
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
     * Returns true when mic/camera must be denied under GuardTalk policy.
     *
     * @param deviceLocked {@link android.app.KeyguardManager#isDeviceLocked(int)}
     * @param userUnlocked {@link android.os.UserManager#isUserUnlocked(int)}
     * @param strongAuthFlags current strong-auth flags for the user (0 if unknown)
     */
    public static boolean mustDenySensors(
            boolean deviceLocked, boolean userUnlocked, int strongAuthFlags) {
        if (isLockdownFailClosedEnabled() && isLockdownActive(strongAuthFlags)) {
            return true;
        }
        if (!isSensorPrivacyWhenLockedEnabled()) {
            return false;
        }
        if (!userUnlocked) {
            return true; // pre-first-unlock after boot
        }
        return deviceLocked;
    }
}
