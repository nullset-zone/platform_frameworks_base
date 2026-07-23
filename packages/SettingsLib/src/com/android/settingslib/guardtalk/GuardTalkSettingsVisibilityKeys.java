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

package com.android.settingslib.guardtalk;

/**
 * Shared contract for GuardTalk Main Settings keep/hide policy (T-SEC-P1-SETTINGS).
 *
 * <p>Preference keys match {@code top_level_settings*.xml}. Bool resource
 * <em>names</em> are overridden by {@code GuardTalkSettingsOverlay}. Frontend
 * task {@code F-SEC-P1-SETTINGS-UI} applies the keep/hide matrix; this class is
 * the stable API surface (no Settings R dependency).
 *
 * @see vendor/guardtalk/docs/SETTINGS_VISIBILITY_POLICY.md
 */
public final class GuardTalkSettingsVisibilityKeys {

    public static final String PREF_TOP_LEVEL_NETWORK = "top_level_network";
    public static final String PREF_TOP_LEVEL_CONNECTED_DEVICES = "top_level_connected_devices";
    public static final String PREF_TOP_LEVEL_APPS = "top_level_apps";
    public static final String PREF_TOP_LEVEL_NOTIFICATIONS = "top_level_notifications";
    public static final String PREF_TOP_LEVEL_SOUND = "top_level_sound";
    public static final String PREF_TOP_LEVEL_PRIORITY_MODES = "top_level_priority_modes";
    public static final String PREF_TOP_LEVEL_DISPLAY = "top_level_display";
    public static final String PREF_TOP_LEVEL_WALLPAPER = "top_level_wallpaper";
    public static final String PREF_TOP_LEVEL_STORAGE = "top_level_storage";
    public static final String PREF_TOP_LEVEL_BATTERY = "top_level_battery";
    public static final String PREF_TOP_LEVEL_SYSTEM = "top_level_system";
    public static final String PREF_TOP_LEVEL_ABOUT_DEVICE = "top_level_about_device";
    public static final String PREF_TOP_LEVEL_SAFETY_CENTER = "top_level_safety_center";
    public static final String PREF_TOP_LEVEL_SECURITY = "top_level_security";
    public static final String PREF_TOP_LEVEL_PRIVACY = "top_level_privacy";
    public static final String PREF_TOP_LEVEL_LOCATION = "top_level_location";
    public static final String PREF_TOP_LEVEL_ACCOUNTS = "top_level_accounts";
    public static final String PREF_TOP_LEVEL_EMERGENCY = "top_level_emergency";
    public static final String PREF_TOP_LEVEL_ACCESSIBILITY = "top_level_accessibility";

    /** Resource bool names (overlay targets in com.android.settings). */
    public static final String BOOL_SHOW_TOP_LEVEL_NETWORK = "config_show_top_level_network";
    public static final String BOOL_SHOW_TOP_LEVEL_CONNECTED_DEVICES =
            "config_show_top_level_connected_devices";
    public static final String BOOL_SHOW_TOP_LEVEL_APPS = "config_show_top_level_apps";
    public static final String BOOL_SHOW_TOP_LEVEL_NOTIFICATIONS =
            "config_show_top_level_notifications";
    public static final String BOOL_SHOW_TOP_LEVEL_SOUND = "config_show_top_level_sound";
    public static final String BOOL_SHOW_TOP_LEVEL_PRIORITY_MODES =
            "config_show_top_level_priority_modes";
    public static final String BOOL_SHOW_TOP_LEVEL_DISPLAY = "config_show_top_level_display";
    public static final String BOOL_SHOW_TOP_LEVEL_STORAGE = "config_show_top_level_storage";
    public static final String BOOL_SHOW_TOP_LEVEL_BATTERY = "config_show_top_level_battery";
    public static final String BOOL_SHOW_TOP_LEVEL_SYSTEM = "config_show_top_level_system";
    public static final String BOOL_SHOW_TOP_LEVEL_ABOUT_DEVICE =
            "config_show_top_level_about_device";
    public static final String BOOL_SHOW_TOP_LEVEL_SAFETY_CENTER =
            "config_show_top_level_safety_center";
    public static final String BOOL_SHOW_TOP_LEVEL_SECURITY = "config_show_top_level_security";
    public static final String BOOL_SHOW_TOP_LEVEL_PRIVACY = "config_show_top_level_privacy";
    public static final String BOOL_SHOW_TOP_LEVEL_LOCATION = "config_show_top_level_location";
    public static final String BOOL_SHOW_TOP_LEVEL_ACCOUNTS = "config_show_top_level_accounts";
    public static final String BOOL_SHOW_EMERGENCY_SETTINGS = "config_show_emergency_settings";
    public static final String BOOL_SHOW_TOP_LEVEL_ACCESSIBILITY =
            "config_show_top_level_accessibility";
    public static final String BOOL_USE_GUARDTALK_SECURITY_DASHBOARD =
            "config_use_guardtalk_security_dashboard";

    /** Fragment class for the GuardTalk Security skeleton dashboard. */
    public static final String GUARDTALK_SECURITY_DASHBOARD_FRAGMENT =
            "com.android.settings.security.guardtalk.GuardTalkSecurityDashboardFragment";

    private GuardTalkSettingsVisibilityKeys() {}
}
