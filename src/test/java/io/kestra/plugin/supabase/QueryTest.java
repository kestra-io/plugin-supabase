package io.kestra.plugin.supabase;

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
class QueryTest extends AbstractSupabaseTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void run() throws Exception {
        var runContext = runContextFactory.of();

        var seed = Insert.builder()
            .url(Property.ofValue(getUrl()))
            .apiKey(Property.ofValue(getApiKey()))
            .table(Property.ofValue("kestra"))
            .data(java.util.Map.of("name", "demo-query-user"))
            .build();

        var seedOut = seed.run(runContext);
        assertThat(seedOut.getInsertedCount(), is(1));

        var task = Query.builder()
            .url(Property.ofValue(getUrl()))
            .apiKey(Property.ofValue(getApiKey()))
            .functionName(Property.ofValue("get_users"))
            .build();

        var output = task.run(runContext);

        assertThat(output, is(notNullValue()));
        assertThat(output.getRows().size(), greaterThanOrEqualTo(1));
        assertThat(output.getRows().toString(), containsString("demo-query-user"));
    }

    @Test
    void run_nonexistentFunction_throwsHttpClientResponseException() {
        var runContext = runContextFactory.of();

        var task = Query.builder()
            .url(Property.ofValue(getUrl()))
            .apiKey(Property.ofValue(getApiKey()))
            .functionName(Property.ofValue("nonexistent_func_" + IdUtils.create()))
            .build();

        assertThrows(HttpClientResponseException.class, () -> task.run(runContext));
    }
}
