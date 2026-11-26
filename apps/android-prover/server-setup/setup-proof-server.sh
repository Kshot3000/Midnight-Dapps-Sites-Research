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
#   ./setup-proof-server.sh [--arch arm64|amd64] [--dir PATH] [--port N] [--start]
#   ./setup-proof-server.sh --check-exposure [PORT] [TCP_TABLE_FILE ...]
#
# Defaults: arch = arm64 (phones), dir = ~/.midnight-proof-server, port = 6300.
# --start launches the server after install and verifies /version on --port.
#
# Provenance: every byte comes from the official Docker Hub image
# midnightntwrk/proof-server:9.0.2. Nothing is rebuilt or patched; the
# binary runs unmodified via the loader that ships in its own image.
# Verified end-to-end on amd64 2026-11-25 (install -> launch -> /version
# 9.0.2 -> /ready ok -> a real DUST-spend payload proven via /prove).
# The arm64 path uses the same steps against the image's arm64 variant;
# its first on-device run is the owner's supervised phone test.
set -euo pipefail

# --- Exposure check (Joe, GROK-17) --------------------------------------
# Writes a small checker that reads /proc/net/tcp{,6} and reports where the
# server's port is ACTUALLY listening. It is run after every start; anything
# but loopback-only (or an unreadable table) prints a loud Wi-Fi warning, so
# nothing can look safe without being verified. Test hook:
#   ./setup-proof-server.sh --check-exposure [PORT] [TCP_TABLE_FILE ...]
write_checker() {
  cat > "$1" <<'CHECKER_EOF'
#!/usr/bin/env bash
# check-exposure.sh — where is the proof server actually listening?
#
# Usage: check-exposure.sh [PORT] [TCP_TABLE_FILE ...]
#   PORT defaults to 6300; the tables default to /proc/net/tcp and /proc/net/tcp6.
#
# Prints one line per LISTEN socket on PORT ("127.0.0.1", "0.0.0.0", "::", ...)
# and a verdict. Exit codes:
#   0  listening ONLY on loopback (127.0.0.0/8, ::1, ::ffff:127.x)
#   2  listening on any other address — reachable from your Wi-Fi (loud warning)
#   3  the tables couldn't be read (Android often hides /proc/net) — treated as exposed
#   1  nothing is listening on PORT, so nothing could be verified (loud warning)
#
# Proof server 9.0.2 has no bind-address option and listens on 0.0.0.0, so on a
# stock install expect exit 2. This check exists so that nothing (a future option,
# a shim, a firewall rule) can look safe without being verified.
set -u
PORT="${1:-6300}"
shift || true
if [ "$#" -eq 0 ]; then set -- /proc/net/tcp /proc/net/tcp6; fi
PORT_HEX="$(printf '%04X' "$PORT")"

WARNING="WARNING: the proof server on port $PORT is reachable from other devices on your network (Wi-Fi). Anyone on the same network can send it proving jobs. Use trusted networks only and stop the server (Ctrl-C) when you are done."

readable=0
addrs=""
need_tcp6=0
tcp6_readable=0
for f in "$@"; do
  case "$f" in *tcp6*|*TCP6*) need_tcp6=1 ;; esac
  if [ ! -r "$f" ]; then
    continue
  fi
  readable=1
  case "$f" in *tcp6*|*TCP6*) tcp6_readable=1 ;; esac
  # Columns: sl local_address rem_address st ...; st 0A = LISTEN. Hex is upper or lower case.
  found="$(awk -v port="$PORT_HEX" '
    NR > 1 {
      split($2, a, ":"); st = toupper($4); p = toupper(a[2]); h = toupper(a[1])
      if (st == "0A" && p == port) print h
    }' "$f")"
  [ -n "$found" ] && addrs="$addrs $found"
done

if [ "$readable" = 0 ]; then
  echo "!! Could not read $* — can't verify where port $PORT listens."
  echo "!! $WARNING"
  exit 3
fi
# Joe nit 8: a missing IPv6 table can hide a listener on ::, so loopback on
# IPv4 alone is never enough to say OK when tcp6 was requested but unreadable.
if [ "$need_tcp6" = 1 ] && [ "$tcp6_readable" = 0 ]; then
  echo "!! Could not read the IPv6 table (tcp6) — can't verify where port $PORT listens."
  echo "!! $WARNING"
  exit 3
fi
if [ -z "$addrs" ]; then
  echo "!! Nothing is listening on port $PORT yet, so where it listens can't be verified."
  echo "!! $WARNING"
  exit 1
fi

exposed=0
for h in $addrs; do
  case "$h" in
    ????????)                                  # IPv4, little-endian: 0100007F = 127.0.0.1
      b1="${h:6:2}"
      ip="$((16#${h:6:2})).$((16#${h:4:2})).$((16#${h:2:2})).$((16#${h:0:2}))"
      if [ "$b1" = "7F" ]; then echo "listening on $ip (loopback)"; else echo "listening on $ip"; exposed=1; fi ;;
    00000000000000000000000001000000)
      echo "listening on ::1 (loopback)" ;;
    0000000000000000FFFF0000??????7F)
      echo "listening on ::ffff:127.x (loopback)" ;;
    00000000000000000000000000000000)
      echo "listening on :: (every IPv6/IPv4 interface)"; exposed=1 ;;
    *)
      echo "listening on IPv6 $h"; exposed=1 ;;
  esac
done

if [ "$exposed" = 1 ]; then
  echo
  echo "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
  echo "!! $WARNING"
  echo "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
  exit 2
fi
echo "OK: port $PORT is reachable only from this device (loopback)."
exit 0
CHECKER_EOF
  chmod +x "$1"
}

if [ "${1:-}" = "--check-exposure" ]; then
  shift
  tmp="$(mktemp)"; write_checker "$tmp"
  set +e; bash "$tmp" "$@"; rc=$?; rm -f "$tmp"; exit "$rc"
fi

REPO="midnightntwrk/proof-server"
TAG="9.0.2"
ARCH="arm64"
WORK="$HOME/.midnight-proof-server"
START=0
PORT=6300

while [ $# -gt 0 ]; do
  case "$1" in
    --arch) ARCH="$2"; shift 2 ;;
    --dir) WORK="$2"; shift 2 ;;
    --port) PORT="$2"; shift 2 ;;
    --start) START=1; shift ;;
    *) echo "unknown argument: $1" >&2; exit 2 ;;
  esac
done
case "$PORT" in
  ''|*[!0-9]*) echo "invalid --port: $PORT" >&2; exit 2 ;;
esac
if [ "$PORT" -lt 1 ] || [ "$PORT" -gt 65535 ]; then
  echo "invalid --port: $PORT" >&2; exit 2
fi

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
CHECKER="$WORK/check-exposure.sh"
write_checker "$CHECKER"
cat > "$LAUNCHER" <<EOF
#!/usr/bin/env bash
# Generated by setup-proof-server.sh — runs the official, unmodified
# midnight-proof-server $TAG ($DOCKER_ARCH) via its staged loader.
echo "NETWORK NOTE: proof server 9.0.2 has no bind-address option (--help lists only --port, --verbose, --job-capacity, --num-workers, --job-timeout, --no-fetch-params) and listens on 0.0.0.0, i.e. every interface. Anyone on the same Wi-Fi can reach the port and send it proving jobs. Use trusted networks only, and stop the server when you are done." >&2
# Post-start check: once the port is listening, report where (see $CHECKER).
PORT=6300
prev=""
for a in "\$@"; do [ "\$prev" = "--port" ] && PORT="\$a"; case "\$a" in --port=*) PORT="\${a#--port=}" ;; esac; prev="\$a"; done
(
  for i in \$(seq 1 60); do
    sleep 2
    "$CHECKER" "\$PORT" >/dev/null 2>&1; rc=\$?
    if [ "\$rc" != 1 ]; then "$CHECKER" "\$PORT" >&2; exit 0; fi
  done
  "$CHECKER" "\$PORT" >&2  # never saw a listener: say it can't be verified
) &
exec "$LOADER" --library-path "$LIBPATH" "$BIN" "\$@"
EOF
chmod +x "$LAUNCHER"
echo "== installed: $BIN"
echo "== launcher:  $LAUNCHER"
echo "== exposure check: $CHECKER [PORT]"

# --- 7. Optionally start + verify ---------------------------------------
if [ "$START" = "1" ]; then
  echo "== starting server on port $PORT (log: $WORK/server.log)..."
  setsid nohup "$LAUNCHER" --port "$PORT" > "$WORK/server.log" 2>&1 < /dev/null &
  for i in $(seq 1 30); do
    sleep 2
    V="$(curl -s -m 3 "http://127.0.0.1:${PORT}/version" || true)"
    if [ "$V" = "$TAG" ]; then
      echo "== proof server is LIVE: /version = $V"
      curl -s -m 3 "http://127.0.0.1:${PORT}/ready" || true
      echo
      echo "== where is port $PORT listening? (reads /proc/net/tcp and tcp6)"
      "$CHECKER" "$PORT" || true
      exit 0
    fi
  done
  echo "server did not answer /version=$TAG within 60s; see $WORK/server.log" >&2
  exit 1
fi

echo "Start it with: $LAUNCHER   (then open the app and tap 'Check server')"
