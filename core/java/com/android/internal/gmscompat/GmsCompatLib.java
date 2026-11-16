package com.android.internal.gmscompat;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import com.android.internal.util.Preconditions;

public class GmsCompatLib {
    private static final String TAG = "GmsCompatLib";
    private static IGmsCompatLib instance;

    public static IGmsCompatLib get() {
        return instance;
    }

    public static void init(Context appContext, String processName) {
        Preconditions.checkState(instance == null);

        Context libCtx;
        try {
            int flags = Context.CONTEXT_INCLUDE_CODE
                    // GmsCompatLib is part of system image
                    | Context.CONTEXT_IGNORE_SECURITY;
            libCtx = appContext.createPackageContext("app.grapheneos.gmscompat.lib", flags);
        } catch (PackageManager.NameNotFoundException e) {
            // T-CAM-GMSCOMPAT-TOLERATE-MISSING (GuardTalk, 2026-11-09).
            //
            // GuardTalkOS excises the GmsCompat cluster (T-REMEDIATE-B2-EXCISE,
            // DEC-REMEDIATE-001 item 10) because it is a no-GMS product. This
            // method used to rethrow, which killed the calling app's process at
            // Instrumentation.newApplication() — before any of the app's own
            // code ran. Any preinstalled app that merely advertises
            // com.google.android.gms.version metadata is routed here, including
            // PixelCameraServicesConnectivityClient (PCS). PCS died on every
            // start, and because PCS exports
            //   com.google.android.apps.camera.services.extensions.service.PixelExtensions*
            // (the ICameraExtensionsProxyService the GrapheneOS camera's camerax
            // ExtensionVersionImpl binds to), camera extensions could never
            // initialise: ExtensionsManager NPE'd on a null proxy binder,
            // PXLEXT_PixelExtensionsClient retried once per second ON THE MAIN
            // THREAD, input dispatch starved past 5 s, and Camera2CameraImpl
            // failed configureStreams with a 5000 ms TimeoutException.
            //
            // Every consumer of get() already null-checks, so degrading to
            // "no compatibility shims" is safe:
            //   BinderProxy.java:519/748/780   if (gmcLib != null)
            //   ContextImpl.java:2362          if (gmcLib != null)
            //   GmsCompat.java                 get() == null
            // The one unguarded call site is GmsHooks:706 (Constellation /
            // VerifyPhoneNumber); it is already enclosed in a
            // try/catch (Exception) that logs "failed" and continues, so a null
            // here degrades that optional RCS-notification path rather than
            // crashing. No edit is needed there.
            //
            // This does NOT install GMS and does not weaken the no-GMS policy:
            // nothing is added to the image, and the shims simply never load.
            // Precedent: GosPackageStatePermissions already log-and-returns for
            // an absent GmsCompatApp.PKG_NAME after the 2026-06-30 boot loop.
            Log.w(TAG, "GmsCompatLib is absent (excised by GuardTalk); "
                    + "continuing without compatibility shims in process '"
                    + processName + "'", e);
            return;
        }

        IGmsCompatLib lib;
        try {
            Class cls = libCtx.getClassLoader().loadClass("app.grapheneos.gmscompat.lib.GmsCompatLibImpl");
            lib = (IGmsCompatLib) cls.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            Log.w(TAG, "GmsCompatLib present but not loadable; continuing without shims", e);
            return;
        }
        instance = lib;
        lib.init(appContext, libCtx, processName);
    }
}
