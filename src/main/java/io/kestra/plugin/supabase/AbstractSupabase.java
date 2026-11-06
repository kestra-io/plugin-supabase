package io.kestra.plugin.supabase;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpHeaders;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;

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
    // Characters java.net.URI rejects in a query component, plus '#' which would start a fragment
    private static final String UNSAFE_QUERY_CHARS = " \"#<>\\^`{|}";
    private static final HexFormat HEX = HexFormat.of().withUpperCase();

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

    /**
     * Percent-encodes characters that are not legal in a URI query, while preserving valid %HH sequences
     * so users can pre-encode reserved characters (e.g. {@code a%2Bb} to send a literal '+').
     * {@link java.net.URLEncoder} or the multi-argument {@link URI} constructors cannot be used because they always re-encode '%'.
     */
    static String encodeQuery(String query) {
        if (query == null || query.isEmpty()) {
            return query;
        }

        var bytes = query.getBytes(StandardCharsets.UTF_8);
        var sb = new StringBuilder(bytes.length);
        for (var i = 0; i < bytes.length; i++) {
            var b = bytes[i] & 0xFF;
            var keepEscape = b == '%' && i + 2 < bytes.length
                && HexFormat.isHexDigit(bytes[i + 1]) && HexFormat.isHexDigit(bytes[i + 2]);
            if (b <= 0x20 || b >= 0x7F || UNSAFE_QUERY_CHARS.indexOf(b) >= 0 || (b == '%' && !keepEscape)) {
                sb.append('%').append(HEX.toHexDigits((byte) b));
            } else {
                sb.append((char) b);
            }
        }
        return sb.toString();
    }
}
