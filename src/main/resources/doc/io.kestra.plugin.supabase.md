# How to use the Supabase plugin

Read and write data in Supabase from Kestra flows using the PostgREST API.

## Authentication

Set `url` (required, your Supabase project URL) and `apiKey` (required, your service role or anon key). Optionally set `schema` (default `public`). Store secrets in [secrets](https://kestra.io/docs/concepts/secret) and apply connection properties globally with [plugin defaults](https://kestra.io/docs/workflow-components/plugin-defaults).

## Tasks

`Select` fetches rows from a table — set `table` (required). Optionally set `select` (comma-separated columns, defaults to `*`), `filter` (PostgREST filter expression), `order`, `limit`, and `offset`. The output includes `rows`, `size`, `uri`, `code`, and `headers`.

`Insert` inserts one or more rows into a table — set `table` (required) and `data` (required, a single object or array). Optionally set `select`, `onConflict` (columns for upsert conflict detection), and `resolution` (`merge-duplicates` or `ignore-duplicates`, default `merge-duplicates`). The output includes `insertedRows` and `insertedCount`.

`Update` patches rows in a table — set `table` (required), `data` (required, a map of columns to new values), and `filter` (required, PostgREST filter expression). Optionally set `select`. The output includes `updatedRows` and `updatedCount`.

`Delete` removes rows from a table — set `table` (required) and `filter` (required, PostgREST filter expression). Optionally set `select`. The output includes `deletedRows` and `deletedCount`.

`Query` calls a Supabase stored procedure via RPC — set `functionName` (required). Optionally pass `parameters` (a map rendered as the JSON request body). The output includes `rows`, `size`, and `rawResponse`.
