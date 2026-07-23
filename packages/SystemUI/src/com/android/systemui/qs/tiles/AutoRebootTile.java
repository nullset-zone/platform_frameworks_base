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

package com.android.systemui.qs.tiles;

import android.content.Intent;
import android.content.SharedPreferences;
import android.ext.settings.ExtSettings;
import android.guardtalk.GuardTalkAutoRebootPolicy;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.widget.Switch;

import androidx.annotation.Nullable;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.systemui.Prefs;
import com.android.systemui.animation.Expandable;
import com.android.systemui.dagger.qualifiers.Background;
import com.android.systemui.dagger.qualifiers.Main;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.FalsingManager;
import com.android.systemui.plugins.qs.QSTile.BooleanState;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.QsEventLogger;
import com.android.systemui.qs.logging.QSLogger;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.res.R;
import com.android.systemui.util.settings.GlobalSettings;
import com.android.systemui.util.settings.SettingObserver;

import javax.inject.Inject;

/**
 * GuardTalk Auto-reboot Quick Settings tile (T-SEC-P3-QS).
 *
 * <p>Short-press toggles Off ↔ last non-Off profile (default 8h) via
 * {@link ExtSettings#AUTO_REBOOT_TIMEOUT}. Long-press opens Settings
 * Exploit protection (Auto reboot picker). See
 * {@code vendor/guardtalk/docs/QS_TILES_POLICY.md}.
 */
public class AutoRebootTile extends QSTileImpl<BooleanState> {

    public static final String TILE_SPEC = "autoreboot";

    /** Persists last non-Off profile ms across Off toggles (SystemUI Prefs). */
    private static final String PREF_LAST_ENABLED_TIMEOUT_MS =
            "guardtalk_qs_auto_reboot_last_timeout_ms";

    private static final String SETTINGS_PACKAGE = "com.android.settings";
    private static final String EXPLOIT_PROTECTION_ACTIVITY =
            "com.android.settings.Settings$ExploitProtectionActivity";
    private static final String EXPLOIT_PROTECTION_FRAGMENT =
            "com.android.settings.safetycenter.ExploitProtectionFragment";
    /** Mirrors SettingsActivity.EXTRA_SHOW_FRAGMENT. */
    private static final String EXTRA_SHOW_FRAGMENT = ":settings:show_fragment";

    private final SettingObserver mSetting;

    @Inject
    public AutoRebootTile(
            QSHost host,
            QsEventLogger uiEventLogger,
            @Background Looper backgroundLooper,
            @Main Handler mainHandler,
            FalsingManager falsingManager,
            MetricsLogger metricsLogger,
            StatusBarStateController statusBarStateController,
            ActivityStarter activityStarter,
            QSLogger qsLogger,
            GlobalSettings globalSettings) {
        super(host, uiEventLogger, backgroundLooper, mainHandler, falsingManager, metricsLogger,
                statusBarStateController, activityStarter, qsLogger);
        mSetting = new SettingObserver(
                globalSettings,
                mHandler,
                Settings.Global.AUTO_REBOOT_TIMEOUT,
                GuardTalkAutoRebootPolicy.DEFAULT_PROFILE_MS) {
            @Override
            protected void handleValueChanged(int value, boolean observedChange) {
                handleRefreshState(value);
            }
        };
    }

    @Override
    public boolean isAvailable() {
        return GuardTalkAutoRebootPolicy.isProfilesEnabled();
    }

    @Override
    public BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    protected void handleDestroy() {
        super.handleDestroy();
        mSetting.setListening(false);
    }

    @Override
    public void handleSetListening(boolean listening) {
        super.handleSetListening(listening);
        mSetting.setListening(listening);
    }

    @Override
    protected void handleClick(@Nullable Expandable expandable) {
        final int current = getEffectiveTimeoutMs();
        if (current > 0) {
            saveLastEnabledTimeout(current);
            ExtSettings.AUTO_REBOOT_TIMEOUT.put(mContext, GuardTalkAutoRebootPolicy.PROFILE_OFF_MS);
        } else {
            ExtSettings.AUTO_REBOOT_TIMEOUT.put(mContext, getRestoreTimeoutMs());
        }
        refreshState(null);
    }

    @Override
    public Intent getLongClickIntent() {
        final Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClassName(SETTINGS_PACKAGE, EXPLOIT_PROTECTION_ACTIVITY);
        intent.putExtra(EXTRA_SHOW_FRAGMENT, EXPLOIT_PROTECTION_FRAGMENT);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.quick_settings_auto_reboot_label);
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        final int timeoutMs = arg instanceof Integer
                ? GuardTalkAutoRebootPolicy.getEffectiveTimeoutMillis((Integer) arg)
                : getEffectiveTimeoutMs();
        final boolean enabled = timeoutMs > 0;
        state.value = enabled;
        state.state = enabled ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE;
        state.label = mContext.getString(R.string.quick_settings_auto_reboot_label);
        state.contentDescription = state.label;
        state.icon = maybeLoadResourceIcon(enabled
                ? R.drawable.qs_auto_reboot_icon_on
                : R.drawable.qs_auto_reboot_icon_off);
        state.expandedAccessibilityClassName = Switch.class.getName();
        if (enabled) {
            final long hours = timeoutMs / (60L * 60L * 1000L);
            if (hours > 0 && hours * 60L * 60L * 1000L == timeoutMs) {
                state.secondaryLabel = mContext.getResources().getQuantityString(
                        R.plurals.quick_settings_auto_reboot_secondary_hours,
                        (int) hours, (int) hours);
            } else {
                state.secondaryLabel =
                        mContext.getString(R.string.quick_settings_auto_reboot_secondary_on);
            }
        } else {
            state.secondaryLabel =
                    mContext.getString(R.string.quick_settings_auto_reboot_secondary_off);
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.VIEW_UNKNOWN;
    }

    private int getEffectiveTimeoutMs() {
        return GuardTalkAutoRebootPolicy.getEffectiveTimeoutMillis(
                ExtSettings.AUTO_REBOOT_TIMEOUT.get(mContext));
    }

    private int getRestoreTimeoutMs() {
        final SharedPreferences prefs = Prefs.get(mContext);
        final int last = prefs.getInt(
                PREF_LAST_ENABLED_TIMEOUT_MS, GuardTalkAutoRebootPolicy.DEFAULT_PROFILE_MS);
        if (last <= 0) {
            return GuardTalkAutoRebootPolicy.DEFAULT_PROFILE_MS;
        }
        return GuardTalkAutoRebootPolicy.clampToProfileMillis(last);
    }

    private void saveLastEnabledTimeout(int timeoutMs) {
        final int clamped = GuardTalkAutoRebootPolicy.clampToProfileMillis(timeoutMs);
        if (clamped <= 0) {
            return;
        }
        Prefs.get(mContext).edit().putInt(PREF_LAST_ENABLED_TIMEOUT_MS, clamped).apply();
    }
}
