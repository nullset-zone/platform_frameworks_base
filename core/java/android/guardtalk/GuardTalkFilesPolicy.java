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

import java.io.File;

/**
 * GuardTalk Files backend policy (T-SEC-P4-FILES).
 *
 * <p>When enabled:
 * <ul>
 *   <li>DocumentsUI trash flow is forced on for user storage (SDK Baklava+)</li>
 *   <li>Delete/trash of critical mounts (system/vendor/boot/recovery/…) is blocked</li>
 *   <li>ZIP compress remains available via stock DocumentsUI CompressJob</li>
 * </ul>
 *
 * <p>See {@code vendor/guardtalk/docs/FILES_HANDLERS_POLICY.md}.
 *
 * @hide
 */
public final class GuardTalkFilesPolicy {

    /** Master switch for GuardTalk Files protect + trash enablement. */
    public static final String PROP_FILES_POLICY = "ro.guardtalk.files_policy";

    /**
     * When true (default under {@link #PROP_FILES_POLICY}), DocumentsUI trash UI/API
     * is enabled even when the platform SDK equals Baklava (trash APIs are present).
     */
    public static final String PROP_FILES_TRASH = "ro.guardtalk.files_trash";

    /**
     * When true (default under {@link #PROP_FILES_POLICY}), delete/trash against
     * critical partition paths is rejected fail-closed.
     */
    public static final String PROP_FILES_PROTECT_CRITICAL =
            "ro.guardtalk.files_protect_critical";

    /**
     * Canonical critical mount prefixes. Paths are matched after {@link File#getCanonicalPath()}
     * when available; otherwise absolute path. Trailing-slash and exact-root forms accepted.
     */
    private static final String[] CRITICAL_PATH_PREFIXES = {
            "/system",
            "/system_ext",
            "/vendor",
            "/product",
            "/odm",
            "/oem",
            "/boot",
            "/recovery",
            "/vendor_boot",
            "/init_boot",
            "/firmware",
            "/persist",
            "/mnt/vendor",
            "/mnt/product",
            // GuardTalk privacy tmpfs is RAM-only; user file-manager must not trash it.
            "/mnt/guardtalk_privacy",
    };

    private GuardTalkFilesPolicy() {}

    /** Product Files policy is active. */
    public static boolean isEnabled() {
        return SystemProperties.getBoolean(PROP_FILES_POLICY, false);
    }

    /**
     * Whether DocumentsUI should expose trash/restore for user files.
     * Fail-open toward enabling when master policy is on and trash prop unset.
     */
    public static boolean isTrashEnabled() {
        if (!isEnabled()) {
            return false;
        }
        return SystemProperties.getBoolean(PROP_FILES_TRASH, true);
    }

    /**
     * Whether critical-partition delete/trash protection is active.
     * Fail-closed toward protecting when master policy is on and protect prop unset.
     */
    public static boolean isCriticalProtectEnabled() {
        if (!isEnabled()) {
            return false;
        }
        return SystemProperties.getBoolean(PROP_FILES_PROTECT_CRITICAL, true);
    }

    /**
     * Returns true if {@code path} resolves under a critical mount that must not be
     * deleted or trashed by the user file manager.
     *
     * @param path absolute or relative filesystem path; null/empty ⇒ not protected
     */
    public static boolean isProtectedPath(String path) {
        if (!isCriticalProtectEnabled()) {
            return false;
        }
        if (path == null || path.isEmpty()) {
            return false;
        }
        final String normalized = normalizePath(path);
        // Unresolvable / traversal-suspect paths are treated as protected (fail-closed).
        if (normalized == null) {
            return true;
        }
        for (String prefix : CRITICAL_PATH_PREFIXES) {
            if (normalized.equals(prefix) || normalized.startsWith(prefix + "/")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the file's resolved path is under a critical mount.
     *
     * @param file file to inspect; null ⇒ not protected
     */
    public static boolean isProtectedFile(File file) {
        if (file == null) {
            return false;
        }
        try {
            return isProtectedPath(file.getCanonicalPath());
        } catch (Exception e) {
            // Canonicalization failed — fail-closed when policy is on.
            return isCriticalProtectEnabled();
        }
    }

    /**
     * Normalizes a path for prefix matching. Returns {@code null} when the path
     * cannot be safely resolved (caller must fail-closed).
     */
    private static String normalizePath(String path) {
        String p = path.trim();
        if (p.isEmpty()) {
            return null;
        }
        if (p.contains("\0")) {
            return null;
        }
        if (p.contains("/../") || p.endsWith("/..") || p.equals("..") || p.startsWith("../")) {
            try {
                p = new File(p).getCanonicalPath();
            } catch (Exception e) {
                return null;
            }
        }
        if (p.length() > 1 && p.endsWith("/")) {
            p = p.substring(0, p.length() - 1);
        }
        return p;
    }

    /** Diagnostic label for logging (mount prefix only — no full user paths). */
    public static String describeProtection(String path) {
        if (!isCriticalProtectEnabled()) {
            return "policy-off";
        }
        final String normalized = normalizePath(path);
        if (normalized == null) {
            return "protected:unresolved";
        }
        for (String prefix : CRITICAL_PATH_PREFIXES) {
            if (normalized.equals(prefix) || normalized.startsWith(prefix + "/")) {
                return "protected:" + prefix;
            }
        }
        return "unprotected";
    }

    /** Test helper — copy of critical prefixes. */
    public static String[] getCriticalPathPrefixesForTest() {
        return CRITICAL_PATH_PREFIXES.clone();
    }
}
