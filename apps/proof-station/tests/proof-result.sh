#!/usr/bin/env bash
set -euo pipefail

app=$(cd "$(dirname "$0")/.." && pwd)
source "$app/gpu-lab.sh"

header=$'mode\trep\tk\tthreads\tstart_load1\tproof\tstate\tprove_s\tverify_s\tverified\tmsm_calls\tntt_calls\tquotient_calls\tdevice_errors\tfallbacks\tdeclines\thook_declines\th2d_copies\th2d_bytes\td2h_copies\td2h_bytes\tmsm_d2h_copies\tmsm_d2h_bytes\tstage\tpeak_device_bytes\tgpu_model\tdevice_total_bytes'
gpu_cold=$'gpu\t1\t16\t8\t0.20\t0\tcold\t0.250\t0.010\ttrue\t2\t4\t1\t0\t0\t[]\t[]\t0\t0\t0\t0\t0\t0\t[]\t2048\tfixture\t8192'
gpu_warm=$'gpu\t1\t16\t8\t0.20\t1\twarm\t0.200\t0.009\ttrue\t2\t4\t1\t0\t1\t[]\t[]\t0\t0\t0\t0\t0\t0\t[]\t1024\tfixture\t8192'
cpu_cold=$'cpu\t1\t16\t8\t0.20\t0\tcold\t1.250\t0.010\ttrue\t0\t0\t0\t0\t0\t[]\t[]\t0\t0\t0\t0\t0\t0\t[]\t0\tNA\t0'
cpu_warm=$'cpu\t1\t16\t8\t0.20\t1\twarm\t1.200\t0.009\ttrue\t0\t0\t0\t0\t0\t[]\t[]\t0\t0\t0\t0\t0\t0\t[]\t0\tNA\t0'

check_rows() {
    printf '%s\n' "$header" "$2" "$3" | verify_proof_rows "$1"
}

reject() {
    if check_rows "$1" "$2" "$3"; then
        printf 'Expected a refused %s fixture\n' "$4" >&2
        exit 1
    fi
    printf 'PASS · Refused %s fixture\n' "$4"
}

[[ "$(check_rows gpu "$gpu_cold" "$gpu_warm")" == $'STATION_PROOF\t2\t0.250\t0.200\t0.009\t14\t2048\t1' ]]
printf 'PASS · Verified fixture reports CUDA work, verification time, memory and fallbacks\n'
reject gpu "$gpu_cold" "$cpu_warm" "CPU result in CUDA mode"
reject gpu "$gpu_cold" "${gpu_warm/$'\ttrue\t'/$'\tfalse\t'}" "unverified CUDA"
reject gpu "$gpu_cold" "${gpu_warm/$'\t2\t4\t1\t0\t1\t'/$'\t0\t0\t0\t0\t1\t'}" "zero-device-work"
reject gpu "$gpu_cold" "${gpu_warm/$'\t2\t4\t1\t0\t1\t'/$'\t2\t4\t1\t1\t1\t'}" "device-error"
reject gpu "$gpu_cold" "$gpu_cold" "duplicate cold row"

[[ "$(check_rows cpu "$cpu_cold" "$cpu_warm")" == $'STATION_PROOF\t2\t1.250\t1.200\t0.009\t0\t0\t0' ]]
printf 'PASS · CPU fixture reports both verified proof timings without GPU counters\n'
reject cpu "$cpu_cold" "$gpu_warm" "CUDA result in CPU mode"
reject cpu "$cpu_cold" "${cpu_warm/$'\ttrue\t'/$'\tfalse\t'}" "unverified CPU"
reject cpu "$cpu_cold" "${cpu_warm/$'\ttrue\t0\t'/$'\ttrue\t1\t'}" "CPU row with device work"

# Use the production prove command with fixture telemetry and proof binaries.
# The WSL passthrough check is the only platform dependency bypassed here.
fixture=$(mktemp -d)
trap 'rm -rf -- "$fixture"' EXIT
export XDG_DATA_HOME="$fixture"
export STATION_FIXTURE_DIR="$fixture"
fixture_uuid=GPU-c0de0000-1111-2222-3333-444444444444
work="$fixture/midnight-proof-station/$revision/sm-86"

require_wsl_cuda() { :; }
nvidia-smi() {
    [[ "$1" == -i && ("$2" == 0 || "$2" == "$fixture_uuid") ]] || return 1
    printf '%s, Fixture GPU, 8.6, %s, 24576, 610.60\n' "$fixture_uuid" "$(< "$fixture/free-mib")"
}

printf '3071\n' > "$fixture/free-mib"
[[ "$(main detect 0)" == $'STATION_GPU\tFixture GPU\t86\t3071\t24576\t610.60\t3072' ]]
if blocked=$(main prove 0 2>&1); then
    fail "CUDA proving accepted 3071 MiB of free VRAM."
fi
[[ "$blocked" == *"This device has 3071 MiB free."* && ! -d "$work" ]]
printf 'PASS · CUDA detection reports low memory and proving stops before preparation\n'

printf 'N/A\n' > "$fixture/free-mib"
if blocked=$(main prove 0 2>&1); then
    fail "CUDA proving accepted malformed VRAM telemetry."
fi
[[ "$blocked" == *"did not report free and total VRAM in MiB."* ]]
printf 'PASS · Missing VRAM telemetry stops CUDA proving\n'

mkdir -p "$work/bin"
touch "$work/cuda-ready"
printf '%s\n' "$header" "$gpu_cold" "$gpu_warm" > "$fixture/proof-rows"
cat > "$work/bin/rc1-cpu" <<'FIXTURE'
#!/usr/bin/env bash
set -eu
[[ "$1" == prepare ]]
printf 'prepare\n' >> "$STATION_FIXTURE_DIR/calls"
if [[ -f "$STATION_FIXTURE_DIR/prepare-free-mib" ]]; then
    cat "$STATION_FIXTURE_DIR/prepare-free-mib" > "$STATION_FIXTURE_DIR/free-mib"
fi
FIXTURE
cat > "$work/bin/rc1-gpu" <<'FIXTURE'
#!/usr/bin/env bash
set -eu
[[ "$1" == run && "$CUDA_VISIBLE_DEVICES" == "GPU-c0de0000-1111-2222-3333-444444444444" ]]
printf 'run\n' >> "$STATION_FIXTURE_DIR/calls"
cat "$STATION_FIXTURE_DIR/proof-rows"
FIXTURE
chmod +x "$work/bin/rc1-cpu" "$work/bin/rc1-gpu"

printf '3072\n' > "$fixture/free-mib"
result=$(main prove 0 2> "$fixture/prove.log")
[[ "$result" == *$'STATION_PROOF\t2\t0.250\t0.200\t0.009\t14\t2048\t1' ]]
[[ "$(< "$fixture/calls")" == $'prepare\nrun' ]]
printf 'PASS · Exactly 3072 MiB permits the CUDA proof job on the selected device\n'

: > "$fixture/calls"
printf '3071\n' > "$fixture/prepare-free-mib"
if blocked=$(main prove 0 2>&1); then
    fail "CUDA proving continued after available VRAM dropped below the minimum."
fi
[[ "$blocked" == *"This device has 3071 MiB free."* && "$(< "$fixture/calls")" == prepare ]]
printf 'PASS · The same GPU is checked again after preparation and low VRAM stops proving\n'
