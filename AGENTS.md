# Kestra Supabase Plugin

## What

- Provides plugin components under `io.kestra.plugin.supabase`.
- Includes classes such as `Delete`, `Insert`, `Update`, `Select`.

## Why

- What user problem does this solve? Teams need to call Supabase APIs for data and storage operations from orchestrated workflows instead of relying on manual console work, ad hoc scripts, or disconnected schedulers.
- Why would a team adopt this plugin in a workflow? It keeps Supabase steps in the same Kestra flow as upstream preparation, approvals, retries, notifications, and downstream systems.
- What operational/business outcome does it enable? It reduces manual handoffs and fragmented tooling while improving reliability, traceability, and delivery speed for processes that depend on Supabase.

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
