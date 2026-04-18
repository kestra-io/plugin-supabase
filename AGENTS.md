# Kestra Supabase Plugin

## What

- Provides plugin components under `io.kestra.plugin.supabase`.
- Includes classes such as `Delete`, `Insert`, `Update`, `Select`.

## Why

- This plugin integrates Kestra with Supabase.
- It provides tasks that call Supabase APIs for data and storage operations.

## How

### Architecture

Single-module plugin. Source packages under `io.kestra.plugin`:

- `supabase`

Infrastructure dependencies (Docker Compose services):

- `db`
- `postgrest`
- `traefik`

### Key Plugin Classes

- `io.kestra.plugin.supabase.Delete`
- `io.kestra.plugin.supabase.Insert`
- `io.kestra.plugin.supabase.Query`
- `io.kestra.plugin.supabase.Select`
- `io.kestra.plugin.supabase.Update`

### Project Structure

```
plugin-supabase/
├── src/main/java/io/kestra/plugin/supabase/
├── src/test/java/io/kestra/plugin/supabase/
├── build.gradle
└── README.md
```

## References

- https://kestra.io/docs/plugin-developer-guide
- https://kestra.io/docs/plugin-developer-guide/contribution-guidelines
