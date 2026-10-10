"""Prints the release notes of a mod version: its section of CHANGELOG.md.

With --russian the section of CHANGELOG.ru.md follows the English one (for Modrinth and CurseForge, which
show a single changelog to everyone).

Usage: release_notes.py <mod version> [--russian]
"""
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


def section(changelog, version):
    lines = []
    found = False
    for line in (ROOT / changelog).read_text(encoding='utf-8').splitlines():
        if line.startswith('## ['):
            if found:
                break
            found = line.startswith(f'## [{version}]')
            continue
        if found:
            lines.append(line)
    if not found:
        sys.exit(f'{changelog} has no section for {version}')
    return '\n'.join(lines).strip()


def main(args):
    if not args or args[0].startswith('-') or any(arg != '--russian' for arg in args[1:]):
        sys.exit(__doc__)
    notes = section('CHANGELOG.md', args[0])
    if '--russian' in args[1:]:
        notes += '\n\n---\n\n**Русский**\n\n' + section('CHANGELOG.ru.md', args[0])
    print(notes)


if __name__ == '__main__':
    main(sys.argv[1:])
