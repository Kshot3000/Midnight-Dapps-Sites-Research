#!/usr/bin/env bash
set -euo pipefail

app=$(cd "$(dirname "$0")/.." && pwd)
source "$app/gpu-lab.sh"

header=$'mode\trep\tk\tthreads\tstart_load1\tproof\tstate\tprove_s\tverify_s\tverified\tmsm_calls\tntt_calls\tquotient_calls\tdevice_errors\tfallbacks\tdeclines\thook_declines\th2d_copies\th2d_bytes\td2h_copies\td2h_bytes\tmsm_d2h_copies\tmsm_d2h_bytes\tstage\tpeak_device_bytes\tgpu_model\tdevice_total_bytes'
cold=$'gpu\t1\t16\t8\t0.20\t0\tcold\t0.250\t0.010\ttrue\t2\t4\t1\t0\t0\t[]\t[]\t0\t0\t0\t0\t0\t0\t[]\t2048\tfixture\t8192'
warm=$'gpu\t1\t16\t8\t0.20\t1\twarm\t0.200\t0.009\ttrue\t2\t4\t1\t0\t1\t[]\t[]\t0\t0\t0\t0\t0\t0\t[]\t1024\tfixture\t8192'
expected=$'STATION_PROOF\t2\t0.250\t0.200\t0.009\t14\t2048\t1'

check_rows() {
    printf '%s\n' "$header" "$1" "$2" | verify_proof_rows
}

reject() {
    if check_rows "$1" "$2"; then
        printf 'Expected a refused %s fixture\n' "$3" >&2
        exit 1
    fi
    printf 'PASS · Refused %s fixture\n' "$3"
}

[[ "$(check_rows "$cold" "$warm")" == "$expected" ]]
printf 'PASS · Verified fixture reports CUDA work, verification time, memory and fallbacks\n'
reject "$cold" "${warm/gpu/cpu}" "CPU-only"
reject "$cold" "${warm/$'\ttrue\t'/$'\tfalse\t'}" unverified
reject "$cold" "${warm/$'\t2\t4\t1\t0\t1\t'/$'\t0\t0\t0\t0\t1\t'}" "zero-device-work"
reject "$cold" "${warm/$'\t2\t4\t1\t0\t1\t'/$'\t2\t4\t1\t1\t1\t'}" "device-error"
reject "$cold" "$cold" "duplicate cold row"
