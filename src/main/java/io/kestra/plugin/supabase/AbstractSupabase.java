package io.kestra.plugin.supabase;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpHeaders;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.http.HttpRequest;
import io.kestra.core.http.client.HttpClient;
import io.kestra.core.http.client.configurations.HttpConfiguration;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Abstract base class for Supabase tasks providing common functionality.
 */
@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public abstract class AbstractSupabase extends Task implements SupabaseInterface {

    @NotNull
    protected Property<String> url;

    @ToString.Exclude
    @NotNull
    protected Property<String> apiKey;

    @Builder.Default
    protected Property<String> schema = Property.ofValue("public");

    @Schema(title = "HTTP request options")
    @Builder.Default
    protected HttpConfiguration options = HttpConfiguration.builder().build();

    /**
     * Creates an HTTP client configured for Supabase API calls.
     */
    protected HttpClient client(RunContext runContext) throws IllegalVariableEvaluationException, MalformedURLException, URISyntaxException {
        return HttpClient.builder()
            .configuration(this.options)
            .runContext(runContext)
            .build();
    }

    /**
     * Creates a base HTTP request with common Supabase headers.
     */
    protected HttpRequest.HttpRequestBuilder baseRequest(RunContext runContext, String endpoint) throws IllegalVariableEvaluationException, URISyntaxException {
        var rUrl = runContext.render(this.url).as(String.class).orElseThrow(() -> new IllegalArgumentException("Missing required property 'url'"));
        var rApiKey = runContext.render(this.apiKey).as(String.class).orElseThrow(() -> new IllegalArgumentException("Missing required property 'apiKey'"));
        var rSchema = runContext.render(this.schema).as(String.class).orElse("public");

        var cleanUrl = rUrl.replaceAll("/+$", "");
        if (!cleanUrl.endsWith("/rest/v1")) {
            cleanUrl = cleanUrl + "/rest/v1";
        }

        var fullUrl = cleanUrl + (endpoint.startsWith("/") ? endpoint : "/" + endpoint);

        var headers = new HashMap<String, List<String>>();
        headers.put("apikey", List.of(rApiKey));
        headers.put("Authorization", List.of("Bearer " + rApiKey));
        headers.put("Content-Type", List.of("application/json"));
        headers.put("Accept", List.of("application/json"));

        // Add schema header if not using the default public schema
        if (!"public".equals(rSchema)) {
            headers.put("Accept-Profile", List.of(rSchema));
            headers.put("Content-Profile", List.of(rSchema));
        }

        return HttpRequest.builder()
            .uri(new URI(fullUrl))
            .headers(HttpHeaders.of(headers, (a, b) -> true));
    }

    /**
     * Builds the REST API endpoint for a table.
     */
    protected String buildTableEndpoint(String tableName) {
        return "/" + tableName;
    }

    /**
     * Builds the RPC endpoint for stored procedures.
     */
    protected String buildRpcEndpoint(String functionName) {
        return "/rpc/" + functionName;
    }

    /**
     * Constructs a request URI with encoded query parameters, preserving existing %HH escape sequences.
     */
    protected URI withQuery(URI baseUri, List<String> queryParams) {
        if (queryParams == null || queryParams.isEmpty()) {
            return baseUri;
        }

        var rawQuery = encodeQuery(String.join("&", queryParams));
        return URI.create(baseUri.toString() + "?" + rawQuery);
    }

    static String encodeQuery(String query) {
        if (query == null || query.isEmpty()) {
            return query;
        }

        var sb = new StringBuilder();
        var len = query.length();
        for (var i = 0; i < len; i++) {
            var c = query.charAt(i);
            if (c == ' ') {
                sb.append("%20");
            } else if (c == '"') {
                sb.append("%22");
            } else if (c == '<') {
                sb.append("%3C");
            } else if (c == '>') {
                sb.append("%3E");
            } else if (c == '{') {
                sb.append("%7B");
            } else if (c == '}') {
                sb.append("%7D");
            } else if (c == '|') {
                sb.append("%7C");
            } else if (c == '\\') {
                sb.append("%5C");
            } else if (c == '^') {
                sb.append("%5E");
            } else if (c == '`') {
                sb.append("%60");
            } else if (c == '%') {
                if (i + 2 < len && isHexDigit(query.charAt(i + 1)) && isHexDigit(query.charAt(i + 2))) {
                    sb.append('%');
                } else {
                    sb.append("%25");
                }
            } else if (c > 127 || c <= 31) {
                for (var b : String.valueOf(c).getBytes(java.nio.charset.StandardCharsets.UTF_8)) {
                    sb.append(String.format("%%%02X", b));
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static boolean isHexDigit(char c) {
        return (c >= '0' && c <= '9') ||
            (c >= 'a' && c <= 'f') ||
            (c >= 'A' && c <= 'F');
    }
}
