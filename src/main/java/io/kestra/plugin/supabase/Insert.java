package io.kestra.plugin.supabase;

import com.fasterxml.jackson.core.type.TypeReference;
import io.kestra.core.http.HttpRequest;
import io.kestra.core.http.HttpResponse;
import io.kestra.core.http.client.HttpClient;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Data;
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
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static io.kestra.core.utils.Rethrow.throwFunction;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Insert rows into a Supabase table",
    description = "POST rows via Supabase REST. Uses `Prefer: return=representation`; `select` defaults to `*`. Upserts when `onConflict` is set, with resolution defaulting to `merge-duplicates`."
)
@Plugin(
    examples = {
        @Example(
            title = "Insert a single record.",
            full = true,
            code = """
                id: supabase_insert_single
                namespace: company.team

                tasks:
                  - id: insert_user
                    type: io.kestra.plugin.supabase.Insert
                    url: https://your-project.supabase.com
                    apiKey: "{{ secret('SUPABASE_API_KEY') }}"
                    table: users
                    data:
                      name: "John Doe"
                      email: "john@example.com"
                      status: "active"
                """
        ),
        @Example(
            title = "Insert multiple records.",
            full = true,
            code = """
                id: supabase_insert_multiple
                namespace: company.team

                tasks:
                  - id: insert_users
                    type: io.kestra.plugin.supabase.Insert
                    url: https://your-project.supabase.com
                    apiKey: "{{ secret('SUPABASE_API_KEY') }}"
                    table: users
                    data:
                      - name: "John Doe"
                        email: "john@example.com"
                        status: "active"
                      - name: "Jane Smith"
                        email: "jane@example.com"
                        status: "active"
                """
        ),
        @Example(
            title = "Insert with conflict resolution (upsert).",
            full = true,
            code = """
                id: supabase_upsert
                namespace: company.team

                tasks:
                  - id: upsert_user
                    type: io.kestra.plugin.supabase.Insert
                    url: https://your-project.supabase.com
                    apiKey: "{{ secret('SUPABASE_API_KEY') }}"
                    table: users
                    data:
                      id: 123
                      name: "John Doe Updated"
                      email: "john.updated@example.com"
                    onConflict: "id"
                    resolution: "merge-duplicates"
                """
        )
    }
)
public class Insert extends AbstractSupabase implements RunnableTask<Insert.Output>, io.kestra.core.models.property.Data.From {

    @Schema(
        title = "Target table name",
        description = "Supabase table to insert into; value is rendered before the request"
    )
    @NotNull
    private Property<String> table;

    @Schema(
        title = "Rows to insert",
        description = "Single object or array of objects; rendered then sent as JSON",
        anyOf = {List.class, Map.class}
    )
    @NotNull
    private Object data;

    @Schema(
        title = "Columns returned",
        description = "Comma-separated columns returned from inserted rows; defaults to `*`"
    )
    private Property<String> select;

    @Schema(
        title = "Conflict target columns",
        description = "Comma-separated column names used for upsert conflict detection"
    )
    private Property<String> onConflict;

    @Schema(
        title = "Conflict resolution strategy",
        description = "How to handle conflicts: `merge-duplicates` (default) or `ignore-duplicates`"
    )
    @Builder.Default
    private Property<String> resolution = Property.ofValue("merge-duplicates");

    @Override
    public Output run(RunContext runContext) throws Exception {
        try (HttpClient client = this.client(runContext)) {
            String renderedTable = runContext.render(this.table).as(String.class).orElseThrow();

            List<Map<String, Object>> rows = Data.from(getFrom())
                    .read(runContext)
                    .map(throwFunction(obj -> runContext.render((Map<String, Object>) obj)))
                    .collectList()
                    .block();

            String endpoint = buildTableEndpoint(renderedTable);
            HttpRequest.HttpRequestBuilder requestBuilder = baseRequest(runContext, endpoint)
                .method("POST");

            var rOnConflict = runContext.render(this.onConflict).as(String.class).orElse(null);
            var rResolution = runContext.render(this.resolution).as(String.class).orElse("merge-duplicates");
            var rSelect = runContext.render(this.select).as(String.class).orElse("*");

            // prefer header
            var prefer = new StringBuilder("return=representation");
            if (rOnConflict != null && !rOnConflict.trim().isEmpty()) {
                prefer.append(",resolution=").append(rResolution);
            }
            requestBuilder.addHeader("Prefer", prefer.toString());

            // query params
            StringBuilder query = new StringBuilder("select=").append(rSelect);
            if (rOnConflict != null && !rOnConflict.trim().isEmpty()) {
                query.append("&on_conflict=").append(rOnConflict);
            }

            String baseUri = requestBuilder.build().getUri() + "?" + query;

            // Convert data to JSON
            var jsonBody = JacksonMapper.ofJson().writeValueAsString(rows);

            var request = requestBuilder
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
            List<Map<String, Object>> insertedRows = null;
            if (responseBody != null && !responseBody.trim().isEmpty()) {
                try {
                    insertedRows = JacksonMapper.ofJson().readValue(responseBody, new TypeReference<List<Map<String, Object>>>() {});
                } catch (Exception e) {
                    runContext.logger().warn("Failed to parse response as JSON: {}", e.getMessage());
                    insertedRows = List.of();
                }
            } else {
                insertedRows = List.of();
            }

            return Output.builder()
                .uri(request.getUri())
                .code(response.getStatus().getCode())
                .headers(response.getHeaders().map())
                .insertedRows(insertedRows)
                .insertedCount(insertedRows.size())
                .rawResponse(responseBody)
                .build();
        }
    }

    @Override
    public Object getFrom() {
        return getData();
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
            title = "Inserted rows returned"
        )
        private final List<Map<String, Object>> insertedRows;

        @Schema(
            title = "Number of rows inserted"
        )
        private final Integer insertedCount;

        @Schema(
            title = "Raw response body"
        )
        private final String rawResponse;
    }
}
