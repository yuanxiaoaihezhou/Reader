#!/usr/bin/env python3
from __future__ import annotations

import argparse
import datetime as dt
import json
import re
from pathlib import Path

START = "<!-- reader-changelog:start -->"
END = "<!-- reader-changelog:end -->"
NO_VISIBLE_CHANGE = "无用户可见变化。"
PLACEHOLDER_TOKENS = ("请填写", "待填写", "TODO", "TBD")
READER_TAG_RE = re.compile(r"^v(?P<version>\d+\.\d+\.\d+(?:-beta\.\d+)?)$")
HAN_RE = re.compile(r"[\u3400-\u9fff]")


class ChangelogError(ValueError):
    pass


def extract_changelog(body: str, *, allow_missing: bool = False) -> str:
    start_count = body.count(START)
    end_count = body.count(END)
    if start_count == 0 and end_count == 0 and allow_missing:
        return NO_VISIBLE_CHANGE
    if start_count != 1 or end_count != 1:
        raise ChangelogError(
            f"PR 描述必须且只能包含一组 changelog 标记：{START} / {END}"
        )
    start = body.index(START) + len(START)
    end = body.index(END)
    if end <= start:
        raise ChangelogError("reader changelog 结束标记必须位于开始标记之后")
    content = body[start:end].strip()
    if not content:
        raise ChangelogError("用户可见更新日志不能为空")
    if any(token.lower() in content.lower() for token in PLACEHOLDER_TOKENS):
        raise ChangelogError("用户可见更新日志仍包含模板占位文字")
    if not HAN_RE.search(content):
        raise ChangelogError("用户可见更新日志请至少包含一处中文说明")
    return content


def extract_marked_changelog(text: str) -> str | None:
    try:
        return extract_changelog(text)
    except ChangelogError:
        return None


def strip_markdown_title(text: str, expected: str | None = None) -> str:
    normalized = text.lstrip("\ufeff\r\n ")
    lines = normalized.splitlines()
    if not lines:
        return ""
    first = lines[0].strip()
    if first.startswith("# ") and (expected is None or first == expected):
        return "\n".join(lines[1:]).lstrip("\n")
    return normalized.rstrip()


def source_line(source: dict) -> str:
    number = source.get("number")
    title = (source.get("title") or "").strip()
    url = (source.get("html_url") or "").strip()
    if number:
        label = f"PR #{number}"
        if title:
            label += f" · {title}"
    else:
        label = title or "直接提交"
    safe_label = label.replace("[", "\\[").replace("]", "\\]")
    return f"[{safe_label}]({url})" if url else label


def build_release_notes(
    *,
    version: str,
    version_code: str,
    commit: str,
    built: str,
    changelog: str,
    source: dict,
    prerelease: bool,
) -> str:
    warning = "\n> 此版本为自动测试版，升级前请备份数据。\n" if prerelease else ""
    return (
        f"<!-- reader-version-code:{version_code} -->\n"
        f"# Reader {version}\n\n"
        "## 本次更新\n\n"
        f"{START}\n{changelog}\n{END}\n\n"
        "## 来源\n\n"
        f"{source_line(source)}\n\n"
        "## 构建信息\n\n"
        f"- VersionCode: `{version_code}`\n"
        f"- Commit: `{commit[:7]}`\n"
        f"- Built: {built}\n"
        f"{warning}"
    ).rstrip() + "\n"


def build_app_release_notes(*, version: str, changelog: str, source: dict, prerelease: bool) -> str:
    warning = "\n> 这是 Reader 测试版，升级前建议先备份数据。\n" if prerelease else ""
    return (
        f"# Reader {version}\n\n"
        "## 本次更新\n\n"
        f"{changelog}\n\n"
        "## 来源\n\n"
        f"{source_line(source)}\n"
        f"{warning}"
    ).rstrip() + "\n"


def iter_release_history(releases: list[dict], current_tag: str):
    items = []
    for release in releases:
        if release.get("draft"):
            continue
        tag = release.get("tag_name") or ""
        match = READER_TAG_RE.fullmatch(tag)
        if not match or tag == current_tag:
            continue
        changelog = extract_marked_changelog(release.get("body") or "")
        if not changelog:
            continue
        published_at = release.get("published_at") or release.get("created_at") or ""
        items.append(
            {
                "tag": tag,
                "version": match.group("version"),
                "published": published_at[:10],
                "published_at": published_at,
                "changelog": changelog,
                "url": release.get("html_url") or "",
            }
        )
    items.sort(key=lambda item: item["published_at"], reverse=True)
    return items


def build_full_history(
    *,
    version: str,
    tag: str,
    changelog: str,
    source: dict,
    releases: list[dict],
    seed: str,
    upstream_log: str,
    today: str,
) -> str:
    parts = [
        "# Reader 更新日志",
        "",
        "> Reader 自己的发行记录。自动 changelog 启用前的改动已在下方补录；文末保留 Legado 上游更新日志。",
        "",
        f"## Reader {version} · {today}",
        "",
        changelog,
        "",
        f"来源：{source_line(source)}",
    ]
    for item in iter_release_history(releases, tag):
        suffix = f" · {item['published']}" if item["published"] else ""
        parts += ["", f"## Reader {item['version']}{suffix}", "", item["changelog"]]
        if item["url"]:
            parts += ["", f"来源：[GitHub Release]({item['url']})"]
    seed_text = seed.strip()
    if seed_text:
        parts += ["", seed_text]
    upstream = strip_markdown_title(upstream_log, "# 更新日志").strip()
    if upstream:
        parts += [
            "",
            "# Legado 上游更新日志",
            "",
            "> 以下内容随 Reader 的上游同步保留，便于查看继承自 Legado 的更新。",
            "",
            upstream,
        ]
    return "\n".join(parts).rstrip() + "\n"


def read_json(path: Path, default):
    if not path.exists():
        return default
    return json.loads(path.read_text(encoding="utf-8-sig"))


def cmd_validate(args):
    content = extract_changelog(Path(args.body_file).read_text(encoding="utf-8-sig"))
    print("Reader changelog valid:")
    print(content)


def cmd_extract(args):
    body = Path(args.body_file).read_text(encoding="utf-8-sig")
    content = extract_changelog(body, allow_missing=args.allow_missing)
    Path(args.output).write_text(content.rstrip() + "\n", encoding="utf-8", newline="\n")


def cmd_compose(args):
    changelog = Path(args.changelog_file).read_text(encoding="utf-8-sig").strip()
    if not changelog:
        raise ChangelogError("changelog file is empty")
    source = read_json(Path(args.source_json), {})
    releases = read_json(Path(args.releases_json), [])
    if not isinstance(releases, list):
        raise ChangelogError("releases JSON must be an array")
    seed = Path(args.seed_file).read_text(encoding="utf-8-sig")
    upstream = Path(args.upstream_log).read_text(encoding="utf-8-sig")
    built = args.built or dt.datetime.now(dt.timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")
    today = built[:10]
    prerelease = args.channel == "beta"

    release_notes = build_release_notes(
        version=args.version,
        version_code=args.version_code,
        commit=args.commit,
        built=built,
        changelog=changelog,
        source=source,
        prerelease=prerelease,
    )
    app_notes = build_app_release_notes(
        version=args.version,
        changelog=changelog,
        source=source,
        prerelease=prerelease,
    )
    history = build_full_history(
        version=args.version,
        tag=args.tag,
        changelog=changelog,
        source=source,
        releases=releases,
        seed=seed,
        upstream_log=upstream,
        today=today,
    )
    Path(args.release_output).write_text(release_notes, encoding="utf-8", newline="\n")
    Path(args.app_output).write_text(app_notes, encoding="utf-8", newline="\n")
    Path(args.history_output).write_text(history, encoding="utf-8", newline="\n")


def parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(description="Reader PR changelog validation and release-note generation")
    sub = p.add_subparsers(dest="command", required=True)

    validate = sub.add_parser("validate")
    validate.add_argument("--body-file", required=True)
    validate.set_defaults(func=cmd_validate)

    extract = sub.add_parser("extract")
    extract.add_argument("--body-file", required=True)
    extract.add_argument("--output", required=True)
    extract.add_argument("--allow-missing", action="store_true")
    extract.set_defaults(func=cmd_extract)

    compose = sub.add_parser("compose")
    compose.add_argument("--changelog-file", required=True)
    compose.add_argument("--source-json", required=True)
    compose.add_argument("--releases-json", required=True)
    compose.add_argument("--seed-file", required=True)
    compose.add_argument("--upstream-log", required=True)
    compose.add_argument("--version", required=True)
    compose.add_argument("--version-code", required=True)
    compose.add_argument("--tag", required=True)
    compose.add_argument("--commit", required=True)
    compose.add_argument("--channel", choices=("beta", "stable"), required=True)
    compose.add_argument("--built")
    compose.add_argument("--release-output", required=True)
    compose.add_argument("--app-output", required=True)
    compose.add_argument("--history-output", required=True)
    compose.set_defaults(func=cmd_compose)
    return p


def main() -> int:
    args = parser().parse_args()
    try:
        args.func(args)
    except ChangelogError as exc:
        print(f"Reader changelog error: {exc}", flush=True)
        return 2
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
