package com.dangerfield.goodtimes.libraries.config.impl.data

import com.dangerfield.goodtimes.libraries.config.AppConfigMap
import com.dangerfield.goodtimes.libraries.core.Catching

interface RemoteConfigDataSource {
    suspend fun getConfig(): Catching<AppConfigMap>
}
