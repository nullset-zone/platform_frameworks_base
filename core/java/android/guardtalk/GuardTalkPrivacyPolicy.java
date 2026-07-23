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
 * GuardTalk privacy tmpfs + clipboard clear policy (T-SEC-P4-PRIVACY).
 *
 * <p>When enabled:
 * <ul>
 *   <li>{@code privacy_tmpfs} — sensitive ephemeral logs/cache live under a
 *       RAM-backed mount wiped across reboot</li>
 *   <li>{@code clipboard_clear} — system clipboard is cleared on lock and on a
 *       short timeout; fail-closed toward clearing</li>
 * </ul>
 *
 * <p>See {@code vendor/guardtalk/docs/PRIVACY_TMPFS_CLIPBOARD_POLICY.md}.
 *
 * @hide
 */
public final class GuardTalkPrivacyPolicy {

    /** When true, init mounts the GuardTalk privacy tmpfs. */
    public static final String PROP_PRIVACY_TMPFS = "ro.guardtalk.privacy_tmpfs";

    /** Absolute mount path for RAM-only privacy material (logs/cache). */
    public static final String PROP_PRIVACY_TMPFS_PATH = "ro.guardtalk.privacy_tmpfs_path";

    /** Default mount point — must match init.guardtalk.privacy_tmpfs.rc. */
    public static final String DEFAULT_PRIVACY_TMPFS_PATH = "/mnt/guardtalk_privacy";

    /**
     * When true, {@code ClipboardService} clears clipboard on lock / timeout
     * and forces auto-clear even if DeviceConfig would disable it.
     */
    public static final String PROP_CLIPBOARD_CLEAR = "ro.guardtalk.clipboard_clear";

    /** Auto-clear timeout in milliseconds when {@link #PROP_CLIPBOARD_CLEAR} is on. */
    public static final String PROP_CLIPBOARD_CLEAR_TIMEOUT_MS =
            "ro.guardtalk.clipboard_clear_timeout_ms";

    /** Default clipboard auto-clear timeout (60s) — fail-closed toward short retention. */
    public static final long DEFAULT_CLIPBOARD_CLEAR_TIMEOUT_MS = 60_000L;

    private GuardTalkPrivacyPolicy() {}

    /** Product privacy tmpfs mount is enabled. */
    public static boolean isPrivacyTmpfsEnabled() {
        return SystemProperties.getBoolean(PROP_PRIVACY_TMPFS, false);
    }

    /**
     * Returns the privacy tmpfs path. Uses the product prop when set, else the
     * default mount point.
     */
    public static String getPrivacyTmpfsPath() {
        final String path = SystemProperties.get(PROP_PRIVACY_TMPFS_PATH, "");
        if (path == null || path.isEmpty()) {
            return DEFAULT_PRIVACY_TMPFS_PATH;
        }
        return path;
    }

    /** Product clipboard clear policy is enabled. */
    public static boolean isClipboardClearEnabled() {
        return SystemProperties.getBoolean(PROP_CLIPBOARD_CLEAR, false);
    }

    /**
     * Timeout for clipboard auto-clear when policy is on. Values {@code <= 0}
     * fall back to {@link #DEFAULT_CLIPBOARD_CLEAR_TIMEOUT_MS} (fail-closed).
     */
    public static long getClipboardClearTimeoutMs() {
        final long timeout = SystemProperties.getLong(
                PROP_CLIPBOARD_CLEAR_TIMEOUT_MS, DEFAULT_CLIPBOARD_CLEAR_TIMEOUT_MS);
        if (timeout <= 0L) {
            return DEFAULT_CLIPBOARD_CLEAR_TIMEOUT_MS;
        }
        return timeout;
    }

    /**
     * Whether clipboard content must be cleared for a lock transition.
     * Fail-closed: if policy is on and lock-state is unknown, clear.
     *
     * @param deviceLocked {@code true} when keyguard reports locked; {@code null}
     *                     when the lock state could not be determined
     */
    public static boolean mustClearClipboardOnLock(Boolean deviceLocked) {
        if (!isClipboardClearEnabled()) {
            return false;
        }
        if (deviceLocked == null) {
            return true; // fail-closed toward clearing
        }
        return deviceLocked;
    }
}
