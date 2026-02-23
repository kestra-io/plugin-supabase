package io.kestra.plugin.supabase;

import com.fasterxml.jackson.core.type.TypeReference;
import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.http.HttpRequest;
import io.kestra.core.http.HttpResponse;
import io.kestra.core.http.client.HttpClient;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.core.serializers.JacksonMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Update rows in a Supabase table",
    description = "PATCHes rows via Supabase REST using a required PostgREST filter. Always requests `Prefer: return=representation`; `select` defaults to `*` to return all columns. Requires an API key with write access to the table."
)
@Plugin(
    examples = {
        @Example(
            title = "Update records with a simple filter.",
            full = true,
            code = """
                id: supabase_update_simple
                namespace: company.team

                tasks:
                  - id: update_user_status
                    type: io.kestra.plugin.supabase.Update
                    url: https://your-project.supabase.com
                    apiKey: "{{ secret('SUPABASE_API_KEY') }}"
                    table: users
                    data:
                      status: "inactive"
                      updated_at: "{{ now() }}"
                    filter: "id=eq.123"
                """
        ),
        @Example(
            title = "Update multiple records with complex filtering.",
            full = true,
            code = """
                id: supabase_update_multiple
                namespace: company.team

                tasks:
                  - id: update_inactive_users
                    type: io.kestra.plugin.supabase.Update
                    url: https://your-project.supabase.com
                    apiKey: "{{ secret('SUPABASE_API_KEY') }}"
                    table: users
                    data:
                      status: "archived"
                      archived_at: "{{ now() }}"
                    filter: "status=eq.inactive&last_login=lt.2023-01-01"
                """
        ),
        @Example(
            title = "Update with return of specific columns.",
            full = true,
            code = """
                id: supabase_update_with_return
                namespace: company.team

                tasks:
                  - id: update_user_email
                    type: io.kestra.plugin.supabase.Update
                    url: https://your-project.supabase.com
                    apiKey: "{{ secret('SUPABASE_API_KEY') }}"
                    table: users
                    data:
                      email: "newemail@example.com"
                      email_verified: false
                    filter: "id=eq.123"
                    select: "id,email,email_verified,updated_at"
                """
        )
    }
)
public class Update extends AbstractSupabase implements RunnableTask<Update.Output> {

    @Schema(
        title = "Target table name",
        description = "Supabase table to patch; value is rendered before the request."
    )
    @NotNull
    private Property<String> table;

    @Schema(
        title = "Row values to apply",
        description = "Map of column names to values sent as JSON after rendering expressions."
    )
    @NotNull
    private Property<Map<String, Object>> data;

    @Schema(
        title = "PostgREST filter",
        description = "Required filter expression (e.g., `id=eq.123`) that scopes which rows are updated; broad filters can modify many rows."
    )
    @NotNull
    private Property<String> filter;

    @Schema(
        title = "Columns returned",
        description = "Comma-separated columns to include in the response; defaults to `*`."
    )
    private Property<String> select;

    @Override
    public Output run(RunContext runContext) throws Exception {
        try (HttpClient client = this.client(runContext)) {
            String renderedTable = runContext.render(this.table).as(String.class).orElseThrow();
            Map<String, Object> renderedData = runContext.render(this.data).asMap(String.class, Object.class);
            String renderedFilter = runContext.render(this.filter).as(String.class).orElseThrow();
            
            String endpoint = buildTableEndpoint(renderedTable);
            HttpRequest.HttpRequestBuilder requestBuilder = baseRequest(runContext, endpoint)
                .method("PATCH");

            // Add return preference
            requestBuilder.addHeader("Prefer", "return=representation");
            
            // Build query parameters
            List<String> queryParams = new ArrayList<>();
            
            // Select columns to return
            String renderedSelect = runContext.render(this.select).as(String.class).orElse("*");
            queryParams.add("select=" + renderedSelect);
            
            // Add filter conditions
            if (renderedFilter != null && !renderedFilter.trim().isEmpty()) {
                queryParams.add(renderedFilter);
            }
            
            // Build final URI with query parameters
            String baseUri = requestBuilder.build().getUri().toString();
            if (!queryParams.isEmpty()) {
                String queryString = String.join("&", queryParams);
                baseUri += "?" + queryString;
            }
            
            // Convert data to JSON
            String jsonBody = JacksonMapper.ofJson().writeValueAsString(renderedData);
            
            HttpRequest request = requestBuilder
                .uri(new URI(baseUri))
                .body(HttpRequest.StringRequestBody.builder()
                    .content(jsonBody)
                    .contentType("application/json")
                    .build())
                .build();

            HttpResponse<Byte[]> response = client.request(request, Byte[].class);

            String responseBody = null;
            if (response.getBody() != null) {
                responseBody = IOUtils.toString(ArrayUtils.toPrimitive(response.getBody()), StandardCharsets.UTF_8.name());
            }

            // Parse response as JSON
            List<Map<String, Object>> updatedRows = null;
            if (responseBody != null && !responseBody.trim().isEmpty()) {
                try {
                    updatedRows = JacksonMapper.ofJson().readValue(responseBody, new TypeReference<List<Map<String, Object>>>() {});
                } catch (Exception e) {
                    runContext.logger().warn("Failed to parse response as JSON: {}", e.getMessage());
                    updatedRows = List.of();
                }
            } else {
                updatedRows = List.of();
            }

            return Output.builder()
                .uri(request.getUri())
                .code(response.getStatus().getCode())
                .headers(response.getHeaders().map())
                .updatedRows(updatedRows)
                .updatedCount(updatedRows.size())
                .rawResponse(responseBody)
                .build();
        }
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(
            title = "Executed request URI"
        )
        private final URI uri;

        @Schema(
            title = "HTTP status code"
        )
        private final Integer code;

        @Schema(
            title = "Response headers"
        )
        @PluginProperty(additionalProperties = List.class)
        private final Map<String, List<String>> headers;

        @Schema(
            title = "Updated rows returned"
        )
        private final List<Map<String, Object>> updatedRows;

        @Schema(
            title = "Number of rows updated"
        )
        private final Integer updatedCount;

        @Schema(
            title = "Raw response body"
        )
        private final String rawResponse;
    }
}
