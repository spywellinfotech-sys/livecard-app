"""Make a generated Flutter Android project sign with a fixed keystore.

Usage: python3 tools/pin_signing.py <android_dir> <keystore_path>

The keystore is a debug-style key (alias androiddebugkey, passwords 'android').
Pinning it in build.gradle(.kts) makes every APK of an app carry the same
signature, so new versions install over old ones.
"""
import os
import sys

android_dir, keystore = sys.argv[1], os.path.abspath(sys.argv[2])
app = os.path.join(android_dir, "app")
kts = os.path.join(app, "build.gradle.kts")
groovy = os.path.join(app, "build.gradle")

if os.path.exists(kts):
    path = kts
    block = f'''
    signingConfigs {{
        getByName("debug") {{
            storeFile = file("{keystore}")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }}
    }}
'''
elif os.path.exists(groovy):
    path = groovy
    block = f'''
    signingConfigs {{
        debug {{
            storeFile file('{keystore}')
            storePassword 'android'
            keyAlias 'androiddebugkey'
            keyPassword 'android'
        }}
    }}
'''
else:
    sys.exit(f"no build.gradle(.kts) in {app}")

src = open(path).read()
if keystore in src:
    print("already pinned")
    sys.exit(0)
i = src.index("android {") + len("android {")
src = src[:i] + block + src[i:]
open(path, "w").write(src)
print(f"pinned {os.path.basename(path)} to {keystore}")
