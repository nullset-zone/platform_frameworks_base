package android.ext.settings;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.content.Context;
import android.provider.Settings;

import static android.annotation.SystemApi.Client.MODULE_LIBRARIES;

/** @hide */
@SystemApi(client = MODULE_LIBRARIES)
public class RemoteKeyProvisioningSettings {

    public static final int GRAPHENEOS_PROXY = 0;
    public static final int STANDARD_SERVER = 1;

    private static final String GRAPHENEOS_PROXY_URL = "https://remoteprovisioning.guardtalk.io/v1";

    /** @hide */
    public static final IntSetting SERVER_SETTING = new IntSetting(
            Setting.Scope.GLOBAL, Settings.Global.REMOTE_KEY_PROVISIONING_SERVER,
            GRAPHENEOS_PROXY, // default
            STANDARD_SERVER, GRAPHENEOS_PROXY // valid values
    );

    @Nullable
    public static String getServerUrlOverride(@NonNull Context ctx) {
        // T-ATTEST-LOCK: always return the GuardTalkOS proxy URL, ignoring the
        // Settings.Global.attest_remote_provisioner_server value. The setting
        // is retained for backward compatibility (so existing readers and the
        // Settings UI controller do not need to be repointed), but it is now
        // a no-op: even if a user managed to flip it to STANDARD_SERVER via
        // `adb shell settings put global`, this method would still return the
        // proxy URL. The pref controller row is additionally hidden via
        // RemoteProvisioningPrefController.getAvailabilityStatus() returning
        // UNSUPPORTED_ON_DEVICE. Reversible: restore the original conditional
        // (return proxy only when setting == GRAPHENEOS_PROXY, else null) to
        // revert (Law 11).
        return GRAPHENEOS_PROXY_URL;
    }

    private RemoteKeyProvisioningSettings() {}
}
