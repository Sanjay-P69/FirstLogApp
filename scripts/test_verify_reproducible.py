"""Exercise comparison failures and secret isolation without an Android SDK."""
import importlib.util
from contextlib import redirect_stdout
import io
import os
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location(
    'verify', Path(__file__).with_name('verify_reproducible.py'))
verify = importlib.util.module_from_spec(spec)
spec.loader.exec_module(verify)


class ReproducibilityTest(unittest.TestCase):
    def run_comparison(self, payloads, fail=False):
        original = Path.cwd()
        scratch = Path(__file__).resolve().parent.parent / 'reproducibility-output'
        scratch.mkdir(exist_ok=True)
        with tempfile.TemporaryDirectory(dir=scratch) as directory:
            root = Path(directory)
            (root / 'scripts').mkdir()
            calls = []

            def build(command, env, check):
                self.assertTrue(check)
                self.assertIn('clean', command)
                self.assertIn('--no-build-cache', command)
                self.assertIn('--rerun-tasks', command)
                for key in ('ANDROID_KEYSTORE_PASSWORD', 'ANDROID_KEYSTORE_PATH',
                            'FATHER_ACCOUNT_GH', 'GH_TOKEN', 'GITHUB_TOKEN'):
                    self.assertNotIn(key, env)
                calls.append(command)
                if fail:
                    raise subprocess.CalledProcessError(1, command)
                apk = root / 'app/build/outputs/apk/release/app-release-unsigned.apk'
                apk.parent.mkdir(parents=True, exist_ok=True)
                apk.write_bytes(payloads[len(calls) - 1])

            try:
                with patch.object(verify, '__file__', str(root / 'scripts/verify.py')), \
                     patch.object(verify.subprocess, 'run', side_effect=build), \
                     patch.object(verify.subprocess, 'check_output', side_effect=['abc\n', '']), \
                     patch.dict(os.environ, {'ANDROID_KEYSTORE_PASSWORD': 'test',
                                'ANDROID_KEYSTORE_PATH': 'test', 'FATHER_ACCOUNT_GH': 'test',
                                'GH_TOKEN': 'test', 'GITHUB_TOKEN': 'test'}):
                    with redirect_stdout(io.StringIO()):
                        verify.main()
                self.assertEqual(len(calls), 2)
                self.assertTrue((root / 'reproducibility-output/result.json').exists())
            finally:
                os.chdir(original)

    def test_matching_builds_and_secret_isolation(self):
        self.run_comparison([b'same', b'same'])

    def test_mismatch_fails(self):
        with self.assertRaisesRegex(SystemExit, 'APK bytes differ'):
            self.run_comparison([b'first', b'second'])

    def test_failed_build_never_reports_success(self):
        with self.assertRaises(subprocess.CalledProcessError):
            self.run_comparison([], fail=True)


if __name__ == '__main__':
    unittest.main()
