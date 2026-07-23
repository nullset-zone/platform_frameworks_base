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

/**
 * GuardTalk production hardening policy (T-SEC-P5-HARDEN).
 *
 * <p>Product props drive enforceably-applied knobs (unknown-source install
 * block, status markers). SELinux/Verified Boot/KeyMint/debug-off primarily
 * come from the {@code user} lunch variant + GrapheneOS/Pixel vendor stack;
 * see {@code vendor/guardtalk/docs/PRODUCTION_HARDENING_POLICY.md}.
 *
 * @hide
 */
public final class GuardTalkProductionHardeningPolicy {

    /** Master switch for GuardTalk production-hardening markers + install block. */
    public static final String PROP_PRODUCTION_HARDENING =
            "ro.guardtalk.production_hardening";

    /**
     * Set only on {@code TARGET_BUILD_VARIANT=user} images. Status UI should
     * treat this as the production profile bit (alongside {@code ro.debuggable=0}).
     */
    public static final String PROP_PRODUCTION_PROFILE =
            "ro.guardtalk.production_profile";

    /** When true, PackageManager denies unknown-source / sideload installs. */
    public static final String PROP_BLOCK_UNKNOWN_SOURCES =
            "ro.guardtalk.block_unknown_sources";

    /** Marker: accessibility service enroll restricted (Settings hide + policy). */
    public static final String PROP_RESTRICT_ACCESSIBILITY =
            "ro.guardtalk.restrict_accessibility_services";

    /** Marker: display overlay / SYSTEM_ALERT_WINDOW posture. */
    public static final String PROP_RESTRICT_DISPLAY_OVERLAYS =
            "ro.guardtalk.restrict_display_overlays";

    /** Marker: dynamic-code / JIT posture (inherits GrapheneOS hardened_malloc). */
    public static final String PROP_RESTRICT_DYNAMIC_CODE =
            "ro.guardtalk.restrict_dynamic_code";

    /** Marker: SELinux Enforcing required for production acceptance. */
    public static final String PROP_SELINUX_ENFORCING_REQUIRED =
            "ro.guardtalk.selinux_enforcing_required";

    /** Marker: Verified Boot / AVB green-state required for production. */
    public static final String PROP_VERIFIED_BOOT_REQUIRED =
            "ro.guardtalk.verified_boot_required";

    /** Marker: KeyMint / HW crypto HAL required (Pixel citadel KeyMint). */
    public static final String PROP_KEYMINT_REQUIRED =
            "ro.guardtalk.keymint_required";

    /** Marker: init.guardtalk.hardening.rc sysctl writes enabled. */
    public static final String PROP_SYSCTL_HARDENING =
            "ro.guardtalk.sysctl_hardening";

    private GuardTalkProductionHardeningPolicy() {}

    /** Product production-hardening master switch. */
    public static boolean isEnabled() {
        return SystemProperties.getBoolean(PROP_PRODUCTION_HARDENING, false);
    }

    /**
     * True on production {@code user} lunch images that set
     * {@link #PROP_PRODUCTION_PROFILE}.
     */
    public static boolean isProductionProfile() {
        return SystemProperties.getBoolean(PROP_PRODUCTION_PROFILE, false);
    }

    /**
     * True when the running image reports non-debuggable (typical {@code user}
     * variant). Independent of GuardTalk props — mirrors AOSP lunch semantics.
     */
    public static boolean isDebuggableOff() {
        return !SystemProperties.getBoolean("ro.debuggable", true);
    }

    /**
     * Fail-closed unknown-source install block when hardening master is on
     * and the block prop is unset/true.
     */
    public static boolean mustBlockUnknownSources() {
        if (!isEnabled()) {
            return false;
        }
        return SystemProperties.getBoolean(PROP_BLOCK_UNKNOWN_SOURCES, true);
    }

    public static boolean isAccessibilityRestricted() {
        return isEnabled()
                && SystemProperties.getBoolean(PROP_RESTRICT_ACCESSIBILITY, true);
    }

    public static boolean isDisplayOverlayRestricted() {
        return isEnabled()
                && SystemProperties.getBoolean(PROP_RESTRICT_DISPLAY_OVERLAYS, true);
    }

    public static boolean isDynamicCodeRestricted() {
        return isEnabled()
                && SystemProperties.getBoolean(PROP_RESTRICT_DYNAMIC_CODE, true);
    }

    public static boolean isSelinuxEnforcingRequired() {
        return isEnabled()
                && SystemProperties.getBoolean(PROP_SELINUX_ENFORCING_REQUIRED, true);
    }

    public static boolean isVerifiedBootRequired() {
        return isEnabled()
                && SystemProperties.getBoolean(PROP_VERIFIED_BOOT_REQUIRED, true);
    }

    public static boolean isKeymintRequired() {
        return isEnabled()
                && SystemProperties.getBoolean(PROP_KEYMINT_REQUIRED, true);
    }

    public static boolean isSysctlHardeningEnabled() {
        return isEnabled()
                && SystemProperties.getBoolean(PROP_SYSCTL_HARDENING, true);
    }
}
