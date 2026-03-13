# Kestra Supabase Plugin

## What

description = 'Supabase plugin for Kestra Exposes 5 plugin components (tasks, triggers, and/or conditions).

## Why

Enables Kestra workflows to interact with Supabase, allowing orchestration of Supabase-based operations as part of data pipelines and automation workflows.

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

### Important Commands

```bash
# Build the plugin
./gradlew shadowJar

# Run tests
./gradlew test

# Build without tests
./gradlew shadowJar -x test
```

### Configuration

All tasks and triggers accept standard Kestra plugin properties. Credentials should use
`{{ secret('SECRET_NAME') }}` — never hardcode real values.

## Agents

**IMPORTANT:** This is a Kestra plugin repository (prefixed by `plugin-`, `storage-`, or `secret-`). You **MUST** delegate all coding tasks to the `kestra-plugin-developer` agent. Do NOT implement code changes directly — always use this agent.
