package com.dangerfield.goodtimes.libraries.core

import com.dangerfield.goodtimes.buildinfo.GoodtimesBuildConfig

object SupabaseInfo {
    val projectId: String
        get() = GoodtimesBuildConfig.SUPABASE_PROJECT_ID

    val url: String
        get() = GoodtimesBuildConfig.SUPABASE_URL

    val anonKey: String
        get() = GoodtimesBuildConfig.SUPABASE_ANON_KEY
}
