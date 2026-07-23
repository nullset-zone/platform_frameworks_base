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

package com.android.systemui.qs.tiles

import com.android.systemui.qs.QsEventLogger
import com.android.systemui.qs.pipeline.shared.TileSpec
import com.android.systemui.qs.shared.model.TileCategory
import com.android.systemui.qs.tileimpl.QSTileImpl
import com.android.systemui.qs.tiles.base.shared.model.QSTileConfig
import com.android.systemui.qs.tiles.base.shared.model.QSTileUIConfig
import com.android.systemui.res.R
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey

/** Dagger bindings for GuardTalk Auto-reboot QS tile (T-SEC-P3-QS). */
@Module
interface AutoRebootModule {

    /** Inject AutoRebootTile into tileMap in QSModule / QSFactoryImpl. */
    @Binds
    @IntoMap
    @StringKey(AutoRebootTile.TILE_SPEC)
    fun bindAutoRebootTile(tile: AutoRebootTile): QSTileImpl<*>

    companion object {
        @Provides
        @IntoMap
        @StringKey(AutoRebootTile.TILE_SPEC)
        fun provideAutoRebootTileConfig(uiEventLogger: QsEventLogger): QSTileConfig =
            QSTileConfig(
                tileSpec = TileSpec.create(AutoRebootTile.TILE_SPEC),
                uiConfig =
                    QSTileUIConfig.Resource(
                        iconRes = R.drawable.qs_auto_reboot_icon_off,
                        labelRes = R.string.quick_settings_auto_reboot_label,
                    ),
                instanceId = uiEventLogger.getNewInstanceId(),
                category = TileCategory.PRIVACY,
            )
    }
}
