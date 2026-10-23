#!/usr/bin/env bash
set -euo pipefail

if (( $# != 6 )); then
  echo "usage: $0 VERSION VERSION_CODE TAG COMMIT_SHA CHANNEL OUTPUT_DIR" >&2
  exit 2
fi

version=$1
version_code=$2
tag=$3
commit_sha=$4
channel=$5
output_dir=$6
repo_root=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)
cd "$repo_root"

: "${GITHUB_REPOSITORY:?GITHUB_REPOSITORY is required}"
: "${GH_TOKEN:?GH_TOKEN is required}"

mkdir -p "$output_dir"
associated="$output_dir/associated-prs.json"
source_json="$output_dir/source.json"
body_file="$output_dir/pr-body.md"
changelog_file="$output_dir/changelog.md"
releases_json="$output_dir/releases.json"

query_associated_prs() {
  local sha=$1
  gh api \
    -H "Accept: application/vnd.github+json" \
    -H "X-GitHub-Api-Version: 2022-11-28" \
    "repos/${GITHUB_REPOSITORY}/commits/${sha}/pulls"
}

query_associated_prs "$commit_sha" > "$associated"
pr_count=$(jq '[.[] | select(.merged_at != null)] | length' "$associated")

if (( pr_count == 0 )); then
  subject=$(git show -s --format=%s "$commit_sha")
  if [[ "$subject" == Bump\ web* ]]; then
    parent=$(git rev-parse "${commit_sha}^")
    echo "No PR associated with generated web asset commit; checking parent $parent"
    query_associated_prs "$parent" > "$associated"
    pr_count=$(jq '[.[] | select(.merged_at != null)] | length' "$associated")
  fi
fi

if (( pr_count > 0 )); then
  jq '[.[] | select(.merged_at != null)] | sort_by(.merged_at) | last | {number, title, html_url}' \
    "$associated" > "$source_json"
  jq -r '[.[] | select(.merged_at != null)] | sort_by(.merged_at) | last | (.body // "")' \
    "$associated" > "$body_file"
else
  subject=$(git show -s --format=%s "$commit_sha")
  commit_url="https://github.com/${GITHUB_REPOSITORY}/commit/${commit_sha}"
  jq -n --arg title "$subject" --arg html_url "$commit_url" \
    '{number: null, title: $title, html_url: $html_url}' > "$source_json"
  cat > "$body_file" <<'EOF'
<!-- reader-changelog:start -->
无用户可见变化。
<!-- reader-changelog:end -->
EOF
fi

python3 .github/scripts/reader-changelog.py extract \
  --body-file "$body_file" \
  --output "$changelog_file" \
  --allow-missing

gh api \
  -H "Accept: application/vnd.github+json" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  "repos/${GITHUB_REPOSITORY}/releases?per_page=100" > "$releases_json"

python3 .github/scripts/reader-changelog.py compose \
  --changelog-file "$changelog_file" \
  --source-json "$source_json" \
  --releases-json "$releases_json" \
  --seed-file app/src/main/assets/readerChangelogSeed.md \
  --upstream-log app/src/main/assets/updateLog.md \
  --version "$version" \
  --version-code "$version_code" \
  --tag "$tag" \
  --commit "$commit_sha" \
  --channel "$channel" \
  --release-output "$output_dir/release-notes.md" \
  --app-output "$output_dir/readerReleaseNotes.md" \
  --history-output "$output_dir/readerChangelog.md"
