#!/usr/bin/env bash
# Installs the CI / release automation that was staged (but not enabled) when
# this project was generated from the template. The staged files under
# template/ci/ already went through the init-time rename and placeholder
# substitution, so installing them later is a pure file move.
set -euo pipefail

cd "$(dirname "$0")/.."

if [ ! -d template/ci ]; then
  echo "Nothing to install: template/ci/ not found (CI may already be enabled)." >&2
  exit 1
fi

cp -R template/ci/. .

# The staging copy lost its executable bits: init_project.main.kts copies with
# File.copyTo, which drops the mode, and `cp -R` above faithfully preserves what
# it finds. Restoring them here rather than in the copy above keeps the one
# place that knows which staged files are scripts.
chmod +x scripts/setup_legal_sync.sh scripts/setup_sentry_ci.sh

rm -rf template
rm -- scripts/enable_ci.sh

echo "CI installed:"
echo "  - .github/workflows/ (ci, release-please, release, legal-sync, ...)"
echo "  - apps/ios fastlane files, legal/, release-please config"
echo
echo "Next: set the GitHub secrets listed in SETUP.md before the pipeline can ship."
echo "For the legal pages specifically, run ./scripts/setup_legal_sync.sh."
