package io.kestra.plugin.supabase;

import io.kestra.core.models.property.Property;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Interface defining common properties for Supabase tasks.
 */
public interface SupabaseInterface {
    
    @Schema(
        title = "Supabase project URL",
        description = "Base project URL (e.g., `https://your-project.supabase.com`); the REST path /rest/v1 is appended automatically."
    )
    @NotNull
    Property<String> getUrl();

    @Schema(
        title = "Supabase API key",
        description = "API key sent in Authorization and apikey headers; use service_role for writes and elevated policies, anon key is limited."
    )
    @NotNull
    Property<String> getApiKey();

    @Schema(
        title = "Database schema",
        description = "Postgres schema for the request; defaults to `public` and adds Accept-Profile/Content-Profile headers when set."
    )
    Property<String> getSchema();
}
