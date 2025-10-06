#!/usr/bin/env bash
# wait-for-it.sh: Waits for a service to be ready before continuing

set -e

host="$1"
shift
port="$1"
shift

until nc -z "$host" "$port"; do
  echo "⏳ Waiting for $host:$port to be available..."
  sleep 2
done

echo "✅ $host:$port is available!"
exec "$@"
