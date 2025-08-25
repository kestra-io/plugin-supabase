package io.kestra.plugin.supabase;

import io.kestra.core.models.property.Property;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Interface defining common properties for Supabase tasks.
 */
public interface SupabaseInterface {
    
    @Schema(
        title = "The Supabase project URL",
        description = "The URL of your Supabase project (e.g., https://your-project.supabase.com)"
    )
    @NotNull
    Property<String> getUrl();

    @Schema(
        title = "The Supabase API key",
        description = "The API key for authenticating with Supabase -- use the anon key for client-side operations or the service_role key for server-side operations with elevated privileges."
    )
    @NotNull
    Property<String> getApiKey();

    @Schema(
        title = "The schema to use",
        description = "The database schema to use for operations -- defaults to 'public'."
    )
    Property<String> getSchema();
}
