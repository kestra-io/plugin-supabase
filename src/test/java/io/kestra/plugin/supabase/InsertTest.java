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
class InsertTest extends AbstractSupabaseTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void run() throws Exception {
        RunContext runContext = runContextFactory.of();

        Insert task = Insert.builder()
            .url(Property.ofValue(getUrl()))
            .apiKey(Property.ofValue(getApiKey()))
            .table(Property.ofValue("kestra"))
            .data(Map.of("name", "demo-user"))
            .build();

        Insert.Output output = task.run(runContext);

        assertThat(output, is(notNullValue()));
        assertThat(output.getInsertedCount(), is(1));
        assertThat(output.getInsertedRows().toString(), containsString("demo-user"));
    }
}
