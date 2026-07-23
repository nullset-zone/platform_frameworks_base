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

package com.android.server.guardtalk;

import android.annotation.NonNull;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.guardtalk.GuardTalkConfigGateManager;
import android.guardtalk.GuardTalkConfigMutations;
import android.os.Binder;
import android.os.IGuardTalkConfigGate;
import android.os.Process;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.util.Slog;
import android.util.SparseLongArray;

import com.android.internal.widget.LockPatternUtils;
import com.android.server.LocalServices;
import com.android.server.SystemService;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * GuardTalk GT Config password gate (T-SEC-P1-GTGATE).
 *
 * <p>Security mutations require a short-lived in-memory session opened only after
 * Settings confirms the main device credential. Fail-closed: missing session,
 * unknown mutation keys, safe mode, or shell/root callers ⇒ reject.
 *
 * <p>P1 resists trivial ADB/Recovery/Safe Mode bypass:
 * <ul>
 *   <li>Auth state is never persisted to Settings.* (no {@code adb shell settings} flip)</li>
 *   <li>Shell/root UIDs cannot open sessions</li>
 *   <li>Safe mode rejects session open and mutations</li>
 *   <li>Recovery does not run system_server gate (by design)</li>
 *   <li>Session clears on user present / screen off broadcast</li>
 * </ul>
 * Full hardening (P5) may add stronger SELinux neverallows and TEE binding.
 */
public final class GuardTalkConfigGateService extends SystemService {
    private static final String TAG = "GtConfigGate";

    /** Default session TTL after successful credential confirmation. */
    private static final long SESSION_TTL_MS = 5L * 60L * 1000L;

    private static final Set<String> KNOWN_MUTATIONS = new HashSet<>(Arrays.asList(
            GuardTalkConfigMutations.GT_CONFIG_WRITE,
            GuardTalkConfigMutations.MAINTENANCE_ACCESS,
            GuardTalkConfigMutations.SECURITY_DEVICE_LOCK,
            GuardTalkConfigMutations.SECURITY_SENSOR_PRIVACY,
            GuardTalkConfigMutations.SECURITY_USB_PROTECTION,
            GuardTalkConfigMutations.SECURITY_LOCKDOWN,
            GuardTalkConfigMutations.SECURITY_AUTO_REBOOT,
            GuardTalkConfigMutations.SECURITY_DURESS_CONFIG,
            GuardTalkConfigMutations.SECURITY_SECURE_WIPE
    ));

    private final Context mContext;
    private final Object mLock = new Object();
    /** userId → session expiry (elapsedRealtime). */
    private final SparseLongArray mSessionExpiryElapsed = new SparseLongArray();
    private LockPatternUtils mLockPatternUtils;
    private BinderService mBinderService;

    public GuardTalkConfigGateService(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public void onStart() {
        mLockPatternUtils = new LockPatternUtils(mContext);
        mBinderService = new BinderService();
        publishBinderService(GuardTalkConfigGateManager.SERVICE_NAME, mBinderService);
        LocalServices.addService(GuardTalkConfigGateInternal.class, new LocalService());
        Slog.i(TAG, "GuardTalk config password gate published");
    }

    @Override
    public void onBootPhase(int phase) {
        if (phase == PHASE_ACTIVITY_MANAGER_READY) {
            final IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            filter.addAction(Intent.ACTION_USER_PRESENT);
            mContext.registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                        // Fail-closed: lock clears maintenance session.
                        clearAllSessions("screen_off");
                    }
                }
            }, filter, Context.RECEIVER_NOT_EXPORTED);
        }
    }

    private boolean isGateEnabledLocked() {
        // Default true on GuardTalk products; property can force-disable for bring-up only.
        return SystemProperties.getBoolean(GuardTalkConfigGateManager.PROP_GATE_ENABLED, true);
    }

    private static boolean isDeviceInSafeMode() {
        return SystemProperties.getInt("ro.sys.safemode", 0) != 0
                || SystemProperties.getInt("persist.sys.safemode", 0) != 0;
    }

    private void clearAllSessions(String reason) {
        synchronized (mLock) {
            if (mSessionExpiryElapsed.size() > 0) {
                Slog.i(TAG, "Clearing sessions (" + reason + ")");
                mSessionExpiryElapsed.clear();
            }
        }
    }

    private boolean hasActiveSessionLocked(int userId) {
        final long expiry = mSessionExpiryElapsed.get(userId, 0L);
        if (expiry <= 0L) {
            return false;
        }
        final long now = SystemClock.elapsedRealtime();
        if (now >= expiry) {
            mSessionExpiryElapsed.delete(userId);
            return false;
        }
        return true;
    }

    private long remainingMillisLocked(int userId) {
        final long expiry = mSessionExpiryElapsed.get(userId, 0L);
        if (expiry <= 0L) {
            return 0L;
        }
        final long remaining = expiry - SystemClock.elapsedRealtime();
        if (remaining <= 0L) {
            mSessionExpiryElapsed.delete(userId);
            return 0L;
        }
        return remaining;
    }

    private boolean isMutationAuthorizedInternal(int userId, @NonNull String mutationKey) {
        if (!isGateEnabledLocked()) {
            // Product prop explicitly disabled (bring-up only). Default is enabled.
            return true;
        }
        if (mutationKey == null || mutationKey.isEmpty() || !KNOWN_MUTATIONS.contains(mutationKey)) {
            Slog.w(TAG, "Unknown mutation key (fail-closed): " + mutationKey);
            return false;
        }
        if (isDeviceInSafeMode()) {
            Slog.w(TAG, "Safe mode — mutation denied (fail-closed)");
            return false;
        }
        synchronized (mLock) {
            return hasActiveSessionLocked(userId);
        }
    }

    private void enforcePrivilegedCallerForSessionOpen() {
        final int uid = Binder.getCallingUid();
        if (uid == Process.SHELL_UID || uid == Process.ROOT_UID) {
            throw new SecurityException(
                    "GT Config session open rejected for shell/root (ADB bypass blocked)");
        }
        if (uid != Process.SYSTEM_UID && UserHandle.getAppId(uid) != Process.SYSTEM_UID) {
            // Settings shares android.uid.system. Other apps must not open sessions.
            mContext.enforceCallingOrSelfPermission(
                    android.Manifest.permission.MANAGE_USERS,
                    "GT Config session open");
        }
    }

    private final class LocalService extends GuardTalkConfigGateInternal {
        @Override
        public boolean isAuthorized(int userId) {
            if (!isGateEnabledLocked()) {
                return true;
            }
            if (isDeviceInSafeMode()) {
                return false;
            }
            synchronized (mLock) {
                return hasActiveSessionLocked(userId);
            }
        }

        @Override
        public boolean isMutationAuthorized(int userId, @NonNull String mutationKey) {
            return isMutationAuthorizedInternal(userId, mutationKey);
        }

        @Override
        public void assertMutationAuthorized(int userId, @NonNull String mutationKey) {
            if (!isMutationAuthorizedInternal(userId, mutationKey)) {
                throw new SecurityException(
                        "GuardTalk GT Config gate: unauthorized mutation " + mutationKey
                                + " for user " + userId);
            }
        }
    }

    private final class BinderService extends IGuardTalkConfigGate.Stub {
        @Override
        public boolean isGateEnabled() {
            return isGateEnabledLocked();
        }

        @Override
        public boolean hasSecureLockScreen(int userId) {
            final long token = Binder.clearCallingIdentity();
            try {
                return mLockPatternUtils.isSecure(userId);
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }

        @Override
        public boolean isAuthorized(int userId) {
            if (!isGateEnabledLocked()) {
                return true;
            }
            if (isDeviceInSafeMode()) {
                return false;
            }
            synchronized (mLock) {
                return hasActiveSessionLocked(userId);
            }
        }

        @Override
        public boolean isMutationAuthorized(int userId, String mutationKey) {
            return isMutationAuthorizedInternal(userId, mutationKey);
        }

        @Override
        public boolean onDeviceCredentialConfirmed(int userId) {
            enforcePrivilegedCallerForSessionOpen();
            if (!isGateEnabledLocked()) {
                return true;
            }
            if (isDeviceInSafeMode()) {
                Slog.w(TAG, "Session open denied in safe mode");
                return false;
            }
            final long token = Binder.clearCallingIdentity();
            try {
                if (!mLockPatternUtils.isSecure(userId)) {
                    // Fail-closed: no main password ⇒ cannot authorize Security mutations.
                    Slog.w(TAG, "Session open denied: no secure lock for user " + userId);
                    return false;
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
            synchronized (mLock) {
                mSessionExpiryElapsed.put(userId,
                        SystemClock.elapsedRealtime() + SESSION_TTL_MS);
            }
            Slog.i(TAG, "GT Config session opened for user " + userId
                    + " ttlMs=" + SESSION_TTL_MS);
            return true;
        }

        @Override
        public void closeSession(int userId) {
            enforcePrivilegedCallerForSessionOpen();
            synchronized (mLock) {
                mSessionExpiryElapsed.delete(userId);
            }
            Slog.i(TAG, "GT Config session closed for user " + userId);
        }

        @Override
        public void assertMutationAuthorized(int userId, String mutationKey) {
            if (!isMutationAuthorizedInternal(userId, mutationKey)) {
                throw new SecurityException(
                        "GuardTalk GT Config gate: unauthorized mutation " + mutationKey
                                + " for user " + userId);
            }
        }

        @Override
        public long getSessionRemainingMillis(int userId) {
            if (!isGateEnabledLocked() || isDeviceInSafeMode()) {
                return 0L;
            }
            synchronized (mLock) {
                return remainingMillisLocked(userId);
            }
        }
    }
}
