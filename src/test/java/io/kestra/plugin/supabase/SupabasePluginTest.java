package io.kestra.plugin.supabase;

import io.kestra.core.models.property.Property;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SupabasePluginTest {

    @Test
    void testSelectTaskCreation() {
        Select task = Select.builder()
            .url(Property.ofValue("https://test-project.supabase.com"))
            .apiKey(Property.ofValue("test-api-key"))
            .table(Property.ofValue("users"))
            .build();

        assertNotNull(task);
        assertNotNull(task.getUrl());
        assertNotNull(task.getApiKey());
        assertNotNull(task.getTable());
    }

    @Test
    void testInsertTaskCreation() {
        Insert task = Insert.builder()
            .url(Property.ofValue("https://test-project.supabase.com"))
            .apiKey(Property.ofValue("test-api-key"))
            .table(Property.ofValue("users"))
            .data(Property.ofValue(java.util.Map.of("name", "test")))
            .build();

        assertNotNull(task);
        assertNotNull(task.getUrl());
        assertNotNull(task.getApiKey());
        assertNotNull(task.getTable());
        assertNotNull(task.getData());
    }

    @Test
    void testUpdateTaskCreation() {
        Update task = Update.builder()
            .url(Property.ofValue("https://test-project.supabase.com"))
            .apiKey(Property.ofValue("test-api-key"))
            .table(Property.ofValue("users"))
            .data(Property.ofValue(java.util.Map.of("name", "updated")))
            .filter(Property.ofValue("id=eq.1"))
            .build();

        assertNotNull(task);
        assertNotNull(task.getUrl());
        assertNotNull(task.getApiKey());
        assertNotNull(task.getTable());
        assertNotNull(task.getData());
        assertNotNull(task.getFilter());
    }

    @Test
    void testDeleteTaskCreation() {
        Delete task = Delete.builder()
            .url(Property.ofValue("https://test-project.supabase.com"))
            .apiKey(Property.ofValue("test-api-key"))
            .table(Property.ofValue("users"))
            .filter(Property.ofValue("id=eq.1"))
            .build();

        assertNotNull(task);
        assertNotNull(task.getUrl());
        assertNotNull(task.getApiKey());
        assertNotNull(task.getTable());
        assertNotNull(task.getFilter());
    }

    @Test
    void testQueryTaskCreation() {
        Query task = Query.builder()
            .url(Property.ofValue("https://test-project.supabase.com"))
            .apiKey(Property.ofValue("test-api-key"))
            .functionName(Property.ofValue("get_users"))
            .build();

        assertNotNull(task);
        assertNotNull(task.getUrl());
        assertNotNull(task.getApiKey());
        assertNotNull(task.getFunctionName());
    }

    @Test
    void testEndpointBuilding() {
        Select selectTask = Select.builder().build();
        Query queryTask = Query.builder().build();
        
        // Test table endpoint building
        String tableEndpoint = selectTask.buildTableEndpoint("users");
        assertEquals("/users", tableEndpoint);
        
        // Test RPC endpoint building
        String rpcEndpoint = queryTask.buildRpcEndpoint("get_users");
        assertEquals("/rpc/get_users", rpcEndpoint);
    }

    @Test
    void testTaskBuilders() {
        // Test that all task builders work without throwing exceptions
        assertDoesNotThrow(() -> Select.builder().build());
        assertDoesNotThrow(() -> Insert.builder().build());
        assertDoesNotThrow(() -> Update.builder().build());
        assertDoesNotThrow(() -> Delete.builder().build());
        assertDoesNotThrow(() -> Query.builder().build());
    }

    @Test
    void testPropertyConfiguration() {
        Select task = Select.builder()
            .url(Property.ofValue("https://example.supabase.com"))
            .apiKey(Property.ofValue("api-key"))
            .table(Property.ofValue("test_table"))
            .select(Property.ofValue("id,name"))
            .filter(Property.ofValue("status=eq.active"))
            .limit(Property.ofValue(10))
            .build();

        // Verify properties are set
        assertNotNull(task.getUrl());
        assertNotNull(task.getApiKey());
        assertNotNull(task.getTable());
        assertNotNull(task.getSelect());
        assertNotNull(task.getFilter());
        assertNotNull(task.getLimit());
    }

    @Test
    void testComplexDataTypes() {
        // Test with complex data structures
        java.util.Map<String, Object> complexData = java.util.Map.of(
            "name", "Test User",
            "metadata", java.util.Map.of("role", "admin", "active", true),
            "tags", java.util.List.of("important", "verified")
        );

        Insert task = Insert.builder()
            .url(Property.ofValue("https://test.supabase.com"))
            .apiKey(Property.ofValue("key"))
            .table(Property.ofValue("users"))
            .data(Property.ofValue(complexData))
            .build();

        assertNotNull(task.getData());
    }
}
