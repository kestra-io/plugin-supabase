package io.kestra.plugin.supabase;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.kestra.core.http.client.HttpClientResponseException;
import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.utils.IdUtils;

import jakarta.inject.Inject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    void run_nonexistentTable_throwsHttpClientResponseException() {
        var runContext = runContextFactory.of();

        var task = Insert.builder()
            .url(Property.ofValue(getUrl()))
            .apiKey(Property.ofValue(getApiKey()))
            .table(Property.ofValue("table_does_not_exist_" + IdUtils.create()))
            .data(Map.of("name", "fail-user"))
            .build();

        assertThrows(HttpClientResponseException.class, () -> task.run(runContext));
    }
}
