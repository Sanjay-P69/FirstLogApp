"""Compare two independently executed clean, unsigned release APK builds.

No signing credentials are inherited by the build subprocesses. A successful
comparison proves this checkout/environment only, not cross-platform identity.
"""
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess


def main():
    root = Path(__file__).resolve().parent.parent
    os.chdir(root)
    output = root / 'reproducibility-output'
    output.mkdir(exist_ok=True)
    # A failed rerun must not leave a previous successful report visible.
    (output / 'result.json').unlink(missing_ok=True)
    env = {key: value for key, value in os.environ.items()
           if not key.startswith('ANDROID_KEY')
           and key not in {'FATHER_ACCOUNT_GH', 'GH_TOKEN', 'GITHUB_TOKEN'}}
    env.update(TZ='UTC', LANG='en_US.UTF-8', LC_ALL='en_US.UTF-8')
    wrapper = ['cmd.exe', '/c', 'gradlew.bat'] if os.name == 'nt' else ['bash', 'gradlew']
    hashes = []
    for index in (1, 2):
        subprocess.run(wrapper + ['clean', ':app:assembleRelease', '--no-build-cache',
                       '--no-configuration-cache', '--rerun-tasks', '--stacktrace'],
                       env=env, check=True)
        apk = root / 'app/build/outputs/apk/release/app-release-unsigned.apk'
        destination = output / f'release-{index}-unsigned.apk'
        shutil.copyfile(apk, destination)
        hashes.append(hashlib.sha256(destination.read_bytes()).hexdigest())
    report = {'commit': subprocess.check_output(['git', 'rev-parse', 'HEAD'], text=True).strip(),
              'dirty': bool(subprocess.check_output(['git', 'status', '--porcelain'], text=True).strip()),
              'sha256': hashes, 'match': hashes[0] == hashes[1],
              'scope': 'two clean unsigned builds, same checkout and toolchain; no cross-host proof'}
    (output / 'result.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(report, indent=2))
    if not report['match']:
        raise SystemExit('APK bytes differ. Inspect both APKs; reproducibility FAILED.')


if __name__ == '__main__':
    main()
