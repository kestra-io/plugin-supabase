package io.kestra.plugin.supabase;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@KestraTest
class DeleteTest extends AbstractSupabaseTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void run() throws Exception {
        RunContext runContext = runContextFactory.of();

        Insert insertTask = Insert.builder()
            .url(Property.ofValue(getUrl()))
            .apiKey(Property.ofValue(getApiKey()))
            .table(Property.ofValue("kestra"))
            .data(Property.ofValue(Map.of("name", "demo-user-delete")))
            .build();

        Insert.Output insertOutput = insertTask.run(runContext);
        assertThat(insertOutput, is(notNullValue()));
        assertThat(insertOutput.getInsertedCount(), is(1));

        Delete deleteTask = Delete.builder()
            .url(Property.ofValue(getUrl()))
            .apiKey(Property.ofValue(getApiKey()))
            .table(Property.ofValue("kestra"))
            .filter(Property.ofValue("name=eq.demo-user-delete"))
            .build();

        Delete.Output deleteOutput = deleteTask.run(runContext);
        assertThat(deleteOutput, is(notNullValue()));
        assertThat(deleteOutput.getDeletedCount(), is(1));
        assertThat(deleteOutput.getDeletedRows().toString(), containsString("demo-user-delete"));
    }
}
