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
 * Shared contract for GuardTalk Auto-reboot (T-SEC-P2-AUTOREBOOT).
 *
 * <p>Frontend {@code F-SEC-P2-SECURITY-SCREENS} consumes this API — see
 * {@code vendor/guardtalk/docs/AUTO_REBOOT_POLICY.md}.
 */
public final class GuardTalkAutoRebootKeys {

    /** Settings preference key: Auto-reboot row on Security dashboard. */
    public static final String PREF_AUTO_REBOOT = "guardtalk_security_auto_reboot";

    /** Overlayable bool: GuardTalk Auto-reboot profiles + exclusion windows. */
    public static final String BOOL_AUTO_REBOOT_PROFILES =
            "config_guardtalk_auto_reboot_profiles";

    /** Product prop (mirror of {@code android.guardtalk.GuardTalkAutoRebootPolicy}). */
    public static final String PROP_AUTO_REBOOT_PROFILES =
            "ro.guardtalk.auto_reboot_profiles";

    /** Product prop: default profile timeout in ms when Global value is unknown. */
    public static final String PROP_AUTO_REBOOT_DEFAULT_MS =
            "ro.guardtalk.auto_reboot_default_ms";

    /** GrapheneOS / ExtSettings Global key for the timeout (ms). */
    public static final String GLOBAL_AUTO_REBOOT_TIMEOUT = "settings_reboot_after_timeout";

    private GuardTalkAutoRebootKeys() {}
}
