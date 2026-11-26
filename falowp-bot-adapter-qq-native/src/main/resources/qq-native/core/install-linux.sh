#!/bin/sh
set -eu
umask 077
root=$1
arch=$(dpkg --print-architecture)
case "$arch" in
    amd64) digest=436c65e1dd28422e128a21cbddb5fe4e2e6c079d7f27fec2541e014d38cc336e ;;
    arm64) digest=181b895cb573548e396e54bb0548cca92ffdc1b1a4b018fa4551bb062385fdca ;;
    *) echo "Unsupported Linux architecture: $arch" >&2; exit 1 ;;
esac
target="$root/client-53644-$arch"
if [ -x "$target/opt/QQ/qq" ]; then exit 0; fi
stage=$(mktemp -d "$root/.install.XXXXXXXX")
trap 'rm -rf "$stage"' EXIT
pkg="linuxqq_3.2.34-53644_${arch}.deb"
curl --fail --silent --show-error --location --proto '=https' --proto-redir '=https' --connect-timeout 20 --max-time 900 --retry 2 -o "$stage/client.deb" "https://qqdl.gtimg.cn/qqfile/QQNT/9.9.36/beta/9ee04bef/$pkg"
(cd "$stage" && echo "$digest  client.deb" | sha256sum -c -)
test "$(dpkg-deb -f "$stage/client.deb" Architecture)" = "$arch"
dpkg-deb -x "$stage/client.deb" "$stage/unpacked"
test -x "$stage/unpacked/opt/QQ/qq"
mv -T "$stage/unpacked" "$target"
