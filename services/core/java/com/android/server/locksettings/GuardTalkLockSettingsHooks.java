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

package com.android.server.locksettings;

import static com.android.internal.widget.LockDomain.Secondary;
import static com.android.internal.widget.LockPatternUtils.StrongAuthTracker.STRONG_AUTH_REQUIRED_AFTER_BOOT;

import android.content.ContentResolver;
import android.content.Context;
import android.guardtalk.GuardTalkLockPolicy;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Slog;

import com.android.internal.widget.LockDomain;
import com.android.internal.widget.LockscreenCredential;

/**
 * Fail-closed GuardTalk hooks for {@link LockSettingsService} (T-SEC-P2-LOCK).
 *
 * <p>Keeps password-only / lock-after-reboot / inactivity policy out of the
 * core LSS methods for minimal footprint and easy rollback.
 */
final class GuardTalkLockSettingsHooks {
    private static final String TAG = "GtLockHooks";

    private GuardTalkLockSettingsHooks() {}

    /**
     * Enforce password-only credential types before LSS mutates the LSKF.
     *
     * @throws SecurityException when policy forbids the credential type
     */
    static void enforceSetLockCredential(LockscreenCredential credential, LockDomain lockDomain) {
        if (credential == null) {
            return;
        }
        GuardTalkLockPolicy.enforceCredentialTypeAllowed(
                credential.getType(), lockDomain == Secondary);
    }

    /**
     * Block Smart Lock / trust-agent enablement when password-only policy is on.
     *
     * @throws SecurityException when enabling non-empty trust agents
     */
    static void enforceSetString(String key, String value) {
        if (GuardTalkLockPolicy.shouldBlockTrustAgents(key, value)) {
            Slog.w(TAG, "Reject trust agent enablement (fail-closed)");
            throw new SecurityException(
                    "GuardTalk password-only lock: Smart Lock / trust agents forbidden");
        }
    }

    /**
     * Apply lock-after-reboot strong-auth flag and clamp inactivity timeout.
     * Called from {@link LockSettingsService#systemReady()}.
     */
    static void onSystemReady(Context context, LockSettingsService service) {
        if (GuardTalkLockPolicy.isLockAfterRebootEnabled()) {
            Slog.i(TAG, "Enforcing lock-after-reboot (STRONG_AUTH_REQUIRED_AFTER_BOOT)");
            service.requireStrongAuth(STRONG_AUTH_REQUIRED_AFTER_BOOT, UserHandle.USER_ALL);
        }
        if (!GuardTalkLockPolicy.isPasswordOnlyLockEnabled()) {
            return;
        }
        clampInactivityTimeout(context);
    }

    /**
     * Clamp {@link Settings.Secure#LOCK_SCREEN_LOCK_AFTER_TIMEOUT} to policy max.
     * Keyguard reads this setting for post-screen-off lock delay.
     */
    static void clampInactivityTimeout(Context context) {
        final ContentResolver cr = context.getContentResolver();
        final long maxMs = GuardTalkLockPolicy.getMaxLockAfterTimeoutMs();
        final long current = Settings.Secure.getLong(
                cr, Settings.Secure.LOCK_SCREEN_LOCK_AFTER_TIMEOUT, 5000L);
        final long clamped = GuardTalkLockPolicy.clampLockAfterTimeoutMs(current);
        if (clamped != current) {
            Settings.Secure.putLong(
                    cr, Settings.Secure.LOCK_SCREEN_LOCK_AFTER_TIMEOUT, clamped);
            Slog.i(TAG, "Clamped lock-after-timeout " + current + " → " + clamped
                    + " (max=" + maxMs + ")");
        }
    }
}
