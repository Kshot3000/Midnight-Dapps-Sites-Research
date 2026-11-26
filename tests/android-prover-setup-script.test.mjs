import assert from "node:assert/strict";
import { spawnSync } from "node:child_process";
import { mkdtempSync, readFileSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { fileURLToPath } from "node:url";
import { test } from "node:test";

// GROK-17: the setup script's post-start exposure check (Joe) and the
// non-root extraction fix, tested without downloading anything.
const root = fileURLToPath(new URL("../", import.meta.url));
const script = join(root, "apps/android-prover/server-setup/setup-proof-server.sh");
const header = "  sl  local_address rem_address   st tx_queue rx_queue tr tm->when retrnsmt   uid  timeout inode\n";
const v4 = (local, st = "0A") => `   0: ${local} 00000000:0000 ${st} 00000000:00000000 00:00000000 00000000  1000        0 12345 1 0000000000000000 100 0 0 10 0\n`;
const v6 = (local, st = "0A") => `   0: ${local} 00000000000000000000000000000000:0000 ${st} 00000000:00000000 00:00000000 00000000  1000        0 12346 1 0000000000000000 100 0 0 10 0\n`;
const dir = mkdtempSync(join(tmpdir(), "grok17-tcp-"));
const table = (name, ...rows) => { const p = join(dir, name); writeFileSync(p, header + rows.join("")); return p; };
const check = (...args) => spawnSync("bash", [script, "--check-exposure", ...args], { encoding: "utf8" });

// 6300 = 0x189C
test("0.0.0.0:6300 (what proof server 9.0.2 does) is reported as exposed, loudly", () => {
  const r = check("6300", table("a", v4("00000000:189C")));
  assert.equal(r.status, 2, r.stdout + r.stderr);
  assert.match(r.stdout, /listening on 0\.0\.0\.0/);
  assert.match(r.stdout, /!! WARNING: .*reachable from other devices on your network \(Wi-Fi\)/);
});

test("IPv6 :: alone (tcp6, all zeros) is exposed", () => {
  const r = check("6300", table("g", ""), table("g6", v6("00000000000000000000000000000000:189C")));
  assert.equal(r.status, 2, r.stdout);
  assert.match(r.stdout, /listening on :: /);
  assert.match(r.stdout, /!! WARNING/);
});

test("IPv6 ::1 alone is safe", () => {
  const r = check("6300", table("h", ""), table("h6", v6("00000000000000000000000001000000:189C")));
  assert.equal(r.status, 0, r.stdout);
  assert.match(r.stdout, /listening on ::1 \(loopback\)/);
});

test("IPv4 127.0.0.1 alone is safe", () => {
  const r = check("6300", table("i", v4("0100007F:189C")));
  assert.equal(r.status, 0, r.stdout);
});

test("IPv6 :: and a LAN address are exposed too", () => {
  assert.equal(check("6300", table("b", v4("0100007F:189C")), table("b6", v6("00000000000000000000000000000000:189C"))).status, 2);
  assert.equal(check("6300", table("c", v4("1401A8C0:189C"))).status, 2); // 192.168.1.20
});

test("127.0.0.1 and ::1 only is the one safe verdict", () => {
  const r = check("6300", table("d", v4("0100007F:189C")), table("d6", v6("00000000000000000000000001000000:189C")));
  assert.equal(r.status, 0, r.stdout);
  assert.match(r.stdout, /listening on 127\.0\.0\.1 \(loopback\)/);
  assert.match(r.stdout, /OK: port 6300 is reachable only from this device/);
  assert.doesNotMatch(r.stdout, /WARNING/);
});

test("non-LISTEN sockets and other ports are ignored; lower-case hex works", () => {
  const r = check("6300", table("e", v4("00000000:189C", "01"), v4("00000000:1F90"), v4("0100007f:189c")));
  assert.equal(r.status, 0, r.stdout);
});

test("port not found → can't verify, loud warning; unreadable tables → loud warning, never 'safe'", () => {
  const nf = check("6300", table("f", v4("00000000:1F90")), table("f6", v6("00000000000000000000000000000000:1F90")));
  assert.equal(nf.status, 1);
  assert.match(nf.stdout, /can't be verified/);
  assert.match(nf.stdout, /!! WARNING/);
  const r = check("6300", join(dir, "missing"));
  assert.equal(r.status, 3);
  assert.match(r.stdout, /WARNING/);
});

test("the setup script extracts layers safely as a non-root user and runs the check after start", () => {
  const s = readFileSync(script, "utf8");
  assert.match(s, /tar --delay-directory-restore -xzf/);
  assert.match(s, /chmod -R u\+w "\$ROOTFS"/);
  assert.doesNotMatch(s, /\|\| tar -xf/, "the old fallback hid extraction failures");
  assert.match(s, /"\$CHECKER" 6300/);
});
