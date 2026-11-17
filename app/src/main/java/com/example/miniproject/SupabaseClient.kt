package com.example.miniproject

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.PropertyConversionMethod

object SupabaseClient {
    val instance: SupabaseClient = createSupabaseClient(
        supabaseUrl = "https://zjgxmokfevzsjcfgyowk.supabase.co",
        supabaseKey = "sb_publishable_jGX42-TiobZfhdT9ydmWkA_YmWxK94N"
    ) {
        install(Auth)
        install(Postgrest) {
            propertyConversionMethod = PropertyConversionMethod.CAMEL_CASE_TO_SNAKE_CASE
        }
    }
}
