#!/bin/sh
set -eu
umask 077
root=$1
scripts=$2
sh "$scripts/install-linux.sh" "$root"
qq="$root/client-53644-$(dpkg --print-architecture)/opt/QQ"
export HOME="$root/account"
export XDG_CONFIG_HOME="$HOME/.config"
export XDG_CACHE_HOME="$HOME/.cache"
export XDG_DATA_HOME="$HOME/.local/share"
export FALOWP_QQ_APP="$qq/resources/app"
mkdir -p "$HOME"
host=$(mktemp -d "$root/.host.XXXXXXXX")
trap 'rm -rf "$host"' EXIT
mkdir -p "$host/resources/app"
# A separate Electron application directory keeps the vendor installation intact.
for item in "$qq"/*; do
    case "$item" in
        */resources) ;;
        */qq) ln "$item" "$host/qq" ;;
        *) ln -s "$item" "$host/" ;;
    esac
done
printf '%s\n' '{"name":"falowp-qq-native","version":"1.0.0","main":"worker.cjs"}' > "$host/resources/app/package.json"
cp "$scripts/"*.cjs "$host/resources/app/"
cp -R "$scripts/native" "$host/resources/app/native"
export FALOWP_QQ_MEDIA_INSTALL="$scripts/media-tools.sh"
export FALOWP_QQ_TOOLS="$root/tools"
cd "$FALOWP_QQ_APP"
# The headless worker creates no browser windows. Electron requires this flag for container root.
set -- --disable-gpu
if [ "$(id -u)" = 0 ]; then set -- "$@" --no-sandbox; fi
LD_LIBRARY_PATH="$qq:$FALOWP_QQ_APP" xvfb-run -a "$host/qq" "$@"
