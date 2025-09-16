docker compose -f docker-compose-ci.yml up -d
DB_CTN="$(docker compose -f docker-compose-ci.yml ps -q db)"

# we wait for postgres
until docker exec "$DB_CTN" pg_isready -h 127.0.0.1 -U postgres -d appdb >/dev/null 2>&1; do
  sleep 1
done

# we apply the schema from init.sql
cat src/test/resources/sql/init.sql \
  | docker exec -i "$DB_CTN" psql -h 127.0.0.1 -U postgres -d appdb -v ON_ERROR_STOP=1 -f -

# we generate the JWT token
SECRET='this_is_a_demo_secret_change_me_32+chars'
b64url() { base64 | tr -d '\n=' | tr '/+' '_-'; }
HEADER=$(printf '{"alg":"HS256","typ":"JWT"}' | b64url)
PAYLOAD=$(printf '{"role":"anon"}' | b64url)
SIGNATURE=$(printf '%s.%s' "$HEADER" "$PAYLOAD" \
  | openssl dgst -sha256 -hmac "$SECRET" -binary | b64url)
JWT="$HEADER.$PAYLOAD.$SIGNATURE"

cat > src/test/resources/application-test.yml <<EOF
supabase:
  url: "http://localhost:54323/rest/v1"
  api-key: "$JWT"
EOF

echo "Supabase is ready to use"
