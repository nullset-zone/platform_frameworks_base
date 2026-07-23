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

import static com.android.internal.widget.LockPatternUtils.CREDENTIAL_TYPE_NONE;
import static com.android.internal.widget.LockPatternUtils.CREDENTIAL_TYPE_PASSWORD;
import static com.android.internal.widget.LockPatternUtils.CREDENTIAL_TYPE_PATTERN;
import static com.android.internal.widget.LockPatternUtils.CREDENTIAL_TYPE_PIN;

import android.annotation.NonNull;
import android.os.SystemProperties;
import android.util.Slog;

/**
 * GuardTalk password-only device lock policy (T-SEC-P2-LOCK).
 *
 * <p>Product properties (tokay GuardTalk layer) drive fail-closed server
 * enforcement in {@code LockSettingsService}. Settings UI mirrors the same
 * props via overlay bools — see
 * {@code vendor/guardtalk/docs/PASSWORD_ONLY_LOCK_POLICY.md}.
 *
 * @hide
 */
public final class GuardTalkLockPolicy {

    private static final String TAG = "GtLockPolicy";

    /** When true, only alphabetic password LSKFs may be enrolled. */
    public static final String PROP_PASSWORD_ONLY_LOCK = "ro.guardtalk.password_only_lock";

    /** When true, strong auth is required after every reboot (password). */
    public static final String PROP_LOCK_AFTER_REBOOT = "ro.guardtalk.lock_after_reboot";

    /**
     * Maximum {@link android.provider.Settings.Secure#LOCK_SCREEN_LOCK_AFTER_TIMEOUT}
     * in milliseconds. Values above this are clamped. {@code 0} = use
     * {@link #DEFAULT_MAX_LOCK_AFTER_TIMEOUT_MS}.
     */
    public static final String PROP_MAX_LOCK_AFTER_TIMEOUT_MS =
            "ro.guardtalk.max_lock_after_timeout_ms";

    /** Default max lock-after-timeout when prop unset/zero (5 minutes). */
    public static final long DEFAULT_MAX_LOCK_AFTER_TIMEOUT_MS = 5L * 60L * 1000L;

    /** LockSettings storage key for enabled trust agents (Smart Lock). */
    public static final String KEY_ENABLED_TRUST_AGENTS = "lockscreen.enabledtrustagents";

    private GuardTalkLockPolicy() {}

    /** Password-only enrollment enforced (PIN/pattern/biometric blocked). */
    public static boolean isPasswordOnlyLockEnabled() {
        return SystemProperties.getBoolean(PROP_PASSWORD_ONLY_LOCK, false);
    }

    /** Lock-after-reboot (strong auth after boot) enforced. */
    public static boolean isLockAfterRebootEnabled() {
        // Fail-closed with password-only: if password-only is on, reboot lock
        // is implied unless the reboot prop is explicitly set to 0.
        if (SystemProperties.get(PROP_LOCK_AFTER_REBOOT, "").isEmpty()) {
            return isPasswordOnlyLockEnabled();
        }
        return SystemProperties.getBoolean(PROP_LOCK_AFTER_REBOOT, false);
    }

    /** Max inactivity / lock-after-timeout in ms (clamped policy). */
    public static long getMaxLockAfterTimeoutMs() {
        final long configured = SystemProperties.getLong(PROP_MAX_LOCK_AFTER_TIMEOUT_MS, 0L);
        if (configured > 0L) {
            return configured;
        }
        return isPasswordOnlyLockEnabled() ? DEFAULT_MAX_LOCK_AFTER_TIMEOUT_MS : Long.MAX_VALUE;
    }

    /**
     * Returns true when {@code credentialType} is allowed for a new primary
     * lock under GuardTalk policy. Fail-closed: when policy enabled, only
     * {@link CREDENTIAL_TYPE_PASSWORD} and {@link CREDENTIAL_TYPE_NONE}
     * (clear) are allowed.
     */
    public static boolean isCredentialTypeAllowed(int credentialType) {
        if (!isPasswordOnlyLockEnabled()) {
            return true;
        }
        return credentialType == CREDENTIAL_TYPE_PASSWORD
                || credentialType == CREDENTIAL_TYPE_NONE;
    }

    /**
     * Throws {@link SecurityException} if primary credential type is forbidden.
     * Secondary (biometric second-factor PIN) is always forbidden when policy on.
     */
    public static void enforceCredentialTypeAllowed(int credentialType, boolean secondaryDomain) {
        if (!isPasswordOnlyLockEnabled()) {
            return;
        }
        if (secondaryDomain) {
            Slog.w(TAG, "Reject secondary/biometric-factor credential (fail-closed)");
            throw new SecurityException(
                    "GuardTalk password-only lock: biometric second factor forbidden");
        }
        if (credentialType == CREDENTIAL_TYPE_PIN
                || credentialType == CREDENTIAL_TYPE_PATTERN) {
            Slog.w(TAG, "Reject credential type " + credentialType + " (fail-closed)");
            throw new SecurityException(
                    "GuardTalk password-only lock: PIN/pattern enrollment forbidden");
        }
        if (!isCredentialTypeAllowed(credentialType)) {
            Slog.w(TAG, "Reject credential type " + credentialType + " (fail-closed)");
            throw new SecurityException(
                    "GuardTalk password-only lock: credential type forbidden");
        }
    }

    /**
     * Returns true if Smart Lock / trust-agent enablement must be blocked.
     * Empty/null values (clearing agents) are allowed.
     */
    public static boolean shouldBlockTrustAgents(@NonNull String key, String value) {
        if (!isPasswordOnlyLockEnabled()) {
            return false;
        }
        if (!KEY_ENABLED_TRUST_AGENTS.equals(key)) {
            return false;
        }
        return value != null && !value.isEmpty();
    }

    /** Clamp a proposed lock-after-timeout to the policy maximum. */
    public static long clampLockAfterTimeoutMs(long timeoutMs) {
        if (!isPasswordOnlyLockEnabled() && !isLockAfterRebootEnabled()) {
            return timeoutMs;
        }
        final long max = getMaxLockAfterTimeoutMs();
        if (timeoutMs < 0L) {
            return 0L;
        }
        return Math.min(timeoutMs, max);
    }
}
