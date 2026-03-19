package com.kmptemplate.libraries.core

import com.kmptemplate.buildinfo.KMPTemplateBuildConfig

object SupabaseInfo {
    val projectId: String
        get() = KMPTemplateBuildConfig.SUPABASE_PROJECT_ID

    val url: String
        get() = KMPTemplateBuildConfig.SUPABASE_URL

    val anonKey: String
        get() = KMPTemplateBuildConfig.SUPABASE_ANON_KEY
}
