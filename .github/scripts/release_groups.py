"""Picks the jars a release publishes, one per loader and group of Minecraft versions.

Versions listed together in versions/shared-jars.txt are released as one jar, the one built for the newest of
them, which declares the whole range. That is only safe when building the mod for each version of the group
gives the same code: then every reference in the shared jar was resolved against each of those versions and its
mixins target the same members there. So the jars built for the other versions of the group are compared with
it, leaving out the mod metadata (which names the Minecraft versions) and the manifest.

Usage:
  release_groups.py check <jar folder> [<release folder>]
      Compares the jars of every group and checks that each expected release jar is there; with a release
      folder, copies the jars to publish into it.
  release_groups.py entries
      Prints the files to publish as JSON, one entry per loader and group, for the store upload matrix.
"""
import hashlib
import json
import shutil
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
VERSIONS = ROOT / 'versions'

# Entries of a jar that name the Minecraft versions or differ between builds for another reason.
METADATA = {
    'META-INF/MANIFEST.MF',
    'fabric.mod.json',
    'META-INF/mods.toml',
    'META-INF/neoforge.mods.toml',
}


def version_key(version):
    return [int(part) for part in version.split('.')]


def properties(version):
    values = {}
    for line in (VERSIONS / f'{version}.properties').read_text().splitlines():
        if '=' in line and not line.lstrip().startswith('#'):
            key, value = line.split('=', 1)
            values[key.strip()] = value.strip()
    return values


def loader_versions():
    """Minecraft versions each loader has a jar for, oldest first."""
    versions = sorted((path.stem for path in VERSIONS.glob('*.properties')), key=version_key)
    result = {'fabric': versions, 'neoforge': [], 'forge': []}
    for version in versions:
        props = properties(version)
        if 'neoforge_version' in props:
            result['neoforge'].append(version)
        if 'forge_version' in props:
            result['forge'].append(version)
    # The NeoForge jar for 1.20.1 runs on Forge 1.20.1 too and is published under both names.
    result['forge'].append('1.20.1')
    return result


def groups():
    """Groups of versions/shared-jars.txt per loader."""
    result = {'fabric': [], 'neoforge': [], 'forge': []}
    for line in (VERSIONS / 'shared-jars.txt').read_text().splitlines():
        line = line.strip()
        if not line or line.startswith('#'):
            continue
        loader, versions = (part.strip() for part in line.split(':', 1))
        result[loader].append(versions.split())
    return result


def release_entries():
    """(loader, versions) of every jar a release publishes."""
    shared = groups()
    entries = []
    for loader, versions in loader_versions().items():
        grouped = {version: group for group in shared[loader] for version in group}
        for group in shared[loader]:
            missing = [version for version in group if version not in versions]
            if missing:
                sys.exit(f'versions/shared-jars.txt: {loader} has no jar for {", ".join(missing)}')
        for version in versions:
            group = grouped.get(version, [version])
            if version == group[-1]:
                entries.append((loader, group))
    return entries


def label(versions):
    return versions[0] if len(versions) == 1 else f'{versions[0]}-{versions[-1]}'


def jar(folder, loader, version_label):
    found = sorted(Path(folder).glob(f'MultiClicker-{loader}-*+{version_label}.jar'))
    if len(found) != 1:
        sys.exit(f'expected one {loader} jar for {version_label} in {folder}, found {len(found)}')
    return found[0]


def contents(path):
    with zipfile.ZipFile(path) as archive:
        return {
            name: hashlib.sha256(archive.read(name)).hexdigest()
            for name in archive.namelist()
            if not name.endswith('/') and name not in METADATA
        }


def check(folder, release=None):
    same = True
    for loader, group in groups().items():
        for versions in group:
            shared = jar(folder, loader, label(versions))
            reference = contents(shared)
            group_same = True
            for version in versions[:-1]:
                other = jar(folder, loader, version)
                differing = sorted(name for name in reference.keys() | other.keys()
                                   if reference.get(name) != other.get(name))
                if differing:
                    group_same = False
                    print(f'{loader} {version}: {other.name} differs from {shared.name} in {len(differing)} files:')
                    for name in differing[:20]:
                        print(f'  {name}')
            if group_same:
                print(f'{loader} {label(versions)}: the same code for {", ".join(versions)}')
            same = same and group_same
    if not same:
        sys.exit('Some versions of versions/shared-jars.txt do not build into the same code; '
                 'release them on their own')
    entries = release_entries()
    jars = [jar(folder, loader, label(versions)) for loader, versions in entries]
    if release:
        Path(release).mkdir(parents=True, exist_ok=True)
        for path in jars:
            shutil.copy2(path, release)
    for path in jars:
        print(f'release: {path.name}')


def entries():
    print(json.dumps([
        {'loader': loader, 'label': label(versions), 'versions': ' '.join(versions), 'build': versions[-1]}
        for loader, versions in release_entries()
    ]))


if __name__ == '__main__':
    if len(sys.argv) >= 3 and sys.argv[1] == 'check':
        check(sys.argv[2], sys.argv[3] if len(sys.argv) > 3 else None)
    elif len(sys.argv) == 2 and sys.argv[1] == 'entries':
        entries()
    else:
        sys.exit(__doc__)
