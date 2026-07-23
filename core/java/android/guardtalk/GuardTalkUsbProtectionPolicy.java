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

import android.ext.settings.UsbPortSecurity;
import android.os.SystemProperties;

/**
 * GuardTalk USB protection policy (T-SEC-P2-USB).
 *
 * <p>When enabled, USB data is fail-closed while the device is locked or before
 * first unlock after boot (CE locked). Enforcement extends GrapheneOS
 * {@code UsbPortSecurityHooks} — see
 * {@code vendor/guardtalk/docs/USB_PROTECTION_POLICY.md}.
 *
 * @hide
 */
public final class GuardTalkUsbProtectionPolicy {

    /**
     * When true, USB data (ADB/MTP/PTP/sideload) is denied while locked or
     * pre-first-unlock; charging-only is forced. Modes that allow data before
     * first unlock or while locked are clamped.
     */
    public static final String PROP_USB_PROTECTION_FAIL_CLOSED =
            "ro.guardtalk.usb_protection_fail_closed";

    /**
     * USB gadget data functions blocked under fail-closed deny
     * (ADB, MTP, PTP — covers APK-over-USB / sideload).
     */
    public static final long BLOCKED_USB_DATA_FUNCTIONS =
            android.hardware.usb.UsbManager.FUNCTION_ADB
                    | android.hardware.usb.UsbManager.FUNCTION_MTP
                    | android.hardware.usb.UsbManager.FUNCTION_PTP;

    private GuardTalkUsbProtectionPolicy() {}

    /** Product policy on: fail-closed USB data when locked / pre-unlock. */
    public static boolean isFailClosedEnabled() {
        return SystemProperties.getBoolean(PROP_USB_PROTECTION_FAIL_CLOSED, false);
    }

    /**
     * Returns true when USB data functions must be denied.
     *
     * @param deviceLocked {@link android.app.KeyguardManager#isDeviceLocked(int)}
     * @param userUnlocked {@link android.os.UserManager#isUserUnlocked(int)}
     */
    public static boolean mustDenyUsbData(boolean deviceLocked, boolean userUnlocked) {
        if (!isFailClosedEnabled()) {
            return false;
        }
        if (!userUnlocked) {
            return true; // pre-first-unlock after boot
        }
        return deviceLocked;
    }

    /**
     * Clamps a persisted USB port-security mode to GuardTalk fail-closed
     * semantics. Modes that enable data while locked or before first unlock
     * become {@link UsbPortSecurity#MODE_CHARGING_ONLY_WHEN_LOCKED}.
     */
    public static int clampPortSecurityMode(int mode) {
        if (!isFailClosedEnabled()) {
            return mode;
        }
        switch (mode) {
            case UsbPortSecurity.MODE_ALL_PORTS_ENABLED:
            case UsbPortSecurity.MODE_CHARGING_ONLY_WHEN_LOCKED_AFU:
                return UsbPortSecurity.MODE_CHARGING_ONLY_WHEN_LOCKED;
            default:
                return mode;
        }
    }

    /** Strip ADB/MTP/PTP from a gadget function mask when denying data. */
    public static long stripBlockedDataFunctions(long functions) {
        return functions & ~BLOCKED_USB_DATA_FUNCTIONS;
    }
}
