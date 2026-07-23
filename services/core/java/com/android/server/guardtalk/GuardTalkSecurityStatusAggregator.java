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
import android.content.Context;
import android.guardtalk.GuardTalkSecurityStatus;
import android.guardtalk.GuardTalkSecurityStatus.Snapshot;

/**
 * System-server entry for GuardTalk Security status aggregation (T-SEC-P5-STATUS).
 *
 * <p>Delegates to {@link GuardTalkSecurityStatus} — no parallel policy stack.
 * Callers that expose status to apps/UI must enforce post-unlock and must
 * never read or publish Duress armed state.
 *
 * @hide
 */
public final class GuardTalkSecurityStatusAggregator {

    private GuardTalkSecurityStatusAggregator() {}

    /** Post-unlock gate (fail-closed). */
    public static boolean isPostUnlockStatusAllowed(@NonNull Context context) {
        return GuardTalkSecurityStatus.isPostUnlockStatusAllowed(context);
    }

    /**
     * Prop-only markers for diagnostics. Does not include runtime
     * sensor/USB deny or Duress.
     */
    @NonNull
    public static Snapshot collectPolicyMarkers() {
        return GuardTalkSecurityStatus.collectPolicyMarkers();
    }

    /**
     * Full aggregate with runtime deny flags. When {@code requirePostUnlock}
     * is true, returns {@link Snapshot#denied()} before unlock.
     */
    @NonNull
    public static Snapshot collect(
            @NonNull Context context,
            boolean requirePostUnlock,
            boolean sensorsForceDenied,
            boolean usbDataForceDenied,
            boolean lockdownActive,
            int autoRebootHours) {
        return GuardTalkSecurityStatus.collect(
                context,
                requirePostUnlock,
                sensorsForceDenied,
                usbDataForceDenied,
                lockdownActive,
                autoRebootHours);
    }
}