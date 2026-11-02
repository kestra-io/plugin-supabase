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
class DeleteTest extends AbstractSupabaseTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void run() throws Exception {
        var runContext = runContextFactory.of();

        var insertTask = Insert.builder()
            .url(Property.ofValue(getUrl()))
            .apiKey(Property.ofValue(getApiKey()))
            .table(Property.ofValue("kestra"))
            .data(Map.of("name", "demo-user-delete"))
            .build();

        var insertOutput = insertTask.run(runContext);
        assertThat(insertOutput, is(notNullValue()));
        assertThat(insertOutput.getInsertedCount(), is(1));

        var deleteTask = Delete.builder()
            .url(Property.ofValue(getUrl()))
            .apiKey(Property.ofValue(getApiKey()))
            .table(Property.ofValue("kestra"))
            .filter(Property.ofValue("name=eq.demo-user-delete"))
            .build();

        var deleteOutput = deleteTask.run(runContext);
        assertThat(deleteOutput, is(notNullValue()));
        assertThat(deleteOutput.getDeletedCount(), is(1));
        assertThat(deleteOutput.getDeletedRows().toString(), containsString("demo-user-delete"));
    }

    @Test
    void run_nonexistentTable_throwsHttpClientResponseException() {
        var runContext = runContextFactory.of();

        var task = Delete.builder()
            .url(Property.ofValue(getUrl()))
            .apiKey(Property.ofValue(getApiKey()))
            .table(Property.ofValue("table_does_not_exist_" + IdUtils.create()))
            .filter(Property.ofValue("id=eq.1"))
            .build();

        assertThrows(HttpClientResponseException.class, () -> task.run(runContext));
    }
}
