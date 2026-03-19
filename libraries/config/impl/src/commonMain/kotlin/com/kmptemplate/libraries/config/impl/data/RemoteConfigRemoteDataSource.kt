package com.kmptemplate.libraries.config.impl.data

import com.kmptemplate.libraries.core.logging.KLog
import com.kmptemplate.libraries.config.AppConfigMap
import com.kmptemplate.libraries.config.impl.model.BasicMapAppConfig
import com.kmptemplate.libraries.core.Catching
import com.kmptemplate.libraries.flowroutines.DispatcherProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class RemoteConfigRemoteDataSource @Inject constructor(
    private val dispatcherProvider: DispatcherProvider
) : RemoteConfigDataSource {

    private val logger = KLog.withTag("DummyConfigDataSource")

    override suspend fun getConfig(): Catching<AppConfigMap> = withContext(dispatcherProvider.io) {
        delay(1.seconds)
        Catching {
            val dynamicValue = Random.nextInt(0, 100)
            logger.d { "Returning dummy config value=$dynamicValue" }
            BasicMapAppConfig(
                mapOf(
                    "featureFlags" to mapOf(
                        "dummyFlag" to true
                    ),
                    "refreshToken" to dynamicValue
                )
            )
        }
    }
}
