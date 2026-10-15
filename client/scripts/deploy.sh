#!/usr/bin/env bash
# Build the client and rsync dist/ to DreamHost.
# Settings come from exported env vars or client/.env.deploy.local (see .env.deploy.example).
# Usage: npm run deploy             (build + upload)
#        npm run deploy -- --dry-run  (build + show what would change, upload nothing)
set -euo pipefail

CLIENT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# Parsed instead of sourced, so the file can't run anything; never overrides an exported var.
load_env_file() {
  local file="$1" line key value
  [ -f "$file" ] || return 0
  while IFS= read -r line || [ -n "$line" ]; do
    case "$line" in ''|'#'*) continue ;; esac
    key="${line%%=*}"
    value="${line#*=}"
    if [ "$key" = "$line" ] || [[ ! "$key" =~ ^[A-Za-z_][A-Za-z0-9_]*$ ]]; then
      echo "Ignoring a malformed line in $(basename "$file")" >&2
      continue
    fi
    if ! printenv "$key" >/dev/null 2>&1; then
      export "$key=$value"
    fi
  done < "$file"
}

load_env_file "$CLIENT_DIR/.env.deploy.local"

: "${DEPLOY_USER:?DEPLOY_USER is not set (environment or .env.deploy.local)}"
: "${DEPLOY_HOST:?DEPLOY_HOST is not set (environment or .env.deploy.local)}"
: "${DEPLOY_PATH:?DEPLOY_PATH is not set (environment or .env.deploy.local)}"
# Vite reads VITE_* from the environment and bakes it into the bundle at build time.
: "${VITE_API_BASE_URL:?VITE_API_BASE_URL is not set (environment or .env.deploy.local)}"

# rsync --delete removes anything in DEPLOY_PATH that isn't in dist/, so refuse paths
# that look like a home directory or the root of the server.
if [ "${DEPLOY_PATH%/}" = "" ] || [ "${DEPLOY_PATH%/}" = "/" ] \
  || [[ "${DEPLOY_PATH%/}" =~ ^(/home/[^/]+|~)$ ]]; then
  echo "Refusing to deploy to '$DEPLOY_PATH': use the folder dedicated to this site." >&2
  exit 1
fi

RSYNC_FLAGS=(-avz --delete)
if [ "${1:-}" = "--dry-run" ]; then
  RSYNC_FLAGS+=(--dry-run)
  echo "Dry run: nothing will be uploaded."
fi

echo "Building against $VITE_API_BASE_URL..."
(cd "$CLIENT_DIR" && npm run build)

# Without .htaccess, deep links 404 and the security headers are missing.
if [ ! -f "$CLIENT_DIR/dist/.htaccess" ]; then
  echo "dist/.htaccess is missing; not deploying." >&2
  exit 1
fi

echo "Uploading dist/ to $DEPLOY_HOST:$DEPLOY_PATH..."
rsync "${RSYNC_FLAGS[@]}" "$CLIENT_DIR/dist/" "$DEPLOY_USER@$DEPLOY_HOST:${DEPLOY_PATH%/}/"

echo "Deploy complete."
