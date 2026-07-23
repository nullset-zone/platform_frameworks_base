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

package com.android.settingslib.guardtalk;

/**
 * Shared contract for GuardTalk sensor privacy + lockdown (T-SEC-P2-SENSOR).
 *
 * <p>Frontend {@code F-SEC-P2-SECURITY-SCREENS} consumes this API — see
 * {@code vendor/guardtalk/docs/SENSOR_PRIVACY_LOCKDOWN_POLICY.md}.
 */
public final class GuardTalkSensorPrivacyKeys {

    /** Settings preference key: Sensor privacy row. */
    public static final String PREF_SENSOR_PRIVACY = "guardtalk_security_sensor_privacy";

    /** Settings preference key: Lockdown row (not a QS tile). */
    public static final String PREF_LOCKDOWN = "guardtalk_security_lockdown";

    /** Overlayable bool: show sensor-privacy-when-locked policy in UI. */
    public static final String BOOL_SENSOR_PRIVACY_WHEN_LOCKED =
            "config_guardtalk_sensor_privacy_when_locked";

    /** Overlayable bool: lockdown fail-closed (sensors + network). */
    public static final String BOOL_LOCKDOWN_FAIL_CLOSED =
            "config_guardtalk_lockdown_fail_closed";

    /** Product props (mirror of {@code android.guardtalk.GuardTalkSensorPrivacyPolicy}). */
    public static final String PROP_SENSOR_PRIVACY_WHEN_LOCKED =
            "ro.guardtalk.sensor_privacy_when_locked";
    public static final String PROP_LOCKDOWN_FAIL_CLOSED = "ro.guardtalk.lockdown_fail_closed";

    private GuardTalkSensorPrivacyKeys() {}
}
