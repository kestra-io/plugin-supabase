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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Call a Supabase stored procedure",
    description = "Invokes a Postgres function over Supabase RPC. Expects the function to exist and accept the provided parameters; sends JSON body (empty object when no parameters) and returns parsed rows."
)
@Plugin(
    examples = {
        @Example(
            title = "Execute a simple query using a stored procedure.",
            full = true,
            code = """
                id: supabase_query
                namespace: company.team

                tasks:
                  - id: query_users
                    type: io.kestra.plugin.supabase.Query
                    url: https://your-project.supabase.com
                    apiKey: "{{ secret('SUPABASE_API_KEY') }}"
                    functionName: get_users
                    parameters:
                      limit: 10
                      offset: 0
                """
        ),
        @Example(
            title = "Execute a parameterized query.",
            full = true,
            code = """
                id: supabase_parameterized_query
                namespace: company.team

                tasks:
                  - id: query_user_by_id
                    type: io.kestra.plugin.supabase.Query
                    url: https://your-project.supabase.com
                    apiKey: "{{ secret('SUPABASE_API_KEY') }}"
                    functionName: get_user_by_id
                    parameters:
                      user_id: 123
                """
        )
    }
)
public class Query extends AbstractSupabase implements RunnableTask<Query.Output> {

    @Schema(
        title = "Stored procedure name",
        description = "Supabase function name to call via RPC; value is rendered before the request"
    )
    @NotNull
    private Property<String> functionName;

    @Schema(
        title = "Procedure parameters",
        description = "Optional map rendered to JSON and sent as the request body; empty object is sent when not provided"
    )
    private Property<Map<String, Object>> parameters;

    @Override
    public Output run(RunContext runContext) throws Exception {
        try (HttpClient client = this.client(runContext)) {
            String renderedFunctionName = runContext.render(this.functionName).as(String.class).orElseThrow();
            Map<String, Object> renderedParameters = runContext.render(this.parameters).asMap(String.class, Object.class);
            
            String endpoint = buildRpcEndpoint(renderedFunctionName);
            HttpRequest.HttpRequestBuilder requestBuilder = baseRequest(runContext, endpoint)
                .method("POST");

            // Add parameters as JSON body if provided
            if (renderedParameters != null && !renderedParameters.isEmpty()) {
                String jsonBody = JacksonMapper.ofJson().writeValueAsString(renderedParameters);
                requestBuilder.body(HttpRequest.StringRequestBody.builder()
                    .content(jsonBody)
                    .contentType("application/json")
                    .build());
            } else {
                requestBuilder.body(HttpRequest.StringRequestBody.builder()
                    .content("{}")
                    .contentType("application/json")
                    .build());
            }

            HttpRequest request = requestBuilder.build();
            HttpResponse<Byte[]> response = client.request(request, Byte[].class);

            String responseBody = null;
            if (response.getBody() != null) {
                responseBody = IOUtils.toString(ArrayUtils.toPrimitive(response.getBody()), StandardCharsets.UTF_8.name());
            }

            // Parse response as JSON
            List<Map<String, Object>> rows = null;
            if (responseBody != null && !responseBody.trim().isEmpty()) {
                try {
                    rows = JacksonMapper.ofJson().readValue(responseBody, new TypeReference<List<Map<String, Object>>>() {});
                } catch (Exception e) {
                    // If it's not a list, try to parse as a single object
                    try {
                        Map<String, Object> singleRow = JacksonMapper.ofJson().readValue(responseBody, new TypeReference<Map<String, Object>>() {});
                        rows = List.of(singleRow);
                    } catch (Exception ex) {
                        // If parsing fails, return raw response
                        runContext.logger().warn("Failed to parse response as JSON: {}", ex.getMessage());
                    }
                }
            }

            return Output.builder()
                .uri(request.getUri())
                .code(response.getStatus().getCode())
                .headers(response.getHeaders().map())
                .rows(rows)
                .size(rows != null ? rows.size() : 0)
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
            title = "Result rows"
        )
        private final List<Map<String, Object>> rows;

        @Schema(
            title = "Number of rows returned"
        )
        private final Integer size;

        @Schema(
            title = "Raw response body"
        )
        private final String rawResponse;
    }
}
