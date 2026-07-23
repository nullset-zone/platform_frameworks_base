package com.android.server.locksettings;

import android.content.Context;

/**
 * GrapheneOS duress wipe entry point.
 *
 * <p>GuardTalk (T-SEC-P2-WIPE): delegates to the shared {@link SecureWipeEngine}
 * so Duress, Secure wipe UI, and anti-bruteforce share one crypto-erase path
 * ({@code RecoverySystemService.deleteSecrets()} / FBE+KeyMint key destruction).
 */
public class DuressWipe {
    static final String TAG = SecureWipeEngine.TAG;

    /** @deprecated use {@link SecureWipeEngine#sleep5sBeforePoweroff} */
    public static boolean sleep5sBeforePoweroff;

    public static void run(Context context) {
        // Keep test hook in sync with the shared engine.
        SecureWipeEngine.sleep5sBeforePoweroff = sleep5sBeforePoweroff;
        SecureWipeEngine.run(context, SecureWipeEngine.Reason.DURESS);
    }
}
