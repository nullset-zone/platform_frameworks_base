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
 * GuardTalk secure wipe + duress + anti-bruteforce policy (T-SEC-P2-WIPE).
 *
 * <p>Server enforcement uses product props. Settings UI mirrors via overlay
 * bools — see {@code vendor/guardtalk/docs/SECURE_WIPE_SERVICE.md} and
 * {@code vendor/guardtalk/docs/DURESS_AND_ANTI_BRUTEFORCE_POLICY.md}.
 *
 * @hide
 */
public final class GuardTalkSecureWipePolicy {

    /** When true, shared secure wipe engine + anti-bruteforce wipe are active. */
    public static final String PROP_SECURE_WIPE_ENABLED = "ro.guardtalk.secure_wipe_enabled";

    /**
     * Failed unlock attempts (HW-backed counter) before automatic crypto-erase.
     * Default 10. Values &lt; 1 disable anti-bruteforce wipe.
     */
    public static final String PROP_ANTI_BRUTEFORCE_WIPE_THRESHOLD =
            "ro.guardtalk.anti_bruteforce_wipe_threshold";

    /** Canonical wipe threshold when prop unset. */
    public static final int DEFAULT_WIPE_THRESHOLD = 10;

    private GuardTalkSecureWipePolicy() {}

    /** Shared wipe engine + duress/anti-bruteforce hooks enabled. */
    public static boolean isSecureWipeEnabled() {
        return SystemProperties.getBoolean(PROP_SECURE_WIPE_ENABLED, false);
    }

    /**
     * Failed HW-reaching unlock attempts before wipe. Returns {@code 0} when
     * secure wipe is disabled or threshold is non-positive.
     */
    public static int getAntiBruteforceWipeThreshold() {
        if (!isSecureWipeEnabled()) {
            return 0;
        }
        final int configured =
                SystemProperties.getInt(
                        PROP_ANTI_BRUTEFORCE_WIPE_THRESHOLD, DEFAULT_WIPE_THRESHOLD);
        if (configured <= 0) {
            return 0;
        }
        return configured;
    }

    /** True when anti-bruteforce should wipe after N HW-reaching failures. */
    public static boolean isAntiBruteforceWipeEnabled() {
        return getAntiBruteforceWipeThreshold() > 0;
    }
}
