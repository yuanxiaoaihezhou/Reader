#!/usr/bin/env python3
import importlib.util
import json
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SCRIPT = ROOT / ".github/scripts/reader-changelog.py"
spec = importlib.util.spec_from_file_location("reader_changelog", SCRIPT)
mod = importlib.util.module_from_spec(spec)
assert spec.loader is not None
spec.loader.exec_module(mod)


def check(condition, message):
    if not condition:
        raise AssertionError(message)


def main():
    body = f"""## 用户可见更新日志\n{mod.START}\n### 修复\n- 修复更新日志生成。\n{mod.END}\n"""
    extracted = mod.extract_changelog(body)
    check("修复更新日志生成" in extracted, "valid changelog should be extracted")

    for invalid in ("", f"{mod.START}\n请填写\n{mod.END}", f"{mod.START}\nEnglish only\n{mod.END}"):
        try:
            mod.extract_changelog(invalid)
        except mod.ChangelogError:
            pass
        else:
            raise AssertionError(f"invalid body unexpectedly accepted: {invalid!r}")

    check(mod.extract_changelog("no markers", allow_missing=True) == mod.NO_VISIBLE_CHANGE,
          "release extraction should have a safe fallback")

    release_body = (
        "<!-- reader-version-code:123 -->\n"
        f"{mod.START}\n### 优化\n- 优化自动更新。\n{mod.END}\n"
    )
    releases = [{
        "tag_name": "v1.0.0-beta.2",
        "draft": False,
        "published_at": "2026-10-23T06:00:00Z",
        "body": release_body,
        "html_url": "https://example.invalid/release",
    }]
    history = mod.build_full_history(
        version="1.0.0-beta.3",
        tag="v1.0.0-beta.3",
        changelog="### 新增\n- 新增中文发布日志。",
        source={"number": 10, "title": "发布日志", "html_url": "https://example.invalid/pr/10"},
        releases=releases,
        seed="## 历史补录\n\n- 旧版修复。",
        upstream_log="# 更新日志\n\n**2026/10/22**\n- 上游修复。",
        today="2026-10-23",
    )
    check("Reader 1.0.0-beta.3" in history, "current version missing")
    check("Reader 1.0.0-beta.2" in history, "previous marked release missing")
    check("历史补录" in history, "seed history missing")
    check("Legado 上游更新日志" in history and "上游修复" in history, "upstream history missing")

    notes = mod.build_release_notes(
        version="1.0.0-beta.3",
        version_code="38530",
        commit="1234567890abcdef",
        built="2026-10-23T07:00:00Z",
        changelog="### 修复\n- 修复发布说明。",
        source={"number": 10, "title": "发布日志", "html_url": "https://example.invalid/pr/10"},
        prerelease=True,
    )
    check("reader-version-code:38530" in notes, "version-code metadata missing")
    check(mod.START in notes and mod.END in notes, "release changelog markers missing")
    check("PR #10" in notes, "PR source missing")
    check("自动测试版" in notes, "beta warning missing")

    stable_notes = mod.build_release_notes(
        version="1.0.0",
        version_code="38540",
        commit="abcdef1234567890",
        built="2026-10-23T08:00:00Z",
        changelog="### 发布\n- 发布 Reader 稳定版。",
        source={"number": 20, "title": "stable", "html_url": "https://example.invalid/pr/20"},
        prerelease=False,
    )
    check("自动测试版" not in stable_notes, "stable notes must not contain beta warning")

    print("Reader changelog tests passed")


if __name__ == "__main__":
    main()
