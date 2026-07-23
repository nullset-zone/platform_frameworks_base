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
 * Shared preference keys for GuardTalk Security status (T-SEC-P5-STATUS).
 *
 * <p>Consumed by Settings {@code GuardTalkSecurityStatusFragment} and Frontend
 * {@code F-SEC-P5-STATUS-UI}. See
 * {@code vendor/guardtalk/docs/SECURITY_STATUS_API.md}.
 *
 * <p><strong>Never</strong> add a Duress status preference key here — Duress
 * armed/status must not appear on lock screen or Security status.
 */
public final class GuardTalkSecurityStatusKeys {

    /** Dashboard entry → status screen. */
    public static final String PREF_SECURITY_STATUS = "guardtalk_security_status";

    public static final String PREF_STATUS_DEVICE_LOCK = "guardtalk_status_device_lock";
    public static final String PREF_STATUS_SENSOR_PRIVACY = "guardtalk_status_sensor_privacy";
    public static final String PREF_STATUS_USB_PROTECTION = "guardtalk_status_usb_protection";
    public static final String PREF_STATUS_LOCKDOWN = "guardtalk_status_lockdown";
    public static final String PREF_STATUS_AUTO_REBOOT = "guardtalk_status_auto_reboot";
    public static final String PREF_STATUS_ANTI_BRUTEFORCE = "guardtalk_status_anti_bruteforce";
    public static final String PREF_STATUS_SECURE_WIPE = "guardtalk_status_secure_wipe";
    public static final String PREF_STATUS_CLIPBOARD = "guardtalk_status_clipboard";
    public static final String PREF_STATUS_PRIVACY_TMPFS = "guardtalk_status_privacy_tmpfs";
    public static final String PREF_STATUS_FILES = "guardtalk_status_files";
    public static final String PREF_STATUS_HARDENING = "guardtalk_status_hardening";
    public static final String PREF_STATUS_FOOTER = "guardtalk_status_footer";

    private GuardTalkSecurityStatusKeys() {}
}
