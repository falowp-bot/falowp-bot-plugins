#!/bin/sh
set -eu
umask 077
root=$1
if [ -x "$root/ffmpeg" ]; then exit 0; fi
mkdir -p "$root"
stage=$(mktemp -d "$root/.download.XXXXXX")
trap 'rm -rf "$stage"' EXIT
case "$(dpkg --print-architecture)" in
  amd64) pkg=linux-x64; version=4.1.0; digest='Y5BWhGLU/WpQjOArNIgXD3z5mxxdV8c41C+U15nsE5yF8tVcdCGet5zPs5Zy3Ta6bU7haGpIzryutqCGQA/W8A==' ;;
  arm64) pkg=linux-arm64; version=4.1.4; digest='dljEqAOD0oIM6O6DxBW9US/FkvqvQwgJ2lGHOwHDDwu/pX8+V0YsDL1xqHbj1DMX/+nP9rxw7G7gcUvGspSoKg==' ;;
  *) exit 1 ;;
esac
curl -fsSL --proto '=https' --proto-redir '=https' --max-time 90 "https://registry.npmjs.org/@ffmpeg-installer/$pkg/-/$pkg-$version.tgz" -o "$stage/pkg"
test "$(openssl dgst -sha512 -binary "$stage/pkg" | base64 -w0)" = "$digest"
tar -xOzf "$stage/pkg" package/ffmpeg >"$stage/ffmpeg"
chmod 700 "$stage/ffmpeg"
mv "$stage/ffmpeg" "$root/ffmpeg"
