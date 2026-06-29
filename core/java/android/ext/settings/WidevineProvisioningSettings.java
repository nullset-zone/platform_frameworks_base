package android.ext.settings;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.content.Context;
import android.provider.Settings;

import static android.annotation.SystemApi.Client.MODULE_LIBRARIES;

/** @hide */
@SystemApi(client = MODULE_LIBRARIES)
public class WidevineProvisioningSettings {
    /** @hide */
    public static final int WV_GRAPHENEOS_PROXY = 0;
    /** @hide */
    public static final int WV_STANDARD_SERVER = 1;

    private static final String WV_GRAPHENEOS_PROXY_HOSTNAME = "widevineprovisioning.guardtalk.io";

    /** @hide */
    public static final IntSetting SERVER_SETTING = new IntSetting(
            Setting.Scope.GLOBAL, Settings.Global.WIDEVINE_PROVISIONING_SERVER,
            WV_GRAPHENEOS_PROXY, // default
            WV_STANDARD_SERVER, WV_GRAPHENEOS_PROXY // valid values
    );

    @Nullable
    public static String getServerHostnameOverride(@NonNull Context ctx) {
        // T-WIDEVINE-LOCK: always return the GuardTalkOS proxy hostname,
        // ignoring the Settings.Global.widevine_provisioner_server value. The
        // setting is retained for backward compatibility (existing readers and
        // the Settings UI controller do not need to be repointed), but it is
        // now a no-op: even if a user managed to flip it to WV_STANDARD_SERVER
        // via `adb shell settings put global`, this method would still return
        // the proxy hostname. The pref controller row is additionally hidden
        // via WidevineProvisioningPrefController.getAvailabilityStatus()
        // returning UNSUPPORTED_ON_DEVICE. Reversible: restore the original
        // conditional (return proxy only when setting ==
        // WV_GRAPHENEOS_PROXY, else null) to revert (Law 11).
        return WV_GRAPHENEOS_PROXY_HOSTNAME;
    }

    private WidevineProvisioningSettings() {}
}
