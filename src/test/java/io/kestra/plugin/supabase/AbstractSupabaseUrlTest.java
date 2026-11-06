package io.kestra.plugin.supabase;

import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContextFactory;

import jakarta.inject.Inject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@KestraTest
class AbstractSupabaseUrlTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void baseRequest_urlNormalization_handlesVariousFormats() throws Exception {
        var runContext = runContextFactory.of();
        var urls = List.of(
            "https://xyz.supabase.co",
            "https://xyz.supabase.co/",
            "https://xyz.supabase.co/rest/v1",
            "https://xyz.supabase.co/rest/v1/",
            "https://xyz.supabase.co/rest/v1///"
        );

        for (var inputUrl : urls) {
            var task = Select.builder()
                .url(Property.ofValue(inputUrl))
                .apiKey(Property.ofValue("dummy-key"))
                .table(Property.ofValue("kestra"))
                .build();

            var uri = task.baseRequest(runContext, task.buildTableEndpoint("kestra")).build().getUri();
            assertThat("Failed for URL: " + inputUrl, uri.toString(), is("https://xyz.supabase.co/rest/v1/kestra"));
        }
    }

    @Test
    void encodeQuery_preservesExistingPercentEncodingAndEncodesIllegalChars() {
        assertThat(AbstractSupabase.encodeQuery("name=eq.John Doe"), is("name=eq.John%20Doe"));
        assertThat(AbstractSupabase.encodeQuery("name=eq.John%20Doe"), is("name=eq.John%20Doe"));
        assertThat(AbstractSupabase.encodeQuery("email=eq.a%2Bb@x.com"), is("email=eq.a%2Bb@x.com"));
        assertThat(AbstractSupabase.encodeQuery("name=eq.A%26B"), is("name=eq.A%26B"));
        assertThat(AbstractSupabase.encodeQuery("filter=status=in.(active, pending)"), is("filter=status=in.(active,%20pending)"));
        assertThat(AbstractSupabase.encodeQuery("select=id, name"), is("select=id,%20name"));
        assertThat(AbstractSupabase.encodeQuery("desc=100% discount"), is("desc=100%25%20discount"));
        assertThat(AbstractSupabase.encodeQuery("special={\"key\": \"value\"}"), is("special=%7B%22key%22:%20%22value%22%7D"));
        assertThat(AbstractSupabase.encodeQuery("name=eq.C#1"), is("name=eq.C%231"));
        assertThat(AbstractSupabase.encodeQuery("name=eq.Rocket \uD83D\uDE80 Launch"), is("name=eq.Rocket%20%F0%9F%9A%80%20Launch"));
        assertThat(AbstractSupabase.encodeQuery("del=" + (char) 127), is("del=%7F"));
    }

    @Test
    void withQuery_constructsValidUri() {
        var task = Select.builder()
            .url(Property.ofValue("https://xyz.supabase.co"))
            .apiKey(Property.ofValue("dummy-key"))
            .table(Property.ofValue("kestra"))
            .build();

        var base = URI.create("https://xyz.supabase.co/rest/v1/kestra");
        var params = List.of("select=*", "name=eq.John%20Doe", "email=eq.a%2Bb@x.com");
        var resultUri = task.withQuery(base, params);

        assertThat(resultUri.toString(), is("https://xyz.supabase.co/rest/v1/kestra?select=*&name=eq.John%20Doe&email=eq.a%2Bb@x.com"));
        assertThat(resultUri.getRawQuery(), is("select=*&name=eq.John%20Doe&email=eq.a%2Bb@x.com"));
    }
}
