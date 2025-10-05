package com.heyu.zhudeapp.client

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

/**
 * A singleton object to initialize and manage the Supabase client instance.
 * This ensures that there's only one instance of the client throughout the app.
 */
object SupabaseClient {

    val supabase = createSupabaseClient(
        // IMPORTANT: Replace with your actual Supabase URL and Anon Key.
        supabaseUrl = "YOUR_SUPABASE_URL",
        supabaseKey = "YOUR_SUPABASE_ANON_KEY"
    ) {
        // Install the necessary plugins.
        // Auth is required for user management.
        install(Auth)
        // Postgrest is required for interacting with your database tables.
        install(Postgrest)
        // Storage is required for file uploads and downloads.
        install(Storage)
    }
}
