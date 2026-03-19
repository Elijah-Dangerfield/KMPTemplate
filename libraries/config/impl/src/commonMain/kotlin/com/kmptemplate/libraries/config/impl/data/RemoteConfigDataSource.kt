package com.kmptemplate.libraries.config.impl.data

import com.kmptemplate.libraries.config.AppConfigMap
import com.kmptemplate.libraries.core.Catching

interface RemoteConfigDataSource {
    suspend fun getConfig(): Catching<AppConfigMap>
}
