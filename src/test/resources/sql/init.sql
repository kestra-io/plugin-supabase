DO $$
BEGIN
CREATE ROLE anon NOLOGIN;
EXCEPTION WHEN duplicate_object THEN
END
$$;

GRANT USAGE ON SCHEMA public TO anon;

DROP TABLE IF EXISTS public.kestra;
CREATE TABLE public.kestra (
   id   SERIAL PRIMARY KEY,
   name TEXT NOT NULL
);

GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE public.kestra TO anon;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO anon;

CREATE OR REPLACE FUNCTION public.get_users()
RETURNS SETOF public.kestra
LANGUAGE sql
STABLE
AS $$
SELECT * FROM public.kestra;
$$;

GRANT EXECUTE ON FUNCTION public.get_users() TO anon;

NOTIFY pgrst, 'reload schema';
