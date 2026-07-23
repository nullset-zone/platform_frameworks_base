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

package android.os;

/**
 * Binder API for the GuardTalk GT Config password gate (T-SEC-P1-GTGATE).
 *
 * @hide
 */
interface IGuardTalkConfigGate {
    /** True when the gate is active (fail-closed enforcement). */
    boolean isGateEnabled();

    /** True when the user has a secure device credential (password/PIN/pattern). */
    boolean hasSecureLockScreen(int userId);

    /** True when a verified maintenance/config session is currently open. */
    boolean isAuthorized(int userId);

    /**
     * True when {@code mutationKey} may proceed for {@code userId}.
     * Unknown keys fail closed (false).
     */
    boolean isMutationAuthorized(int userId, String mutationKey);

    /**
     * Opens a short-lived authorized session after Settings confirms the main
     * device credential. Privileged callers only; shell/root rejected.
     */
    boolean onDeviceCredentialConfirmed(int userId);

    /** Explicitly closes the authorized session for {@code userId}. */
    void closeSession(int userId);

    /**
     * Fail-closed assert for Security mutations. Throws SecurityException when
     * unauthorized.
     */
    void assertMutationAuthorized(int userId, String mutationKey);

    /** Remaining session TTL in millis, or 0 when unauthorized. */
    long getSessionRemainingMillis(int userId);
}
