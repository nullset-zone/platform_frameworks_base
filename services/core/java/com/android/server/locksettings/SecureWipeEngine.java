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

import android.annotation.UptimeMillisLong;
import android.content.Context;
import android.os.SystemClock;
import android.telephony.euicc.EuiccCardManager;
import android.util.Slog;

import com.android.internal.telephony.euicc.EuiccWipe;
import com.android.server.power.PowerManagerService;
import com.android.server.recoverysystem.RecoverySystemService;

/**
 * GuardTalk shared secure wipe engine (T-SEC-P2-WIPE).
 *
 * <p>Single crypto-erase path used by Secure wipe UI, Duress Password, and
 * anti-bruteforce wipe@10. Delegates to GrapheneOS
 * {@link RecoverySystemService#deleteSecrets()} (KeyMint/FBE key destruction via
 * {@code AndroidKeyStoreMaintenance.deleteAllKeys()} + vold extended wipe).
 * Recoverable by reflash — not flash-overwrite-as-wipe, not a hardware brick.
 *
 * <p>See {@code vendor/guardtalk/docs/SECURE_WIPE_SERVICE.md}.
 */
public final class SecureWipeEngine {
    static final String TAG = "SecureWipeEngine";

    /** Why the shared wipe engine was invoked. */
    public enum Reason {
        /** Settings / API secure-wipe after owner password + confirm. */
        USER_REQUESTED,
        /** Duress credential matched. */
        DURESS,
        /** HW-backed failed-unlock counter reached wipe threshold. */
        ANTI_BRUTEFORCE,
    }

    /** Used only for testing; guarded by owner credential in Duress tests. */
    public static boolean sleep5sBeforePoweroff;

    private SecureWipeEngine() {}

    /**
     * Runs the shared crypto-erase then power-off. Never returns on success.
     *
     * @param context application context
     * @param reason invocation source (logged; not persisted)
     */
    public static void run(Context context, Reason reason) {
        Slog.w(TAG, "start reason=" + reason);

        EuiccWipeThread euiccWipeThread = EuiccWipeThread.start(context);

        Slog.d(TAG, "calling deleteSecrets");
        // deleteSecrets() → AndroidKeyStoreMaintenance.deleteAllKeys() destroys
        // KeyMint keys including storage encryption keys, then ExtendedWipeWithoutReboot.
        RecoverySystemService.deleteSecrets();
        Slog.d(TAG, "deleteSecrets returned");

        euiccWipeThread.await(3000);
        Slog.d(TAG, "finished waiting for euiccWipeThread");

        if (sleep5sBeforePoweroff) {
            SystemClock.sleep(5000);
        }

        PowerManagerService.lowLevelShutdown(null);
    }

    static class EuiccWipeThread extends Thread {
        private final Context context;
        @UptimeMillisLong private long startTime;

        private EuiccWipeThread(Context context) {
            this.context = context;
        }

        static EuiccWipeThread start(Context ctx) {
            var t = new EuiccWipeThread(ctx);
            t.start();
            t.startTime = SystemClock.uptimeMillis();
            return t;
        }

        @Override
        public void run() {
            try {
                EuiccWipe.run(context, EuiccCardManager.RESET_FLAG_IS_FOR_DURESS_WIPE);
            } catch (Throwable e) {
                Slog.e(TAG, "", e);
            }
        }

        void await(long timeoutMs) {
            long remaining = timeoutMs - (SystemClock.uptimeMillis() - startTime);
            if (remaining <= 0) {
                return;
            }
            try {
                join(remaining);
            } catch (InterruptedException e) {
                Slog.e(TAG, "", e);
            }
        }
    }
}
