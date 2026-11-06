package io.kestra.plugin.supabase;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContextFactory;

import jakarta.inject.Inject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@KestraTest
class InsertTest extends AbstractSupabaseTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void run() throws Exception {
        var runContext = runContextFactory.of();

        var task = Insert.builder()
            .url(Property.ofValue(getUrl()))
            .apiKey(Property.ofValue(getApiKey()))
            .table(Property.ofValue("kestra"))
            .data(Map.of("name", "demo-user"))
            .build();

        var output = task.run(runContext);

        assertThat(output, is(notNullValue()));
        assertThat(output.getInsertedCount(), is(1));
        assertThat(output.getInsertedRows().toString(), containsString("demo-user"));
    }

    @Test
    void run_urlWithTrailingSlash_succeeds() throws Exception {
        var runContext = runContextFactory.of();

        var task = Insert.builder()
            .url(Property.ofValue(getUrl() + "/"))
            .apiKey(Property.ofValue(getApiKey()))
            .table(Property.ofValue("kestra"))
            .data(Map.of("name", "demo-user-trailing-slash"))
            .build();

        var output = task.run(runContext);

        assertThat(output, is(notNullValue()));
        assertThat(output.getInsertedCount(), is(1));
        assertThat(output.getInsertedRows().toString(), containsString("demo-user-trailing-slash"));
    }

    @Test
    void run_selectWithSpaces_succeeds() throws Exception {
        var runContext = runContextFactory.of();

        var task = Insert.builder()
            .url(Property.ofValue(getUrl()))
            .apiKey(Property.ofValue(getApiKey()))
            .table(Property.ofValue("kestra"))
            .select(Property.ofValue("id, name"))
            .data(Map.of("name", "demo-user-select-spaces"))
            .build();

        var output = task.run(runContext);

        assertThat(output, is(notNullValue()));
        assertThat(output.getInsertedCount(), is(1));
        assertThat(output.getInsertedRows().toString(), containsString("demo-user-select-spaces"));
    }
}
