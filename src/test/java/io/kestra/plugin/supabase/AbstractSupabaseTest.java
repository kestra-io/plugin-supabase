package io.kestra.plugin.supabase;

import io.kestra.core.junit.annotations.KestraTest;
import io.micronaut.context.annotation.Value;

@KestraTest
public abstract class AbstractSupabaseTest {
    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.api-key}")
    private String supabaseApiKey;

    protected String getUrl() {
        return supabaseUrl;
    }

    protected String getApiKey() {
        return supabaseApiKey;
    }
}
