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
 * Shared contract for GuardTalk USB protection (T-SEC-P2-USB).
 *
 * <p>Frontend {@code F-SEC-P2-SECURITY-SCREENS} consumes this API — see
 * {@code vendor/guardtalk/docs/USB_PROTECTION_POLICY.md}.
 */
public final class GuardTalkUsbProtectionKeys {

    /** Settings preference key: USB protection row. */
    public static final String PREF_USB_PROTECTION = "guardtalk_security_usb_protection";

    /** Overlayable bool: USB fail-closed when locked / pre-first-unlock. */
    public static final String BOOL_USB_PROTECTION_FAIL_CLOSED =
            "config_guardtalk_usb_protection_fail_closed";

    /** Product prop (mirror of {@code android.guardtalk.GuardTalkUsbProtectionPolicy}). */
    public static final String PROP_USB_PROTECTION_FAIL_CLOSED =
            "ro.guardtalk.usb_protection_fail_closed";

    /**
     * GrapheneOS persist mode used as GuardTalk default
     * ({@code UsbPortSecurity.MODE_CHARGING_ONLY_WHEN_LOCKED}).
     */
    public static final String PROP_USB_MODE = "persist.security.usb_mode";

    private GuardTalkUsbProtectionKeys() {}
}
