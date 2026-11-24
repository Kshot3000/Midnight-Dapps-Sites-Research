#!/usr/bin/env bash
set -euo pipefail

upstream=https://github.com/CharlesHoskinson/midnight-zk.git
revision=6bcc6a56e85e584357a67b3bd8a15fb8fda4e2e8
readonly minimum_cuda_free_mib=3072

fail() {
    printf '%s\n' "$*" >&2
    exit 1
}

require_wsl_cuda() {
    [[ -e /dev/dxg ]] || fail "WSL2 GPU passthrough is unavailable. Check WSL2 and your Windows NVIDIA driver."
    export PATH="/usr/lib/wsl/lib:$PATH"
    command -v nvidia-smi >/dev/null || fail "nvidia-smi is unavailable in WSL2."
}

read_cuda_device() {
    local selector="$1" gpu uuid name capability free_memory total_memory driver
    gpu=$(nvidia-smi -i "$selector" --query-gpu=uuid,name,compute_cap,memory.free,memory.total,driver_version \
        --format=csv,noheader,nounits) || fail "NVIDIA device $selector is unavailable in WSL2."
    [[ "$gpu" != *$'\n'* && "$gpu" != *$'\r'* && "$gpu" != *$'\t'* ]] || fail "Select one NVIDIA device."
    IFS=',' read -r uuid name capability free_memory total_memory driver <<< "$gpu"
    capability=${capability# }
    free_memory=${free_memory# }
    total_memory=${total_memory# }
    [[ "$uuid" =~ ^GPU-[a-fA-F0-9-]+$ && "$capability" =~ ^[0-9]+\.[0-9]+$ && -n "${name# }" && -n "${driver# }" ]] ||
        fail "The NVIDIA driver did not report a usable CUDA device."
    [[ "$free_memory" =~ ^[0-9]{1,9}$ && "$total_memory" =~ ^[0-9]{1,9}$ ]] ||
        fail "The NVIDIA driver did not report free and total VRAM in MiB."
    free_memory=$((10#$free_memory))
    total_memory=$((10#$total_memory))
    (( total_memory > 0 && free_memory <= total_memory )) ||
        fail "The NVIDIA driver reported inconsistent VRAM totals."
    printf '%s\t%s\t%s\t%s\t%s\t%s\n' "$uuid" "${name# }" "${capability/./}" "$free_memory" "$total_memory" "${driver# }"
}

report_cuda_device() {
    local device="$1"
    # The UUID selects the device in WSL. The dashboard only displays the remaining fields.
    printf 'STATION_GPU\t%s\t%s\n' "${device#*$'\t'}" "$minimum_cuda_free_mib"
}

require_cuda_memory() {
    local free_memory="$1"
    (( free_memory >= minimum_cuda_free_mib )) ||
        fail "CUDA proofs need at least $minimum_cuda_free_mib MiB of free VRAM. This device has $free_memory MiB free. Close a GPU workload or select CPU."
}

verify_proof_rows() {
    local mode="$1"
    awk -F '\t' -v mode="$mode" '
        NR == 1 {
            if ($1 != "mode" || $10 != "verified" || $11 != "msm_calls" ||
                $14 != "device_errors" || $25 != "peak_device_bytes") bad = 1
            next
        }
        {
            device_calls = $11 + $12 + $13
            if (NF != 27 || $1 != mode || $3 != "16" || $10 != "true" || $14 != "0" || $5 == "" ||
                $11 !~ /^[0-9]+$/ || $12 !~ /^[0-9]+$/ || $13 !~ /^[0-9]+$/ ||
                $15 !~ /^[0-9]+$/ || $25 !~ /^[0-9]+$/ ||
                $8 !~ /^[0-9]+[.][0-9]+$/ || $9 !~ /^[0-9]+[.][0-9]+$/) bad = 1
            if ((mode == "gpu" && device_calls <= 0) ||
                (mode == "cpu" && (device_calls != 0 || $15 != "0" || $25 != "0"))) bad = 1
            calls += device_calls
            if ($25 > peak) peak = $25
            fallbacks += $15
            if ($6 == "0" && $7 == "cold") { cold = $8; cold_count++ }
            if ($6 == "1" && $7 == "warm") { warm = $8; verify = $9; warm_count++ }
        }
        END {
            if (bad || NR != 3 || cold_count != 1 || warm_count != 1) exit 1
            printf "STATION_PROOF\t2\t%s\t%s\t%s\t%.0f\t%.0f\t%.0f\n", cold, warm, verify, calls, peak, fallbacks
        }
    '
}

main() {
    local action="${1:-}" gpu_index="${2:-0}" mode=gpu
    case "$action" in
        detect|build|prove) ;;
        build-cpu|prove-cpu) mode=cpu; action=${action%-cpu} ;;
        *) fail "Choose detect, build, prove, build-cpu, or prove-cpu." ;;
    esac

    export PATH="$HOME/.cargo/bin:$PATH"
    local root="${XDG_DATA_HOME:-$HOME/.local/share}/midnight-proof-station/$revision"
    local source="$root/upstream" work="$root/cpu" arch gpu uuid free_memory
    if [[ "$mode" == gpu ]]; then
        [[ "$gpu_index" =~ ^[0-9]+$ ]] || fail "Select a non-negative NVIDIA device index."
        require_wsl_cuda
        gpu=$(read_cuda_device "$gpu_index") || return 1
        IFS=$'\t' read -r uuid _ arch free_memory _ _ <<< "$gpu"
        report_cuda_device "$gpu"
        [[ "$action" != detect ]] || return 0
        if [[ "$action" == prove ]]; then require_cuda_memory "$free_memory"; fi
        work="$root/sm-$arch"
        export CUDA_VISIBLE_DEVICES="$uuid" MIDNIGHT_GPU_ARCHS="$arch" MIDNIGHT_GPU_REQUIRE_CUDA=1 MIDNIGHT_GPU_MIN_K=14
    fi

    mkdir -p "$work"
    exec 9>"$root/operation.lock"
    flock -n 9 || fail "Another Proof Lab operation is running."

    export RC1_WORK_DIR="$work" CARGO_TARGET_DIR="$work/target"
    export RC1_CACHE="$work/key-cache" RC1_K=16 RC1_WARM=1 RC1_REP=1
    export RC1_MAX_LOAD=1000000 RAYON_NUM_THREADS=8
    export RC1_STOCK_BIN="$work/bin/rc1-cpu"
    local label=CPU proof_bin="$RC1_STOCK_BIN" ready="$work/cpu-ready"
    if [[ "$mode" == gpu ]]; then
        label=CUDA
        proof_bin="$work/bin/rc1-gpu"
        ready="$work/cuda-ready"
    fi

    if [[ "$action" == build ]]; then
        command -v git >/dev/null || fail "Install Git in the default WSL2 distribution."
        command -v cargo >/dev/null || fail "Install Rust with rustup in the default WSL2 distribution."
        if [[ "$mode" == gpu ]]; then
            local cuda_root="${CUDA_PATH:-${CUDA_HOME:-}}"
            if [[ -n "$cuda_root" ]]; then
                [[ -x "$cuda_root/bin/nvcc" ]] || fail "The configured CUDA toolkit does not contain nvcc."
            else
                command -v nvcc >/dev/null || fail "Install the CUDA toolkit (nvcc) inside WSL2."
            fi
        fi

        if [[ ! -d "$source/.git" ]]; then
            git init --quiet "$source"
        fi
        if ! git -C "$source" cat-file -e "$revision^{commit}" 2>/dev/null; then
            GIT_TERMINAL_PROMPT=0 git -C "$source" fetch --no-tags --depth=1 "$upstream" "$revision"
        fi
        if ! git -C "$source" rev-parse --verify HEAD >/dev/null 2>&1; then
            git -C "$source" checkout --quiet --detach "$revision"
        fi
        [[ "$(git -C "$source" rev-parse HEAD)" == "$revision" &&
            -z "$(git -C "$source" status --porcelain)" ]] ||
            fail "The managed upstream checkout has local changes. Use a clean checkout before building."

        rm -f -- "$ready"
        cd "$source"
        printf 'Building the pinned %s prover. The first build downloads Rust dependencies.\n' "$label"
        if [[ "$mode" == cpu ]]; then
            export CARGO_BUILD_JOBS=8
            cargo build --release --locked --no-default-features -p midnight-gpu-prover-bench --bin rc1-bench
            mkdir -p "$work/bin"
            install -m755 "$CARGO_TARGET_DIR/release/rc1-bench" "$RC1_STOCK_BIN"
        else
            printf 'Target NVIDIA architecture: sm_%s.\n' "$arch"
            cargo fetch --locked
            bash bench/gpu-prover/rc1/scripts/build.sh
        fi
        [[ -x "$RC1_STOCK_BIN" && -x "$proof_bin" ]] || fail "The build did not produce the expected proof binaries."
        touch "$ready"
        printf 'STATION_READY\t%s\n' "$revision"
        return
    fi

    [[ -f "$ready" && -x "$RC1_STOCK_BIN" && -x "$proof_bin" ]] ||
        fail "Build the $label prover in the Proof Lab before generating proofs."
    local rows
    printf 'Preparing the synthetic test circuit and checking the proofs with the stock CPU verifier.\n' >&2
    timeout --kill-after=5s 240s "$RC1_STOCK_BIN" prepare
    if [[ "$mode" == gpu ]]; then
        # Measure the same device again just before proving. Another workload can start during preparation.
        gpu=$(read_cuda_device "$uuid") || return 1
        IFS=$'\t' read -r _ _ _ free_memory _ _ <<< "$gpu"
        report_cuda_device "$gpu"
        require_cuda_memory "$free_memory"
    fi
    rows=$(timeout --kill-after=5s 240s "$proof_bin" run)
    printf '%s\n' "$rows" | verify_proof_rows "$mode" ||
        fail "Both proofs must verify with the stock verifier and report the expected $label execution."
}

if [[ "${BASH_SOURCE[0]:-$0}" == "$0" ]]; then
    main "$@" </dev/null
fi
