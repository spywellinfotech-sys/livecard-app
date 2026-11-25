#!/usr/bin/env bash
# Fail the build if <apk> is not signed with <keystore> (alias androiddebugkey, pass android).
# Usage: tools/check_signing.sh <apk> <keystore>
set -euo pipefail
apk="$1"; ks="$2"
BT=$(ls -d "$ANDROID_HOME"/build-tools/* | sort -V | tail -1)
got=$("$BT/apksigner" verify --print-certs "$apk" | grep -m1 "SHA-256 digest" | awk '{print $NF}' | tr 'A-F' 'a-f')
want=$(keytool -list -v -keystore "$ks" -storepass android -alias androiddebugkey | grep -m1 "SHA256:" | awk '{print $2}' | tr -d ':' | tr 'A-F' 'a-f')
echo "APK cert:      $got"
echo "Expected cert: $want"
if [ "$got" != "$want" ]; then
  echo "::error::APK is not signed with the app's fixed key ($ks)"
  exit 1
fi
echo "Signing OK"
