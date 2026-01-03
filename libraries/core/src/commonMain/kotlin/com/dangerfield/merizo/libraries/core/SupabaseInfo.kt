package com.dangerfield.merizo.libraries.core

import com.dangerfield.merizo.buildinfo.MerizoBuildConfig

object SupabaseInfo {
    val projectId: String
        get() = MerizoBuildConfig.SUPABASE_PROJECT_ID

    val url: String
        get() = MerizoBuildConfig.SUPABASE_URL

    val anonKey: String
        get() = MerizoBuildConfig.SUPABASE_ANON_KEY
}
