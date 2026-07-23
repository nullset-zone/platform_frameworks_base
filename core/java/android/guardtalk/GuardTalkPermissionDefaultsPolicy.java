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

import android.Manifest;
import android.os.SystemProperties;
import android.util.ArraySet;

import java.util.Collections;
import java.util.Set;

/**
 * GuardTalk default permission policy (T-SEC-P4-PERMS).
 *
 * <p>Product property enables Messenger default runtime grants and restricts
 * third-party {@code etc/default-permissions} exception grants. See
 * {@code vendor/guardtalk/docs/PERMISSION_DEFAULTS_POLICY.md}.
 *
 * <p><b>Package constant:</b> {@link #MESSENGER_PACKAGE_NAME} =
 * {@code com.guardtalk.messenger}. The Messenger APK is not yet in this tree;
 * plumbing is keyed by this constant and activates when the package is
 * installed (system or user).
 *
 * @hide
 */
public final class GuardTalkPermissionDefaultsPolicy {

    /**
     * GuardTalk Messenger application ID.
     *
     * <p>GAP (2026-07-23): No Messenger APK / Soong module / PRODUCT_PACKAGES
     * entry exists in-tree yet. Branding assets only
     * ({@code vendor/guardtalk/branding/.../guardtalk_messenger}). Do not invent
     * a fake APK — wire grants against this constant until the product APK lands.
     */
    public static final String MESSENGER_PACKAGE_NAME = "com.guardtalk.messenger";

    /** When true, Messenger grants + third-party exception deny policy are active. */
    public static final String PROP_PERMISSION_DEFAULTS = "ro.guardtalk.permission_defaults";

    private static final Set<String> MESSENGER_RUNTIME_PERMISSIONS;

    static {
        final ArraySet<String> perms = new ArraySet<>();
        // Messaging UX
        perms.add(Manifest.permission.POST_NOTIFICATIONS);
        // Voice / video calls and media capture
        perms.add(Manifest.permission.RECORD_AUDIO);
        perms.add(Manifest.permission.CAMERA);
        // Contact sync / picker (ContactsProvider remains installed)
        perms.add(Manifest.permission.READ_CONTACTS);
        perms.add(Manifest.permission.WRITE_CONTACTS);
        perms.add(Manifest.permission.GET_ACCOUNTS);
        // Attachments (scoped media; radio/BT excised product — no phone/SMS/BT grants)
        perms.add(Manifest.permission.READ_MEDIA_IMAGES);
        perms.add(Manifest.permission.READ_MEDIA_VIDEO);
        perms.add(Manifest.permission.READ_MEDIA_AUDIO);
        perms.add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED);
        perms.add(Manifest.permission.ACCESS_MEDIA_LOCATION);
        MESSENGER_RUNTIME_PERMISSIONS = Collections.unmodifiableSet(perms);
    }

    private GuardTalkPermissionDefaultsPolicy() {}

    /** Product policy active on GuardTalk builds. */
    public static boolean isEnabled() {
        return SystemProperties.getBoolean(PROP_PERMISSION_DEFAULTS, false);
    }

    /** True when {@code packageName} is the GuardTalk Messenger package. */
    public static boolean isMessengerPackage(String packageName) {
        return MESSENGER_PACKAGE_NAME.equals(packageName);
    }

    /**
     * Runtime permissions granted by default to Messenger when installed.
     *
     * <p>Only permissions the APK actually requests are applied by
     * {@code DefaultPermissionGrantPolicy}.
     */
    public static Set<String> getMessengerRuntimePermissions() {
        return MESSENGER_RUNTIME_PERMISSIONS;
    }

    /**
     * Whether an {@code etc/default-permissions} exception package may receive
     * automatic dangerous grants under GuardTalk policy.
     *
     * <p>Rules when {@link #isEnabled()}:
     * <ul>
     *   <li>Messenger — always allowed (product privilege)</li>
     *   <li>System image apps — allowed (preserve AOSP/GrapheneOS platform XML)</li>
     *   <li>All other third-party packages — denied (no exception auto-grant)</li>
     * </ul>
     *
     * @param packageName exception package name from XML
     * @param isSystemApp {@link android.content.pm.ApplicationInfo#isSystemApp()}
     */
    public static boolean allowDefaultPermissionException(
            String packageName, boolean isSystemApp) {
        if (!isEnabled()) {
            return true;
        }
        if (isMessengerPackage(packageName)) {
            return true;
        }
        return isSystemApp;
    }
}
