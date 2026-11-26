#!/bin/sh
# Run on Linux with N-API headers and the compiler(s) for the desired targets.
set -eu
src=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
out="$src/../resources/qq-native/core/native"
mkdir -p "$out"
for arch in "$@"; do
    case "$arch" in
        arm64) compiler=${ARM64_CC:-aarch64-linux-gnu-gcc} ;;
        x64) compiler=${X64_CC:-x86_64-linux-gnu-gcc} ;;
        *) echo "Expected arm64 or x64" >&2; exit 1 ;;
    esac
    "$compiler" -shared -fPIC -O2 -Wall -Wextra -Werror -I"${NODE_INCLUDE:-/usr/include/node}" \
        "$src/packet.c" -o "$out/packet-linux-$arch.node" -ldl
 done
