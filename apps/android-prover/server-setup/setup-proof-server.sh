#!/usr/bin/env bash
# setup-proof-server.sh — install Midnight's official proof server 9.0.2
# WITHOUT Docker, by pulling the official image layers from Docker Hub
# and running the genuine binary through its own staged glibc loader.
#
# This is the Route 3 deployment for the Midnight Prover Android app
# (CLAUDE-AP-1 verdict): the arm64 build inside a Linux environment on
# the phone (Termux + proot Debian), reached by the app at
# http://127.0.0.1:6300. The same script works on a desktop/server for
# amd64 — the flow is identical, only the image architecture differs.
#
# Usage:
#   ./setup-proof-server.sh [--arch arm64|amd64] [--dir PATH] [--start]
#
# Defaults: arch = arm64 (phones), dir = ~/.midnight-proof-server.
# --start launches the server after install and verifies /version.
#
# Provenance: every byte comes from the official Docker Hub image
# midnightntwrk/proof-server:9.0.2. Nothing is rebuilt or patched; the
# binary runs unmodified via the loader that ships in its own image.
# Verified end-to-end on amd64 2026-11-25 (install -> launch -> /version
# 9.0.2 -> /ready ok -> a real DUST-spend payload proven via /prove).
# The arm64 path uses the same steps against the image's arm64 variant;
# its first on-device run is the owner's supervised phone test.
set -euo pipefail

REPO="midnightntwrk/proof-server"
TAG="9.0.2"
ARCH="arm64"
WORK="$HOME/.midnight-proof-server"
START=0

while [ $# -gt 0 ]; do
  case "$1" in
    --arch) ARCH="$2"; shift 2 ;;
    --dir) WORK="$2"; shift 2 ;;
    --start) START=1; shift ;;
    *) echo "unknown argument: $1" >&2; exit 2 ;;
  esac
done

case "$ARCH" in
  arm64) DOCKER_ARCH="arm64"; LOADER_NAME="ld-linux-aarch64.so.1" ;;
  amd64) DOCKER_ARCH="amd64"; LOADER_NAME="ld-linux-x86-64.so.2" ;;
  *) echo "unsupported arch: $ARCH (use arm64 or amd64)" >&2; exit 2 ;;
esac

IMG="$WORK/img-$DOCKER_ARCH"
ROOTFS="$WORK/rootfs-$DOCKER_ARCH"
mkdir -p "$IMG" "$ROOTFS"

echo "== Midnight proof server $TAG ($DOCKER_ARCH) -> $WORK"

# --- 1. Anonymous pull token -------------------------------------------
TOKEN=$(curl -fsSL "https://auth.docker.io/token?service=registry.docker.io&scope=repository:${REPO}:pull" \
  | python3 -c 'import sys, json; print(json.load(sys.stdin)["token"])')
AUTH="Authorization: Bearer $TOKEN"
ACCEPT='Accept: application/vnd.oci.image.index.v1+json, application/vnd.docker.distribution.manifest.list.v2+json, application/vnd.docker.distribution.manifest.v2+json, application/vnd.oci.image.manifest.v1+json'

# --- 2. Resolve the platform manifest ----------------------------------
curl -fsSL -H "$AUTH" -H "$ACCEPT" \
  "https://registry-1.docker.io/v2/${REPO}/manifests/${TAG}" -o "$IMG/index.json"

MANIFEST_DIGEST=$(python3 - "$IMG/index.json" "$DOCKER_ARCH" <<'EOF'
import sys, json
d = json.load(open(sys.argv[1]))
arch = sys.argv[2]
if 'manifests' in d:
    for m in d['manifests']:
        p = m.get('platform', {})
        if p.get('architecture') == arch and p.get('os') == 'linux':
            print(m['digest']); break
    else:
        sys.exit(f"no linux/{arch} manifest in index")
else:
    print('SELF')
EOF
)

if [ "$MANIFEST_DIGEST" = "SELF" ]; then
  cp "$IMG/index.json" "$IMG/manifest.json"
else
  curl -fsSL -H "$AUTH" -H "$ACCEPT" \
    "https://registry-1.docker.io/v2/${REPO}/manifests/${MANIFEST_DIGEST}" -o "$IMG/manifest.json"
fi

python3 - "$IMG/manifest.json" > "$IMG/layers.txt" <<'EOF'
import sys, json
d = json.load(open(sys.argv[1]))
for layer in d['layers']:
    print(layer['digest'])
EOF

# --- 3. Download layers (skip ones already present) ---------------------
while read -r dg; do
  fn="$IMG/layer_${dg#sha256:}.tar.gz"
  if [ ! -f "$fn" ]; then
    echo "   downloading layer ${dg#sha256:} (first 12)..."
    curl -fsSL -H "$AUTH" "https://registry-1.docker.io/v2/${REPO}/blobs/${dg}" -o "$fn"
  fi
done < "$IMG/layers.txt"

# --- 4. Extract in order, honouring whiteouts ---------------------------
while read -r dg; do
  fn="$IMG/layer_${dg#sha256:}.tar.gz"
  # The image's nix/store directories are read-only (0555). Without
  # --delay-directory-restore, a non-root extract (no proot fake-root) fails
  # with "Cannot mkdir: Permission denied" (GROK-17, reproduced on amd64).
  chmod -R u+w "$ROOTFS" 2>/dev/null || true
  tar --delay-directory-restore -xzf "$fn" -C "$ROOTFS" \
    || { echo "failed to extract layer ${dg#sha256:}" >&2; exit 1; }
  chmod -R u+w "$ROOTFS" 2>/dev/null || true
  find "$ROOTFS" -name '.wh.*' 2>/dev/null | while read -r wh; do
    base="$(basename "$wh")"; dir="$(dirname "$wh")"
    if [ "$base" != ".wh..wh..opq" ]; then rm -rf "$dir/${base#.wh.}"; fi
    rm -f "$wh"
  done
done < "$IMG/layers.txt"

# --- 5. Locate the binary + its staged loader ---------------------------
BIN="$(find "$ROOTFS" -name 'midnight-proof-server' -type f | head -1)"
[ -n "$BIN" ] || { echo "proof server binary not found in image" >&2; exit 1; }
LOADER="$(find "$ROOTFS" -name "$LOADER_NAME" | head -1)"
[ -n "$LOADER" ] || { echo "staged loader $LOADER_NAME not found in image" >&2; exit 1; }
LIBPATH="$(find "$ROOTFS/nix/store" -maxdepth 2 -type d -name lib 2>/dev/null | tr '\n' ':')"

# --- 6. Write the launcher ----------------------------------------------
LAUNCHER="$WORK/run-proof-server.sh"
cat > "$LAUNCHER" <<EOF
#!/usr/bin/env bash
# Generated by setup-proof-server.sh — runs the official, unmodified
# midnight-proof-server $TAG ($DOCKER_ARCH) via its staged loader.
echo "NETWORK NOTE: proof server 9.0.2 has no bind-address option (--help lists only --port, --verbose, --job-capacity, --num-workers, --job-timeout, --no-fetch-params) and listens on 0.0.0.0, i.e. every interface. Anyone on the same Wi-Fi can reach port 6300 and send it proving jobs. Use trusted networks only, and stop the server when you are done." >&2
exec "$LOADER" --library-path "$LIBPATH" "$BIN" "\$@"
EOF
chmod +x "$LAUNCHER"
echo "== installed: $BIN"
echo "== launcher:  $LAUNCHER"

# --- 7. Optionally start + verify ---------------------------------------
if [ "$START" = "1" ]; then
  echo "== starting server on port 6300 (log: $WORK/server.log)..."
  setsid nohup "$LAUNCHER" > "$WORK/server.log" 2>&1 < /dev/null &
  for i in $(seq 1 30); do
    sleep 2
    V="$(curl -s -m 3 http://127.0.0.1:6300/version || true)"
    if [ "$V" = "$TAG" ]; then
      echo "== proof server is LIVE: /version = $V"
      curl -s -m 3 http://127.0.0.1:6300/ready || true
      echo
      echo "== NETWORK NOTE: proof server 9.0.2 has no bind-address option (--help lists only --port, --verbose, --job-capacity, --num-workers, --job-timeout, --no-fetch-params) and listens on 0.0.0.0, i.e. every interface. Anyone on the same Wi-Fi can reach port 6300 and send it proving jobs. Use trusted networks only, and stop the server when you are done."
      exit 0
    fi
  done
  echo "server did not answer /version=$TAG within 60s; see $WORK/server.log" >&2
  exit 1
fi

echo "Start it with: $LAUNCHER   (then open the app and tap 'Check server')"
