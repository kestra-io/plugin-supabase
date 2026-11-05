package io.kestra.plugin.supabase;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContextFactory;

import io.micronaut.context.annotation.Value;
import jakarta.inject.Inject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

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

    @Test
    void baseRequest_urlNormalization_handlesVariousFormats() throws Exception {
        var runContext = runContextFactory.of();
        var urls = List.of(
            "https://xyz.supabase.co",
            "https://xyz.supabase.co/",
            "https://xyz.supabase.co/rest/v1",
            "https://xyz.supabase.co/rest/v1/",
            "https://xyz.supabase.co/rest/v1///"
        );

        for (var inputUrl : urls) {
            var task = Select.builder()
                .url(Property.ofValue(inputUrl))
                .apiKey(Property.ofValue("dummy-key"))
                .table(Property.ofValue("kestra"))
                .build();

            var uri = task.baseRequest(runContext, task.buildTableEndpoint("kestra")).build().getUri();
            assertThat("Failed for URL: " + inputUrl, uri.toString(), is("https://xyz.supabase.co/rest/v1/kestra"));
        }
    }
}
