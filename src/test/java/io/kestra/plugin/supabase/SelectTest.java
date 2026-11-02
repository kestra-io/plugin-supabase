package io.kestra.plugin.supabase;

import org.junit.jupiter.api.Test;

import io.kestra.core.http.client.HttpClientResponseException;
import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.utils.IdUtils;

import jakarta.inject.Inject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@KestraTest
class SelectTest extends AbstractSupabaseTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void run() throws Exception {
        var runContext = runContextFactory.of();

        var seed = Insert.builder()
            .url(Property.ofValue(getUrl()))
            .apiKey(Property.ofValue(getApiKey()))
            .table(Property.ofValue("kestra"))
            .data(java.util.List.of(java.util.Map.of("name", "demo-select-user")))
            .build();

        var seedOut = seed.run(runContext);
        assertThat(seedOut.getInsertedCount(), is(1));

        var task = Select.builder()
            .url(Property.ofValue(getUrl()))
            .apiKey(Property.ofValue(getApiKey()))
            .table(Property.ofValue("kestra"))
            .filter(Property.ofValue("name=eq.demo-select-user"))
            .order(Property.ofValue("id.desc"))
            .limit(Property.ofValue(1))
            .build();

        var output = task.run(runContext);

        assertThat(output, is(notNullValue()));
        assertThat(output.getRows(), is(notNullValue()));
        assertThat(output.getSize(), greaterThanOrEqualTo(1));
        assertThat(output.getRows().toString(), containsString("demo-select-user"));
    }

    @Test
    void run_nonexistentTable_throwsHttpClientResponseException() {
        var runContext = runContextFactory.of();

        var task = Select.builder()
            .url(Property.ofValue(getUrl()))
            .apiKey(Property.ofValue(getApiKey()))
            .table(Property.ofValue("table_does_not_exist_" + IdUtils.create()))
            .build();

        assertThrows(HttpClientResponseException.class, () -> task.run(runContext));
    }
}
