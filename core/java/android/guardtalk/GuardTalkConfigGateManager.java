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

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import android.os.IGuardTalkConfigGate;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.util.Log;

/**
 * Client helper for the GuardTalk GT Config password gate.
 *
 * <p>Service name: {@link #SERVICE_NAME}. Fail-closed: if the binder is missing
 * or throws, authorization checks return false / throw.
 *
 * <p>See {@code vendor/guardtalk/docs/GT_CONFIG_PASSWORD_GATE_API.md}.
 *
 * @hide
 */
public final class GuardTalkConfigGateManager {

    private static final String TAG = "GtConfigGateMgr";

    /** ServiceManager name published by GuardTalkConfigGateService. */
    public static final String SERVICE_NAME = "guardtalk_config_gate";

    /** Product property reinforcing gate enablement (tokay GuardTalk layer). */
    public static final String PROP_GATE_ENABLED = "ro.guardtalk.config_password_gate";

    private GuardTalkConfigGateManager() {}

    @Nullable
    public static IGuardTalkConfigGate getService() {
        final android.os.IBinder binder = ServiceManager.getService(SERVICE_NAME);
        if (binder == null) {
            return null;
        }
        return IGuardTalkConfigGate.Stub.asInterface(binder);
    }

    /** Fail-closed: missing service ⇒ not authorized. */
    public static boolean isAuthorized(@NonNull Context context) {
        return isAuthorized(context.getUserId());
    }

    public static boolean isAuthorized(int userId) {
        final IGuardTalkConfigGate gate = getService();
        if (gate == null) {
            Log.w(TAG, "isAuthorized: service missing (fail-closed)");
            return false;
        }
        try {
            return gate.isAuthorized(userId);
        } catch (RemoteException e) {
            Log.e(TAG, "isAuthorized failed (fail-closed)", e);
            return false;
        }
    }

    public static boolean isMutationAuthorized(int userId, @NonNull String mutationKey) {
        final IGuardTalkConfigGate gate = getService();
        if (gate == null) {
            Log.w(TAG, "isMutationAuthorized: service missing (fail-closed)");
            return false;
        }
        try {
            return gate.isMutationAuthorized(userId, mutationKey);
        } catch (RemoteException e) {
            Log.e(TAG, "isMutationAuthorized failed (fail-closed)", e);
            return false;
        }
    }

    /**
     * Fail-closed assert for Security / GT Config mutations.
     *
     * @throws SecurityException when unauthorized or service unavailable
     */
    public static void assertMutationAuthorized(int userId, @NonNull String mutationKey) {
        final IGuardTalkConfigGate gate = getService();
        if (gate == null) {
            throw new SecurityException(
                    "GuardTalk config gate unavailable (fail-closed): " + mutationKey);
        }
        try {
            gate.assertMutationAuthorized(userId, mutationKey);
        } catch (RemoteException e) {
            throw new SecurityException(
                    "GuardTalk config gate remote failure (fail-closed): " + mutationKey, e);
        }
    }

    public static boolean onDeviceCredentialConfirmed(@NonNull Context context) {
        return onDeviceCredentialConfirmed(context.getUserId());
    }

    public static boolean onDeviceCredentialConfirmed(int userId) {
        final IGuardTalkConfigGate gate = getService();
        if (gate == null) {
            Log.e(TAG, "onDeviceCredentialConfirmed: service missing");
            return false;
        }
        try {
            return gate.onDeviceCredentialConfirmed(userId);
        } catch (RemoteException e) {
            Log.e(TAG, "onDeviceCredentialConfirmed failed", e);
            return false;
        }
    }

    public static void closeSession(int userId) {
        final IGuardTalkConfigGate gate = getService();
        if (gate == null) {
            return;
        }
        try {
            gate.closeSession(userId);
        } catch (RemoteException e) {
            Log.e(TAG, "closeSession failed", e);
        }
    }

    public static long getSessionRemainingMillis(int userId) {
        final IGuardTalkConfigGate gate = getService();
        if (gate == null) {
            return 0L;
        }
        try {
            return gate.getSessionRemainingMillis(userId);
        } catch (RemoteException e) {
            Log.e(TAG, "getSessionRemainingMillis failed", e);
            return 0L;
        }
    }

    public static boolean hasSecureLockScreen(int userId) {
        final IGuardTalkConfigGate gate = getService();
        if (gate == null) {
            return false;
        }
        try {
            return gate.hasSecureLockScreen(userId);
        } catch (RemoteException e) {
            Log.e(TAG, "hasSecureLockScreen failed", e);
            return false;
        }
    }

    /** Convenience for the calling user. */
    public static void assertMutationAuthorizedForCallingUser(@NonNull String mutationKey) {
        assertMutationAuthorized(UserHandle.myUserId(), mutationKey);
    }
}
