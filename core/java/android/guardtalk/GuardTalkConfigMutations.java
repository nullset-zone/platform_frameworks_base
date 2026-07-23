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

/**
 * Mutation keys gated by {@link GuardTalkConfigGateManager}.
 *
 * <p>Phase-2 owners implement behavior; Phase-1 only defines the gate contract.
 * Wipe/duress execution is intentionally out of scope for P1.
 *
 * @hide
 */
public final class GuardTalkConfigMutations {

    /** Generic GT Config write / maintenance unlock. */
    public static final String GT_CONFIG_WRITE = "gt_config_write";

    /** Maintenance surfaces after main password (Apps/Network gated entries). */
    public static final String MAINTENANCE_ACCESS = "maintenance_access";

    public static final String SECURITY_DEVICE_LOCK = "security_device_lock";
    public static final String SECURITY_SENSOR_PRIVACY = "security_sensor_privacy";
    public static final String SECURITY_USB_PROTECTION = "security_usb_protection";
    public static final String SECURITY_LOCKDOWN = "security_lockdown";
    public static final String SECURITY_AUTO_REBOOT = "security_auto_reboot";

    /**
     * Duress configuration mutations. Gate rejects unauthorized callers;
     * wipe execution belongs to T-SEC-P2-WIPE.
     */
    public static final String SECURITY_DURESS_CONFIG = "security_duress_config";

    /**
     * Secure wipe (user-requested crypto-erase) mutation. Execution uses
     * {@code LockPatternUtils#requestSecureWipe} → SecureWipeEngine.
     */
    public static final String SECURITY_SECURE_WIPE = "security_secure_wipe";

    private GuardTalkConfigMutations() {}
}
