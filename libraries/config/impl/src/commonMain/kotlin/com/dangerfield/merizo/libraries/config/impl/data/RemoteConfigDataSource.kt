package com.dangerfield.merizo.libraries.config.impl.data

import com.dangerfield.merizo.libraries.config.AppConfigMap
import com.dangerfield.merizo.libraries.core.Catching

interface RemoteConfigDataSource {
    suspend fun getConfig(): Catching<AppConfigMap>
}
