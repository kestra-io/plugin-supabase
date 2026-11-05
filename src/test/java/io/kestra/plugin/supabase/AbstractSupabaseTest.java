package io.kestra.plugin.supabase;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.runners.RunContextFactory;

import io.micronaut.context.annotation.Value;
import jakarta.inject.Inject;

@KestraTest
public abstract class AbstractSupabaseTest {
    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.api-key}")
    private String supabaseApiKey;

    @Inject
    protected RunContextFactory runContextFactory;

    protected String getUrl() {
        return supabaseUrl;
    }

    protected String getApiKey() {
        return supabaseApiKey;
    }
}
