"""Downloads the game assets of a Minecraft version into Loom's cache before Gradle runs.

Mojang's download servers now and then fail the same file for Loom several times in a row, which fails the
whole start-up check. This fetches every missing file on its own, checks its hash and tries again after a pause,
so Loom finds everything in place and has nothing left to download.
"""
import hashlib
import json
import os
import sys
import time
import urllib.request
from concurrent.futures import ThreadPoolExecutor

MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
RESOURCES = "https://resources.download.minecraft.net"
OBJECTS = os.path.expanduser("~/.gradle/caches/fabric-loom/assets/objects")


def fetch(url, sha1=None, attempts=5):
    for attempt in range(1, attempts + 1):
        try:
            with urllib.request.urlopen(url, timeout=60) as response:
                data = response.read()
            if sha1 is None or hashlib.sha1(data).hexdigest() == sha1:
                return data
            error = "hash mismatch"
        except Exception as e:
            error = e
        print(f"attempt {attempt} for {url} failed: {error}", flush=True)
        time.sleep(5 * attempt)
    raise RuntimeError(f"could not download {url}")


def fetch_object(sha1):
    path = os.path.join(OBJECTS, sha1[:2], sha1)
    if os.path.exists(path):
        return False
    data = fetch(f"{RESOURCES}/{sha1[:2]}/{sha1}", sha1)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    temp = f"{path}.{os.getpid()}.part"
    with open(temp, "wb") as out:
        out.write(data)
    os.replace(temp, path)
    return True


def main(version):
    versions = {v["id"]: v for v in json.loads(fetch(MANIFEST))["versions"]}
    details = json.loads(fetch(versions[version]["url"], versions[version]["sha1"]))
    index = details["assetIndex"]
    objects = json.loads(fetch(index["url"], index["sha1"]))["objects"]
    hashes = sorted({o["hash"] for o in objects.values()})
    with ThreadPoolExecutor(16) as pool:
        fetched = sum(pool.map(fetch_object, hashes))
    print(f"Minecraft {version}: {len(hashes)} asset files, {fetched} downloaded")


if __name__ == "__main__":
    main(sys.argv[1])
