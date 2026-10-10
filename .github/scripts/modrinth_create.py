"""Creates the MultiClicker project on Modrinth as a draft, with the texts of publishing/modrinth-curseforge.md,
the icon and the screenshots. Run by the "Create Modrinth project" workflow; needs MODRINTH_TOKEN."""
import json
import os
import re
import sys
import urllib.error
import urllib.parse
import urllib.request
import uuid

API = 'https://api.modrinth.com/v2'
TOKEN = os.environ['MODRINTH_TOKEN']
SLUG = os.environ.get('SLUG') or 'multiclicker'
USER_AGENT = 'Shamanalle/MultiClicker (github.com/Shamanalle/MultiClicker)'
SCREENSHOTS = [
    ('hud.png', 'Mob farm', 'The status panel and the highlighted target.', True),
    ('menu_clicker.png', 'Clicker settings', '', False),
    ('menu_automation.png', 'Automation modules', '', False),
    ('menu_survival.png', 'Survival modules', '', False),
    ('menu_visual.png', 'Visual modules', '', False),
    ('profiles.png', 'Presets and profiles', '', False),
    ('list_editor.png', 'Block list editor', '', False),
]


def request(method, url, body=None, content_type=None):
    headers = {'Authorization': TOKEN, 'User-Agent': USER_AGENT}
    if content_type:
        headers['Content-Type'] = content_type
    req = urllib.request.Request(url, data=body, method=method, headers=headers)
    try:
        with urllib.request.urlopen(req) as response:
            text = response.read().decode()
            return json.loads(text) if text else None
    except urllib.error.HTTPError as error:
        sys.exit(f'{method} {url} failed: {error.code} {error.read().decode()}')


def texts():
    doc = open('publishing/modrinth-curseforge.md', encoding='utf-8').read()
    summary = re.search(r'\*\*Summary, English\*\*.*?\n\n> (.+)\n', doc).group(1).strip()
    blocks = re.findall(r'````markdown\n(.*?)\n````', doc, re.S)
    body = blocks[0] + '\n\n<details>\n<summary>Описание на русском</summary>\n\n' + blocks[1] + '\n\n</details>\n'
    return summary, body


def main():
    summary, body = texts()
    data = {
        'slug': SLUG,
        'title': 'MultiClicker',
        'description': summary,
        'body': body,
        'project_type': 'mod',
        'categories': ['utility', 'game-mechanics'],
        'additional_categories': [],
        'client_side': 'required',
        'server_side': 'unsupported',
        'license_id': 'MIT',
        'source_url': 'https://github.com/Shamanalle/MultiClicker',
        'issues_url': 'https://github.com/Shamanalle/MultiClicker/issues',
        'is_draft': True,
        'initial_versions': [],
    }
    boundary = uuid.uuid4().hex
    icon = open('common/src/main/resources/assets/multiclicker/icon.png', 'rb').read()
    parts = [
        f'--{boundary}\r\nContent-Disposition: form-data; name="data"\r\n'
        f'Content-Type: application/json\r\n\r\n'.encode() + json.dumps(data).encode() + b'\r\n',
        f'--{boundary}\r\nContent-Disposition: form-data; name="icon"; filename="icon.png"\r\n'
        f'Content-Type: image/png\r\n\r\n'.encode() + icon + b'\r\n',
        f'--{boundary}--\r\n'.encode(),
    ]
    project = request('POST', f'{API}/project', b''.join(parts), f'multipart/form-data; boundary={boundary}')
    print(f"Created project {project['id']} ({project['slug']})")

    for file, title, description, featured in SCREENSHOTS:
        query = urllib.parse.urlencode({'ext': 'png', 'featured': str(featured).lower(), 'title': title,
                                        'description': description})
        image = open(f'docs/images/en/{file}', 'rb').read()
        request('POST', f"{API}/project/{project['id']}/gallery?{query}", image, 'image/png')
        print(f'Added {file}')

    print(f"Draft ready: https://modrinth.com/mod/{project['slug']}")
    print(f"Set the repository variable MODRINTH_ID to {project['id']}, then submit the project for review on the site.")


if __name__ == '__main__':
    main()
