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
class UpdateTest extends AbstractSupabaseTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void run() throws Exception {
        RunContext runContext = runContextFactory.of();

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
}
