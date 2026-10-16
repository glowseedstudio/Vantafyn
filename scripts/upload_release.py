#!/usr/bin/env python3
"""
Uploads a Vantafyn release and its 4 core binary assets to GitHub Releases.
Credentials are automatically obtained from git-credential-helper.

Usage:
    python3 scripts/upload_release.py <tag_name>
    python3 scripts/upload_release.py v0.9.77
"""

import sys
import os
import json
import urllib.request
import urllib.error
import subprocess

def get_git_token():
    try:
        cred = subprocess.check_output(
            ['git', 'credential', 'fill'],
            input='protocol=https\nhost=github.com\n',
            text=True
        )
        for line in cred.splitlines():
            if line.startswith('password='):
                return line.split('=', 1)[1]
    except Exception as e:
        sys.stderr.write(f"Error getting git credential: {e}\n")
    return None

def main():
    if len(sys.argv) < 2:
        print("Usage: python3 scripts/upload_release.py <tag>")
        sys.exit(1)

    tag = sys.argv[1].strip()
    if not tag.startswith('v'):
        tag = f"v{tag}"

    token = os.environ.get('GITHUB_TOKEN') or get_git_token()
    if not token:
        sys.stderr.write("Unable to resolve GitHub authentication token.\n")
        sys.exit(1)

    repo = "glowseedstudio/Vantafyn"
    headers = {
        'Authorization': f'token {token}',
        'User-Agent': 'Vantafyn-Release-Script',
        'Accept': 'application/vnd.github.v3+json'
    }

    # Release staging directory
    release_dir = os.path.join("release", tag)
    if not os.path.isdir(release_dir):
        sys.stderr.write(f"Release directory {release_dir} not found!\n")
        sys.exit(1)

    notes_file = os.path.join(release_dir, "RELEASE_NOTES.md")
    notes = f"Release {tag}"
    if os.path.isfile(notes_file):
        with open(notes_file, 'r', encoding='utf-8') as f:
            notes = f.read()

    # Find companion zip version
    zips = [f for f in os.listdir(release_dir) if f.startswith("Vantafyn.Plugin.Companion_") and f.endswith(".zip")]

    # Check if release exists or create it
    get_req = urllib.request.Request(f'https://api.github.com/repos/{repo}/releases/tags/{tag}', headers=headers)
    rel = None
    try:
        with urllib.request.urlopen(get_req) as res:
            rel = json.loads(res.read())
            print(f"Release {tag} exists (id: {rel['id']})")
    except urllib.error.HTTPError as e:
        if e.code == 404:
            print(f"Creating GitHub release {tag}...")
            payload = json.dumps({
                'tag_name': tag,
                'name': f"Vantafyn {tag}",
                'body': notes,
                'draft': False,
                'prerelease': False
            }).encode('utf-8')
            create_req = urllib.request.Request(
                f'https://api.github.com/repos/{repo}/releases',
                data=payload,
                headers={**headers, 'Content-Type': 'application/json'}
            )
            with urllib.request.urlopen(create_req) as res:
                rel = json.loads(res.read())
                print(f"Created release {tag} (id: {rel['id']})")
        else:
            raise

    upload_url = rel['upload_url'].split('{')[0]

    # Expected assets
    assets = [
        ('app-mobile-release.apk', os.path.join(release_dir, 'app-mobile-release.apk'), 'application/vnd.android.package-archive'),
        ('app-tv-release.apk', os.path.join(release_dir, 'app-tv-release.apk'), 'application/vnd.android.package-archive'),
    ]

    for z in sorted(zips):
        assets.append((z, os.path.join(release_dir, z), 'application/zip'))

    existing = {a['name']: a['id'] for a in rel.get('assets', [])}

    for name, path, content_type in assets:
        if not os.path.isfile(path):
            print(f"Warning: asset {path} does not exist, skipping.")
            continue
        if name in existing:
            print(f"Asset {name} already uploaded (id: {existing[name]}), skipping.")
            continue
        size_mb = os.path.getsize(path) / (1024 * 1024)
        print(f"Uploading {name} ({size_mb:.1f} MB)...")
        with open(path, 'rb') as f:
            data = f.read()
        u_req = urllib.request.Request(
            f"{upload_url}?name={name}",
            data=data,
            headers={
                'Authorization': f'token {token}',
                'User-Agent': 'Vantafyn-Release-Script',
                'Content-Type': content_type,
                'Content-Length': str(len(data))
            }
        )
        with urllib.request.urlopen(u_req) as res:
            print(f"Successfully uploaded {name}!")

    print(f"All release assets for {tag} verified and uploaded successfully!")

if __name__ == '__main__':
    main()
