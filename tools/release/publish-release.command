#!/bin/bash
# Dvojklik ve Finderu: commitne všechny změny, vytvoří tag verze, pushne main + tag
# a založí GitHub Release s APK. Verze se bere z app/build.gradle.kts,
# poznámky z tools/release/notes/<verze>.md.
set -euo pipefail
cd "$(dirname "$0")/../.."

VERSION=$(sed -n 's/.*versionName = "\(.*\)".*/\1/p' app/build.gradle.kts)
APK="releases/app-release-$VERSION.apk"
NOTES="tools/release/notes/$VERSION.md"
REPO="profispojka/Heller-expense-tracker"

finish() { echo; read -r -p "Stiskni Enter pro zavření…" _; }
trap finish EXIT
fail() { echo; echo "CHYBA: $*"; exit 1; }

echo "== Vydání Heller $VERSION =="
[ "$(git rev-parse --abbrev-ref HEAD)" = "main" ] || fail "nejsi na větvi main."
[ -f "$APK" ] || fail "chybí $APK — nejdřív spusť build-release.command."
[ -f "$NOTES" ] || fail "chybí poznámky k vydání $NOTES."
git rev-parse -q --verify "refs/tags/$VERSION" >/dev/null && fail "tag $VERSION už existuje."
command -v gh >/dev/null || fail "chybí GitHub CLI (brew install gh)."
gh auth status >/dev/null 2>&1 || fail "gh není přihlášené (gh auth login)."

git fetch -q origin main
[ "$(git rev-list --count HEAD..origin/main)" = "0" ] || fail "origin/main má nové commity — nejdřív git pull."

echo
echo "Změny, které se commitnou:"
git status --short
echo
echo "Poznámky k vydání:"
sed 's/^/  /' "$NOTES"
echo
read -r -p "Commitnout, pushnout na main a vydat $VERSION na GitHubu? [a/N] " ok
[[ "$ok" =~ ^[aAyY]$ ]] || fail "zrušeno, nic se nestalo."

TITLE=$(head -n1 "$NOTES")
BODY=$(tail -n +2 "$NOTES")

git add -A
if ! git diff --cached --quiet; then
  git commit -q -F - <<MSG
feat: Release $VERSION — $TITLE
$BODY
MSG
fi
git tag -a "$VERSION" -m "Release $VERSION"
git push origin main
git push origin "$VERSION"

gh release create "$VERSION" "$APK" -R "$REPO" --title "$VERSION" --notes-file "$NOTES" --latest

echo
echo "HOTOVO: https://github.com/$REPO/releases/tag/$VERSION"
