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
class UpdateTest extends AbstractSupabaseTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void run() throws Exception {
        var runContext = runContextFactory.of();

        var seed = Insert.builder()
            .url(Property.ofValue(getUrl()))
            .apiKey(Property.ofValue(getApiKey()))
            .table(Property.ofValue("kestra"))
            .data(java.util.List.of(java.util.Map.of("name", "update-demo-user")))
            .build()
            .run(runContext);

        assertThat(seed.getInsertedCount(), is(1));
        assertThat(seed.getInsertedRows().toString(), containsString("update-demo-user"));

        var id = seed.getInsertedRows().getFirst().get("id");

        var out = Update.builder()
            .url(Property.ofValue(getUrl()))
            .apiKey(Property.ofValue(getApiKey()))
            .table(Property.ofValue("kestra"))
            .data(Property.ofValue(Map.of("name", "updated-demo-user")))
            .filter(Property.ofValue("id=eq." + id))
            .build()
            .run(runContext);

        assertThat(out, is(notNullValue()));
        assertThat(out.getUpdatedCount(), is(1));
        assertThat(out.getUpdatedRows(), is(notNullValue()));
        assertThat(out.getUpdatedRows().toString(), containsString("updated-demo-user"));
    }

    @Test
    void run_nonexistentTable_throwsHttpClientResponseException() {
        var runContext = runContextFactory.of();

        var task = Update.builder()
            .url(Property.ofValue(getUrl()))
            .apiKey(Property.ofValue(getApiKey()))
            .table(Property.ofValue("table_does_not_exist_" + IdUtils.create()))
            .data(Property.ofValue(Map.of("name", "fail-update")))
            .filter(Property.ofValue("id=eq.1"))
            .build();

        assertThrows(HttpClientResponseException.class, () -> task.run(runContext));
    }
}
