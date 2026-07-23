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

import android.annotation.NonNull;
import android.app.KeyguardManager;
import android.content.Context;
import android.os.UserManager;

/**
 * GuardTalk Security status aggregator (T-SEC-P5-STATUS).
 *
 * <p>Collects post-unlock-readable policy/status markers for Settings and
 * services. Extends existing Phase-2/4/5 policy classes — no parallel stack.
 *
 * <p><strong>Hard rule:</strong> Duress armed/status is never a field here and
 * must never be surfaced on the lock screen or any pre-unlock surface. Callers
 * must gate UI with {@link #isPostUnlockStatusAllowed(Context)}.
 *
 * <p>See {@code vendor/guardtalk/docs/SECURITY_STATUS_API.md}.
 *
 * @hide
 */
public final class GuardTalkSecurityStatus {

    private GuardTalkSecurityStatus() {}

    /**
     * True when Security status may be shown: CE unlocked and keyguard not
     * holding the device locked. Fail-closed when services are missing.
     */
    public static boolean isPostUnlockStatusAllowed(@NonNull Context context) {
        final int userId = context.getUserId();
        final UserManager um = context.getSystemService(UserManager.class);
        final KeyguardManager km = context.getSystemService(KeyguardManager.class);
        if (um == null || !um.isUserUnlocked(userId)) {
            return false;
        }
        if (km == null || km.isDeviceLocked(userId)) {
            return false;
        }
        return true;
    }

    /**
     * Collects aggregate status. When {@code requirePostUnlock} is true and the
     * device is locked / CE locked, returns {@link Snapshot#denied()} with
     * {@link Snapshot#postUnlockAllowed} false and all markers cleared.
     *
     * <p>Never includes Duress armed state.
     *
     * @param context caller context (user-scoped)
     * @param requirePostUnlock when true, gate on {@link #isPostUnlockStatusAllowed}
     * @param sensorsForceDenied runtime sensor deny (from Settings helpers)
     * @param usbDataForceDenied runtime USB deny (from Settings helpers)
     * @param lockdownActive lockdown strong-auth active
     * @param autoRebootHours profile hours, or {@code 0} when Off / unknown
     */
    @NonNull
    public static Snapshot collect(
            @NonNull Context context,
            boolean requirePostUnlock,
            boolean sensorsForceDenied,
            boolean usbDataForceDenied,
            boolean lockdownActive,
            int autoRebootHours) {
        if (requirePostUnlock && !isPostUnlockStatusAllowed(context)) {
            return Snapshot.denied();
        }
        return new Snapshot(
                /* postUnlockAllowed= */ true,
                GuardTalkLockPolicy.isPasswordOnlyLockEnabled(),
                GuardTalkSensorPrivacyPolicy.isSensorPrivacyWhenLockedEnabled(),
                sensorsForceDenied,
                GuardTalkUsbProtectionPolicy.isFailClosedEnabled(),
                usbDataForceDenied,
                GuardTalkSensorPrivacyPolicy.isLockdownFailClosedEnabled(),
                lockdownActive,
                GuardTalkAutoRebootPolicy.isProfilesEnabled(),
                Math.max(0, autoRebootHours),
                GuardTalkSecureWipePolicy.isSecureWipeEnabled(),
                GuardTalkSecureWipePolicy.getAntiBruteforceWipeThreshold(),
                GuardTalkPrivacyPolicy.isPrivacyTmpfsEnabled(),
                GuardTalkPrivacyPolicy.isClipboardClearEnabled(),
                GuardTalkPrivacyPolicy.getClipboardClearTimeoutMs(),
                GuardTalkFilesPolicy.isEnabled(),
                GuardTalkFilesPolicy.isTrashEnabled(),
                GuardTalkFilesPolicy.isCriticalProtectEnabled(),
                GuardTalkProductionHardeningPolicy.isEnabled(),
                GuardTalkProductionHardeningPolicy.isProductionProfile(),
                GuardTalkProductionHardeningPolicy.isDebuggableOff(),
                GuardTalkProductionHardeningPolicy.mustBlockUnknownSources());
    }

    /**
     * Prop-only markers (no runtime lock/USB/sensor deny). Still never includes
     * Duress. Prefer {@link #collect} for Settings UI.
     */
    @NonNull
    public static Snapshot collectPolicyMarkers() {
        return new Snapshot(
                /* postUnlockAllowed= */ true,
                GuardTalkLockPolicy.isPasswordOnlyLockEnabled(),
                GuardTalkSensorPrivacyPolicy.isSensorPrivacyWhenLockedEnabled(),
                /* sensorsForceDenied= */ false,
                GuardTalkUsbProtectionPolicy.isFailClosedEnabled(),
                /* usbDataForceDenied= */ false,
                GuardTalkSensorPrivacyPolicy.isLockdownFailClosedEnabled(),
                /* lockdownActive= */ false,
                GuardTalkAutoRebootPolicy.isProfilesEnabled(),
                /* autoRebootHours= */ 0,
                GuardTalkSecureWipePolicy.isSecureWipeEnabled(),
                GuardTalkSecureWipePolicy.getAntiBruteforceWipeThreshold(),
                GuardTalkPrivacyPolicy.isPrivacyTmpfsEnabled(),
                GuardTalkPrivacyPolicy.isClipboardClearEnabled(),
                GuardTalkPrivacyPolicy.getClipboardClearTimeoutMs(),
                GuardTalkFilesPolicy.isEnabled(),
                GuardTalkFilesPolicy.isTrashEnabled(),
                GuardTalkFilesPolicy.isCriticalProtectEnabled(),
                GuardTalkProductionHardeningPolicy.isEnabled(),
                GuardTalkProductionHardeningPolicy.isProductionProfile(),
                GuardTalkProductionHardeningPolicy.isDebuggableOff(),
                GuardTalkProductionHardeningPolicy.mustBlockUnknownSources());
    }

    /**
     * Immutable aggregate. <strong>No Duress field by design.</strong>
     *
     * @hide
     */
    public static final class Snapshot {
        public final boolean postUnlockAllowed;
        public final boolean passwordOnlyLock;
        public final boolean sensorPrivacyPolicy;
        public final boolean sensorsForceDenied;
        public final boolean usbProtectionPolicy;
        public final boolean usbDataForceDenied;
        public final boolean lockdownFailClosed;
        public final boolean lockdownActive;
        public final boolean autoRebootProfiles;
        /** Profile hours when on; {@code 0} means Off / unknown. */
        public final int autoRebootHours;
        public final boolean secureWipeAvailable;
        /** Wipe threshold; {@code 0} means anti-bruteforce wipe disabled. */
        public final int antiBruteforceThreshold;
        public final boolean privacyTmpfs;
        public final boolean clipboardClear;
        public final long clipboardClearTimeoutMs;
        public final boolean filesPolicy;
        public final boolean filesTrash;
        public final boolean filesCriticalProtect;
        public final boolean productionHardening;
        public final boolean productionProfile;
        public final boolean debuggableOff;
        public final boolean blockUnknownSources;

        Snapshot(
                boolean postUnlockAllowed,
                boolean passwordOnlyLock,
                boolean sensorPrivacyPolicy,
                boolean sensorsForceDenied,
                boolean usbProtectionPolicy,
                boolean usbDataForceDenied,
                boolean lockdownFailClosed,
                boolean lockdownActive,
                boolean autoRebootProfiles,
                int autoRebootHours,
                boolean secureWipeAvailable,
                int antiBruteforceThreshold,
                boolean privacyTmpfs,
                boolean clipboardClear,
                long clipboardClearTimeoutMs,
                boolean filesPolicy,
                boolean filesTrash,
                boolean filesCriticalProtect,
                boolean productionHardening,
                boolean productionProfile,
                boolean debuggableOff,
                boolean blockUnknownSources) {
            this.postUnlockAllowed = postUnlockAllowed;
            this.passwordOnlyLock = passwordOnlyLock;
            this.sensorPrivacyPolicy = sensorPrivacyPolicy;
            this.sensorsForceDenied = sensorsForceDenied;
            this.usbProtectionPolicy = usbProtectionPolicy;
            this.usbDataForceDenied = usbDataForceDenied;
            this.lockdownFailClosed = lockdownFailClosed;
            this.lockdownActive = lockdownActive;
            this.autoRebootProfiles = autoRebootProfiles;
            this.autoRebootHours = autoRebootHours;
            this.secureWipeAvailable = secureWipeAvailable;
            this.antiBruteforceThreshold = antiBruteforceThreshold;
            this.privacyTmpfs = privacyTmpfs;
            this.clipboardClear = clipboardClear;
            this.clipboardClearTimeoutMs = clipboardClearTimeoutMs;
            this.filesPolicy = filesPolicy;
            this.filesTrash = filesTrash;
            this.filesCriticalProtect = filesCriticalProtect;
            this.productionHardening = productionHardening;
            this.productionProfile = productionProfile;
            this.debuggableOff = debuggableOff;
            this.blockUnknownSources = blockUnknownSources;
        }

        /** Empty snapshot for pre-unlock / denied callers. */
        @NonNull
        public static Snapshot denied() {
            return create(
                    false, false, false, false, false, false, false, false,
                    false, 0, false, 0, false, false, 0L, false, false, false,
                    false, false, false, false);
        }

        /**
         * Factory for Settings / services that assemble overlay-aware fields.
         * Callers must omit Duress entirely.
         */
        @NonNull
        public static Snapshot create(
                boolean postUnlockAllowed,
                boolean passwordOnlyLock,
                boolean sensorPrivacyPolicy,
                boolean sensorsForceDenied,
                boolean usbProtectionPolicy,
                boolean usbDataForceDenied,
                boolean lockdownFailClosed,
                boolean lockdownActive,
                boolean autoRebootProfiles,
                int autoRebootHours,
                boolean secureWipeAvailable,
                int antiBruteforceThreshold,
                boolean privacyTmpfs,
                boolean clipboardClear,
                long clipboardClearTimeoutMs,
                boolean filesPolicy,
                boolean filesTrash,
                boolean filesCriticalProtect,
                boolean productionHardening,
                boolean productionProfile,
                boolean debuggableOff,
                boolean blockUnknownSources) {
            return new Snapshot(
                    postUnlockAllowed,
                    passwordOnlyLock,
                    sensorPrivacyPolicy,
                    sensorsForceDenied,
                    usbProtectionPolicy,
                    usbDataForceDenied,
                    lockdownFailClosed,
                    lockdownActive,
                    autoRebootProfiles,
                    autoRebootHours,
                    secureWipeAvailable,
                    antiBruteforceThreshold,
                    privacyTmpfs,
                    clipboardClear,
                    clipboardClearTimeoutMs,
                    filesPolicy,
                    filesTrash,
                    filesCriticalProtect,
                    productionHardening,
                    productionProfile,
                    debuggableOff,
                    blockUnknownSources);
        }
    }
}
