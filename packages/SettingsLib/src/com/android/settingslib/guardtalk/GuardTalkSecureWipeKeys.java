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
 * Shared contract for GuardTalk Secure wipe + Duress (T-SEC-P2-WIPE).
 *
 * <p>Frontend {@code F-SEC-P2-SECURITY-SCREENS} consumes this API — see
 * {@code vendor/guardtalk/docs/SECURE_WIPE_SERVICE.md} and
 * {@code vendor/guardtalk/docs/DURESS_AND_ANTI_BRUTEFORCE_POLICY.md}.
 */
public final class GuardTalkSecureWipeKeys {

    /** Settings preference key: Secure wipe row on Security dashboard. */
    public static final String PREF_SECURE_WIPE = "guardtalk_security_secure_wipe";

    /** Settings preference key: Duress row on Security dashboard. */
    public static final String PREF_DURESS = "guardtalk_security_duress";

    /** Settings preference key: Anti-bruteforce policy row (informational). */
    public static final String PREF_ANTI_BRUTEFORCE = "guardtalk_security_anti_bruteforce";

    /** Settings preference key: Security status shell (post-unlock only). */
    public static final String PREF_SECURITY_STATUS = "guardtalk_security_status";

    /** Overlayable bool: Secure wipe / duress / anti-bruteforce UI + policy. */
    public static final String BOOL_SECURE_WIPE_ENABLED = "config_guardtalk_secure_wipe_enabled";

    /** Product prop (mirror of {@code android.guardtalk.GuardTalkSecureWipePolicy}). */
    public static final String PROP_SECURE_WIPE_ENABLED = "ro.guardtalk.secure_wipe_enabled";

    /** Product prop: wipe after N HW-backed failed unlocks (default 10). */
    public static final String PROP_ANTI_BRUTEFORCE_WIPE_THRESHOLD =
            "ro.guardtalk.anti_bruteforce_wipe_threshold";

    /** Canonical threshold. */
    public static final int DEFAULT_WIPE_THRESHOLD = 10;

    private GuardTalkSecureWipeKeys() {}
}
