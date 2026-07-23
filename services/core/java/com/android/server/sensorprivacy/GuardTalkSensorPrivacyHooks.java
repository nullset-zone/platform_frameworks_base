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

package com.android.server.sensorprivacy;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.guardtalk.GuardTalkSensorPrivacyPolicy;
import android.os.Binder;
import android.os.Handler;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.util.Slog;

import com.android.internal.widget.LockPatternUtils;

/**
 * Fail-closed GuardTalk hooks for {@link SensorPrivacyService} (T-SEC-P2-SENSOR).
 *
 * <p>Forces mic/camera AppOps restrictions when locked / pre-first-unlock, and
 * applies lockdown sensor + network fail-closed. Persisted QS/toggle state is
 * left unchanged so preference restores after unlock.
 *
 * <p>Does <em>not</em> register a Lockdown Quick Settings tile (Phase 3).
 */
final class GuardTalkSensorPrivacyHooks {
    private static final String TAG = "GtSensorPrivacy";

    interface RestrictionApplier {
        /** Re-evaluate mic/camera AppOps restrictions for the current user. */
        void applyEffectiveRestrictions();
    }

    private final Context mContext;
    private final Handler mHandler;
    private final RestrictionApplier mApplier;
    private final LockPatternUtils mLockPatternUtils;
    private final UserManager mUserManager;
    private final LockPatternUtils.StrongAuthTracker mStrongAuthTracker;

    private KeyguardManager mKeyguardManager;
    private volatile int mCurrentUserId = UserHandle.USER_SYSTEM;
    private volatile int mStrongAuthFlags;
    private boolean mRegistered;

    /** Prior airplane mode when lockdown engaged (restore on exit). */
    private boolean mNetworkLockdownActive;
    private int mPriorAirplaneMode = -1;

    GuardTalkSensorPrivacyHooks(
            Context context, Handler handler, RestrictionApplier applier) {
        mContext = context;
        mHandler = handler;
        mApplier = applier;
        mLockPatternUtils = new LockPatternUtils(context);
        mUserManager = context.getSystemService(UserManager.class);
        mStrongAuthTracker = new LockPatternUtils.StrongAuthTracker(context, handler.getLooper()) {
            @Override
            public void onStrongAuthRequiredChanged(int userId) {
                if (userId != mCurrentUserId && userId != UserHandle.USER_ALL) {
                    return;
                }
                final int uid = userId == UserHandle.USER_ALL ? mCurrentUserId : userId;
                mStrongAuthFlags = getStrongAuthForUser(uid);
                mHandler.post(() -> onStrongAuthChanged(uid));
            }
        };
    }

    /** Register lock / unlock / strong-auth listeners. Idempotent. */
    void register(KeyguardManager keyguardManager) {
        if (mRegistered) {
            return;
        }
        if (!GuardTalkSensorPrivacyPolicy.isSensorPrivacyWhenLockedEnabled()
                && !GuardTalkSensorPrivacyPolicy.isLockdownFailClosedEnabled()) {
            return;
        }
        mKeyguardManager = keyguardManager;
        mRegistered = true;
        try {
            mLockPatternUtils.registerStrongAuthTracker(mStrongAuthTracker);
            mStrongAuthFlags = mStrongAuthTracker.getStrongAuthForUser(mCurrentUserId);
        } catch (Exception e) {
            Slog.e(TAG, "Failed to register StrongAuthTracker", e);
        }
        if (mKeyguardManager != null) {
            try {
                mKeyguardManager.addKeyguardLockedStateListener(
                        mHandler::post,
                        isKeyguardLocked -> mHandler.post(mApplier::applyEffectiveRestrictions));
            } catch (Exception e) {
                Slog.w(TAG, "KeyguardLockedStateListener unavailable; using broadcasts", e);
            }
        }
        final IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_USER_UNLOCKED);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mHandler.post(mApplier::applyEffectiveRestrictions);
            }
        }, filter, null, mHandler, Context.RECEIVER_NOT_EXPORTED);
        Slog.i(TAG, "Registered GuardTalk sensor privacy / lockdown hooks");
        mHandler.post(mApplier::applyEffectiveRestrictions);
    }

    void setCurrentUser(int userId) {
        mCurrentUserId = userId;
        try {
            mStrongAuthFlags = mStrongAuthTracker.getStrongAuthForUser(userId);
        } catch (Exception ignored) {
            // keep prior flags
        }
    }

    /** True when mic/camera must be AppOps-restricted for {@code userId}. */
    boolean mustDenySensors(int userId) {
        final boolean deviceLocked = isDeviceLocked(userId);
        final boolean userUnlocked = isUserUnlocked(userId);
        int flags = mStrongAuthFlags;
        if (userId != mCurrentUserId) {
            try {
                flags = mStrongAuthTracker.getStrongAuthForUser(userId);
            } catch (Exception e) {
                flags = 0;
            }
        }
        return GuardTalkSensorPrivacyPolicy.mustDenySensors(deviceLocked, userUnlocked, flags);
    }

    /**
     * Combine user toggle state with GuardTalk force-deny.
     *
     * @param toggleEnabled true when software/hardware toggle already restricts the sensor
     */
    boolean effectiveRestriction(int userId, boolean toggleEnabled) {
        return toggleEnabled || mustDenySensors(userId);
    }

    /**
     * Reject attempts to disable sensor privacy (sensors ON) while force-deny applies.
     *
     * @param enablePrivacy true = privacy ON (sensors muted)
     * @return true if the change is allowed
     */
    boolean allowToggleChange(int userId, boolean enablePrivacy) {
        if (enablePrivacy) {
            return true;
        }
        if (mustDenySensors(userId)) {
            Slog.i(TAG, "Reject sensor privacy disable while locked/pre-unlock/lockdown"
                    + " (fail-closed)");
            return false;
        }
        return true;
    }

    private void onStrongAuthChanged(int userId) {
        final boolean lockdown = GuardTalkSensorPrivacyPolicy.isLockdownActive(mStrongAuthFlags);
        if (GuardTalkSensorPrivacyPolicy.isLockdownFailClosedEnabled()) {
            applyNetworkLockdown(lockdown);
        }
        mApplier.applyEffectiveRestrictions();
        if (lockdown) {
            Slog.i(TAG, "Lockdown active — sensors/network fail-closed (user=" + userId + ")");
        }
    }

    /**
     * Fail-closed network path for lockdown: force airplane mode on enter;
     * restore prior airplane setting on exit. No QS tile registration.
     */
    private void applyNetworkLockdown(boolean lockdown) {
        if (lockdown == mNetworkLockdownActive) {
            return;
        }
        final ContentResolver cr = mContext.getContentResolver();
        final long token = Binder.clearCallingIdentity();
        try {
            if (lockdown) {
                mPriorAirplaneMode = Settings.Global.getInt(
                        cr, Settings.Global.AIRPLANE_MODE_ON, 0);
                if (mPriorAirplaneMode == 0) {
                    Settings.Global.putInt(cr, Settings.Global.AIRPLANE_MODE_ON, 1);
                    final Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
                    intent.putExtra("state", true);
                    mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
                }
                mNetworkLockdownActive = true;
                Slog.i(TAG, "Lockdown network fail-closed (airplane on; prior="
                        + mPriorAirplaneMode + ")");
            } else {
                if (mPriorAirplaneMode == 0) {
                    Settings.Global.putInt(cr, Settings.Global.AIRPLANE_MODE_ON, 0);
                    final Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
                    intent.putExtra("state", false);
                    mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
                }
                mNetworkLockdownActive = false;
                mPriorAirplaneMode = -1;
                Slog.i(TAG, "Lockdown network restored");
            }
        } catch (Exception e) {
            // Fail-closed: leave airplane on if we cannot restore cleanly.
            Slog.e(TAG, "Network lockdown apply failed — leaving fail-closed state", e);
            mNetworkLockdownActive = lockdown;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private boolean isDeviceLocked(int userId) {
        if (mKeyguardManager == null) {
            mKeyguardManager = mContext.getSystemService(KeyguardManager.class);
        }
        if (mKeyguardManager == null) {
            // Fail-closed when policy on and keyguard unavailable.
            return GuardTalkSensorPrivacyPolicy.isSensorPrivacyWhenLockedEnabled();
        }
        try {
            return mKeyguardManager.isDeviceLocked(userId);
        } catch (Exception e) {
            return true;
        }
    }

    private boolean isUserUnlocked(int userId) {
        if (mUserManager == null) {
            return false;
        }
        try {
            return mUserManager.isUserUnlocked(userId);
        } catch (Exception e) {
            return false;
        }
    }
}
