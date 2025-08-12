package io.kestra.plugin.supabase;

import com.google.common.base.Strings;
import io.kestra.core.junit.annotations.KestraTest;

@KestraTest
public abstract class AbstractSupabaseTest {
    private static final String SUPABASE_URL = "";
    private static final String SUPABASE_API_KEY = "";

    protected static boolean canNotBeEnabled() {
        return Strings.isNullOrEmpty(getUrl()) || Strings.isNullOrEmpty(getApiKey());
    }

    protected static String getUrl() {
        return SUPABASE_URL;
    }

    protected static String getApiKey() {
        return SUPABASE_API_KEY;
    }
}
