package com.example.miniproject

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.PropertyConversionMethod

object SupabaseClient {
    private const val PROJECT_URL = "https://zjgxmokfevzsjcfgyowk.supabase.co"
    private const val ANON_KEY = "sb_publishable_jGX42-TiobZfhdT9ydmWkA_YmWxK94N"

    val instance: SupabaseClient = createSupabaseClient(
        supabaseUrl = PROJECT_URL,
        supabaseKey = ANON_KEY
    ) {
        install(Auth)
        install(Postgrest) {
            propertyConversionMethod = PropertyConversionMethod.CAMEL_CASE_TO_SNAKE_CASE
        }
    }
}
