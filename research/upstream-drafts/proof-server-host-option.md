DRAFT — not filed. Held for Kshot's weekly batch; would be posted publicly under his account. Do not file.

# Configurable bind host for the proof server (e.g. `MIDNIGHT_PROOF_SERVER_HOST`)

**Repo:** [midnightntwrk/midnight-ledger](https://github.com/midnightntwrk/midnight-ledger)
**Component:** proof-server
**Re-verified:** 2026-11-25 against tag `proof-server-9.0.2` and `main`

## Summary

Please consider adding a configurable bind host for the proof server, for example an env var and flag `MIDNIGHT_PROOF_SERVER_HOST` / `--host`, defaulting to the current `0.0.0.0` so existing Docker and scripted setups stay compatible.

## Why this would help

The official [Proving transactions locally](https://docs.midnight.network/guides/local-proving) guide is clear about exposure: the proof server has no authentication and no TLS, it listens on every interface inside its container, and it accepts cross-origin requests from any web page. The guide therefore says to publish its port on `127.0.0.1` only.

That advice works well with Docker, because `docker run -p 127.0.0.1:6300:6300` limits the published port even when the process inside the container binds `0.0.0.0`. It does not work for no-Docker installs. In those cases there is no port-publishing wrapper, so the process listens on every interface of the host. A typical case is on-device proving on Android under Termux or proot, where the official binary is run directly on the device.

Because the server also uses permissive CORS and has no authentication, any peer that can reach that port can submit proving jobs and see private inputs (witnesses). A bind-host option would let those installs listen on `127.0.0.1` only, without forking the binary or adding a wrapper.

## Evidence (re-checked 2026-11-25)

- Bind is hard-coded at [`proof-server/src/lib.rs:54`](https://github.com/midnightntwrk/midnight-ledger/blob/proof-server-9.0.2/proof-server/src/lib.rs#L54) at tag `proof-server-9.0.2`: `.bind(("0.0.0.0", port))`. The same line is on `main`.
- CORS is permissive at [`lib.rs:47`](https://github.com/midnightntwrk/midnight-ledger/blob/proof-server-9.0.2/proof-server/src/lib.rs#L47): `Cors::permissive()`. The same line is on `main`.
- The environment-backed flags in [`proof-server/src/main.rs` lines 31–58](https://github.com/midnightntwrk/midnight-ledger/blob/proof-server-9.0.2/proof-server/src/main.rs#L31-L58) at that tag are `MIDNIGHT_PROOF_SERVER_PORT`, `_VERBOSE`, `_JOB_CAPACITY`, `_NUM_WORKERS`, `_JOB_TIMEOUT` and `_NO_FETCH_PARAMS`. There is no host option. `main` has the same set of flags (the default for `job_capacity` differs on `main`, which is unrelated to bind).
- Official docs: [Proving transactions locally](https://docs.midnight.network/guides/local-proving) — "The proof server has no authentication and no TLS. It listens on every interface inside its container and accepts cross-origin requests from any web page. Publish its port on `127.0.0.1` only, as the commands in this guide do."

## Suggested minimal change

Default stays `0.0.0.0`, so current behaviour is unchanged. A sketch only; the authors would naturally adjust names and plumbing:

```rust
// proof-server/src/main.rs, inside Args, alongside the existing env-backed flags
#[arg(
    long,
    default_value = "0.0.0.0",
    env = "MIDNIGHT_PROOF_SERVER_HOST"
)]
host: String,

// proof-server/src/lib.rs — today:
.bind(("0.0.0.0", port))?;

// suggested: thread `host` through `server(...)` and
.bind((host.as_str(), port))?;
```

Operators who want a loopback-only listener could then run:

```bash
MIDNIGHT_PROOF_SERVER_HOST=127.0.0.1 midnight-proof-server
# or
midnight-proof-server --host 127.0.0.1
```

## Prior art in this repo

A read-only search of issues and pull requests in `midnightntwrk/midnight-ledger` on 2026-11-25 (terms included `MIDNIGHT_PROOF_SERVER_HOST`, "bind host", "bind address", `0.0.0.0`, "listens on every", "configurable host") found no existing request for a configurable bind host. Code search for `MIDNIGHT_PROOF_SERVER_HOST` returned no hits.

The closest related work is not a duplicate:

- [PR #804](https://github.com/midnightntwrk/midnight-ledger/pull/804) (merged) lets a wrapper script pass arguments through to the binary. It does not add a host option.
- [PR #862](https://github.com/midnightntwrk/midnight-ledger/pull/862) (open) adds a `--max-k` limit and touches `lib.rs`; it does not add a bind-host option.

Thank you for considering this.
