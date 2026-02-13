package io.kestra.plugin.supabase;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@KestraTest
class SelectTest extends AbstractSupabaseTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void run() throws Exception {
        RunContext runContext = runContextFactory.of();

        Insert seed = Insert.builder()
            .url(Property.ofValue(getUrl()))
            .apiKey(Property.ofValue(getApiKey()))
            .table(Property.ofValue("kestra"))
            .data(java.util.List.of(java.util.Map.of("name", "demo-select-user")))
            .build();

        var seedOut = seed.run(runContext);
        assertThat(seedOut.getInsertedCount(), is(1));

        Select task = Select.builder()
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
}
