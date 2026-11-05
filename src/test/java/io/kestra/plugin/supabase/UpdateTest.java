package io.kestra.plugin.supabase;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;

import jakarta.inject.Inject;

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
            .data(List.of(Map.of("name", "update-demo-user")))
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
    void run_filterWithSpaces_succeeds() throws Exception {
        var runContext = runContextFactory.of();

        var seed = Insert.builder()
            .url(Property.ofValue(getUrl()))
            .apiKey(Property.ofValue(getApiKey()))
            .table(Property.ofValue("kestra"))
            .data(List.of(Map.of("name", "update space user")))
            .build()
            .run(runContext);

        assertThat(seed.getInsertedCount(), is(1));

        var out = Update.builder()
            .url(Property.ofValue(getUrl()))
            .apiKey(Property.ofValue(getApiKey()))
            .table(Property.ofValue("kestra"))
            .data(Property.ofValue(Map.of("name", "updated space user")))
            .filter(Property.ofValue("name=eq.update space user"))
            .build()
            .run(runContext);

        assertThat(out, is(notNullValue()));
        assertThat(out.getUpdatedCount(), is(1));
        assertThat(out.getUpdatedRows(), is(notNullValue()));
        assertThat(out.getUpdatedRows().toString(), containsString("updated space user"));
    }

    @Test
    void run_urlWithTrailingSlash_succeeds() throws Exception {
        var runContext = runContextFactory.of();

        var seed = Insert.builder()
            .url(Property.ofValue(getUrl()))
            .apiKey(Property.ofValue(getApiKey()))
            .table(Property.ofValue("kestra"))
            .data(List.of(Map.of("name", "update-trailing-slash-user")))
            .build()
            .run(runContext);

        assertThat(seed.getInsertedCount(), is(1));
        var id = seed.getInsertedRows().getFirst().get("id");

        var out = Update.builder()
            .url(Property.ofValue(getUrl() + "/"))
            .apiKey(Property.ofValue(getApiKey()))
            .table(Property.ofValue("kestra"))
            .data(Property.ofValue(Map.of("name", "updated-trailing-slash-user")))
            .filter(Property.ofValue("id=eq." + id))
            .build()
            .run(runContext);

        assertThat(out, is(notNullValue()));
        assertThat(out.getUpdatedCount(), is(1));
        assertThat(out.getUpdatedRows(), is(notNullValue()));
        assertThat(out.getUpdatedRows().toString(), containsString("updated-trailing-slash-user"));
    }

    @Test
    void run_filterAlreadyEncoded_succeeds() throws Exception {
        var runContext = runContextFactory.of();

        var seed = Insert.builder()
            .url(Property.ofValue(getUrl()))
            .apiKey(Property.ofValue(getApiKey()))
            .table(Property.ofValue("kestra"))
            .data(List.of(Map.of("name", "update encoded user")))
            .build()
            .run(runContext);

        assertThat(seed.getInsertedCount(), is(1));

        var out = Update.builder()
            .url(Property.ofValue(getUrl()))
            .apiKey(Property.ofValue(getApiKey()))
            .table(Property.ofValue("kestra"))
            .data(Property.ofValue(Map.of("name", "updated encoded user")))
            .filter(Property.ofValue("name=eq.update%20encoded%20user"))
            .build()
            .run(runContext);

        assertThat(out, is(notNullValue()));
        assertThat(out.getUpdatedCount(), is(1));
        assertThat(out.getUpdatedRows(), is(notNullValue()));
        assertThat(out.getUpdatedRows().toString(), containsString("updated encoded user"));
    }
}
