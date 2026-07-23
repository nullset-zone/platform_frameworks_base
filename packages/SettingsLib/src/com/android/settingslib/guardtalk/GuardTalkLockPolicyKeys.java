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
 * Shared contract for GuardTalk password-only lock policy (T-SEC-P2-LOCK).
 *
 * <p>Bool resource <em>names</em> are overridden by
 * {@code GuardTalkSettingsOverlay}. Product properties reinforce server-side
 * fail-closed enforcement. Frontend {@code F-SEC-P2-SECURITY-SCREENS} consumes
 * this API — see {@code vendor/guardtalk/docs/PASSWORD_ONLY_LOCK_POLICY.md}.
 */
public final class GuardTalkLockPolicyKeys {

    /** Settings preference key on GuardTalk Security dashboard. */
    public static final String PREF_DEVICE_LOCK = "guardtalk_security_device_lock";

    /** Overlayable bool: password-only enrollment UI. */
    public static final String BOOL_PASSWORD_ONLY_LOCK = "config_guardtalk_password_only_lock";

    /** Overlayable bool: hide Smart Lock / trust-agent preferences. */
    public static final String BOOL_BLOCK_SMART_LOCK = "config_guardtalk_block_smart_lock";

    /** Overlayable bool: hide biometric enrollment surfaces in Settings. */
    public static final String BOOL_BLOCK_BIOMETRIC_ENROLL =
            "config_guardtalk_block_biometric_enroll";

    /** Product props (mirror of {@code android.guardtalk.GuardTalkLockPolicy}). */
    public static final String PROP_PASSWORD_ONLY_LOCK = "ro.guardtalk.password_only_lock";
    public static final String PROP_LOCK_AFTER_REBOOT = "ro.guardtalk.lock_after_reboot";
    public static final String PROP_MAX_LOCK_AFTER_TIMEOUT_MS =
            "ro.guardtalk.max_lock_after_timeout_ms";

    private GuardTalkLockPolicyKeys() {}
}
