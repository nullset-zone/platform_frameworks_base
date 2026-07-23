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

import android.guardtalk.GuardTalkSecureWipePolicy;
import android.util.Slog;

import java.time.Duration;

/**
 * Hardware-backed failed-unlock counter for GuardTalk anti-bruteforce wipe
 * (T-SEC-P2-WIPE).
 *
 * <p><b>Authoritative store:</b> reserved Weaver slot (RPMB / secure element)
 * via {@link SyntheticPasswordManager}. Survives reboot. Not a normal userdata
 * file — ADB {@code settings}/{@code rm} cannot reset it; Recovery/Safe Mode do
 * not expose Weaver writes to untrusted callers.
 *
 * <p><b>Fallback (no Weaver):</b> GateKeeper/Weaver LSKF timeout estimation
 * against the software rate-limiter schedule. When the HW timeout alone implies
 * {@code >=} wipe threshold failures, wipe. Never treats the SPM
 * {@code failure_counter} userdata file as authoritative.
 *
 * <p>See {@code vendor/guardtalk/docs/DURESS_AND_ANTI_BRUTEFORCE_POLICY.md}.
 */
final class HwBackedFailedAttemptCounter {
    private static final String TAG = "GtHwFailCounter";

    /**
     * Mirrors {@link SoftwareRateLimiter} TIMEOUT_TABLE indices used to estimate
     * failures from a GateKeeper/Weaver timeout when Weaver counter is unavailable.
     */
    private static final Duration[] TIMEOUT_TO_FAILURES =
            new Duration[] {
                Duration.ZERO,
                Duration.ZERO,
                Duration.ZERO,
                Duration.ZERO,
                Duration.ZERO,
                Duration.ofMinutes(1),
                Duration.ofMinutes(5),
                Duration.ofMinutes(15),
                Duration.ofMinutes(30),
                Duration.ofMinutes(90),
                Duration.ofHours(4),
                Duration.ofHours(12),
                Duration.ofHours(36),
                Duration.ofDays(4),
                Duration.ofDays(13),
                Duration.ofDays(41),
                Duration.ofDays(123),
                Duration.ofDays(365),
                Duration.ofDays(365L * 3),
                Duration.ofDays(365L * 9),
            };

    private final SyntheticPasswordManager mSpManager;

    HwBackedFailedAttemptCounter(SyntheticPasswordManager spManager) {
        mSpManager = spManager;
    }

    /**
     * Records one unique HW-reaching failed unlock and returns the new count
     * used for wipe decisions (Weaver count when available, else timeout
     * estimate).
     *
     * @param hwTimeout timeout returned by GateKeeper/Weaver for this failure
     * @return failure count for wipe comparison; {@code 0} if policy disabled
     */
    int recordFailureAndGetCount(Duration hwTimeout) {
        if (!GuardTalkSecureWipePolicy.isAntiBruteforceWipeEnabled()) {
            return 0;
        }
        final int threshold = GuardTalkSecureWipePolicy.getAntiBruteforceWipeThreshold();

        Integer weaverCount = null;
        synchronized (mSpManager) {
            weaverCount = mSpManager.guardTalkIncrementAntiBruteforceCounter();
        }
        if (weaverCount != null) {
            Slog.i(TAG, "Weaver anti-bruteforce count=" + weaverCount);
            return weaverCount;
        }

        final int estimated = estimateFailuresFromHwTimeout(hwTimeout);
        Slog.i(TAG, "Weaver unavailable; HW-timeout estimate failures=" + estimated
                + " timeout=" + hwTimeout + " threshold=" + threshold);
        return estimated;
    }

    /** Clears the Weaver counter after a successful unlock. */
    void resetOnSuccess() {
        if (!GuardTalkSecureWipePolicy.isAntiBruteforceWipeEnabled()) {
            return;
        }
        synchronized (mSpManager) {
            mSpManager.guardTalkResetAntiBruteforceCounter();
        }
    }

    /**
     * Estimates failure count from a hardware rate-limiter timeout by walking
     * the known progressive schedule (highest matching index wins).
     */
    static int estimateFailuresFromHwTimeout(Duration hwTimeout) {
        if (hwTimeout == null || hwTimeout.isNegative()) {
            return 0;
        }
        if (hwTimeout.isZero()) {
            // Failures 1–4 typically return zero timeout; cannot distinguish.
            // Conservative: treat as at least 1 unique HW failure this attempt.
            return 1;
        }
        int best = 0;
        for (int i = 0; i < TIMEOUT_TO_FAILURES.length; i++) {
            if (hwTimeout.compareTo(TIMEOUT_TO_FAILURES[i]) >= 0) {
                best = i;
            }
        }
        return best;
    }
}
