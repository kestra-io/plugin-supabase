package io.kestra.plugin.supabase;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpHeaders;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;

import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.http.HttpRequest;
import io.kestra.core.http.HttpResponse;
import io.kestra.core.http.client.HttpClient;
import io.kestra.core.http.client.HttpClientResponseException;
import io.kestra.core.http.client.configurations.HttpConfiguration;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;
import io.kestra.core.serializers.JacksonMapper;

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

        // Ensure URL ends with /rest/v1 for the REST API
        var cleanUrl = rUrl.replaceAll("/+$", "");
        if (!cleanUrl.endsWith("/rest/v1")) {
            cleanUrl = cleanUrl + "/rest/v1";
        }

        var fullUrl = cleanUrl + endpoint;

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
     * Validates that the HTTP response indicates success (< 400).
     * If the status is >= 400, parses the error body (e.g. from PostgREST/Supabase)
     * and throws an {@link HttpClientResponseException}.
     */
    protected void validateResponse(HttpResponse<?> response, String responseBody) throws HttpClientResponseException {
        if (response != null && response.getStatus() != null && response.getStatus().getCode() >= 400) {
            var statusCode = response.getStatus().getCode();
            var errorMessage = "Supabase API call failed with status " + statusCode;

            if (responseBody != null && !responseBody.trim().isEmpty()) {
                try {
                    var errorMap = JacksonMapper.ofJson().readValue(
                        responseBody,
                        new TypeReference<Map<String, Object>>() {}
                    );
                    if (errorMap.containsKey("message") && errorMap.get("message") != null) {
                        var message = String.valueOf(errorMap.get("message"));
                        var code = errorMap.get("code");
                        var details = errorMap.get("details");
                        var hint = errorMap.get("hint");

                        var sb = new StringBuilder("Supabase error");
                        if (code != null) {
                            sb.append(" [").append(code).append("]");
                        }
                        sb.append(": ").append(message);
                        if (details != null && !String.valueOf(details).trim().isEmpty()) {
                            sb.append(" (details: ").append(details).append(")");
                        }
                        if (hint != null && !String.valueOf(hint).trim().isEmpty()) {
                            sb.append(" (hint: ").append(hint).append(")");
                        }
                        errorMessage = sb.toString();
                    } else if (errorMap.containsKey("error_description") && errorMap.get("error_description") != null) {
                        errorMessage = "Supabase error: " + errorMap.get("error_description");
                    } else if (errorMap.containsKey("error") && errorMap.get("error") != null) {
                        errorMessage = "Supabase error: " + errorMap.get("error");
                    } else {
                        errorMessage = errorMessage + ": " + responseBody;
                    }
                } catch (Exception ignored) {
                    errorMessage = errorMessage + ": " + responseBody;
                }
            }

            throw new HttpClientResponseException(errorMessage, response);
        }
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
}
