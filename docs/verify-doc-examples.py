#!/usr/bin/env python3
"""Verify every documented Java code example is exercised by a real test.

For each ```java block in docs/**/*.md, checks that:
  1. some test under xfty/src/test/java/net/nowhereatall/xfty/examples/ cites the
     doc page in a `// from docs/<page>` comment, and
  2. most of the block's significant lines (comments/imports/blank ignored,
     whitespace and the `lookup` identifier normalised) appear in the examples
     corpus - so an example that silently drifts from its test fails the build.

Run from the repo root:  python3 docs/verify-doc-examples.py
"""
from __future__ import annotations

import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
DOCS = ROOT / "docs"
EXAMPLES = ROOT / "xfty" / "src" / "test" / "java" / "net" / "nowhereatall" / "xfty" / "examples"

CODE_BLOCK = re.compile(r"```java\n(.*?)```", re.DOTALL)
TRIVIAL = re.compile(r"^\s*(//.*|import .*|package .*|/\*.*|\*.*|\*/|[{}()]+;?)?\s*$")
MIN_COVERAGE = 0.75


def canon(text: str) -> str:
    # a doc uses a `lookup` local; a test may use LOOKUP, lookup(), lookupWithCase() ...
    text = re.sub(r"\bLOOKUP\b", "lookup", text)
    text = re.sub(r"\blookup\w*\(\)", "lookup", text)
    # collapse ALL whitespace so a differently-wrapped test still matches
    return re.sub(r"\s+", "", text)


def statements(block: str) -> list[str]:
    """Join physical lines into logical statements so a differently-wrapped test still matches."""
    out, buffer = [], ""
    for raw_line in block.splitlines():
        line = re.sub(r"\s*//.*$", "", raw_line).rstrip()
        if not line.strip():
            continue
        buffer = f"{buffer} {line.strip()}".strip()
        if line.rstrip().endswith((";", "{", "}")):
            out.append(canon(buffer))
            buffer = ""
    if buffer:
        out.append(canon(buffer))
    return [s for s in out if s and not TRIVIAL.match(s)]


def load_examples() -> tuple[str, set[str]]:
    if not EXAMPLES.is_dir():
        sys.exit(f"no examples directory at {EXAMPLES}")
    raw = "\n".join(f.read_text(encoding="utf-8") for f in EXAMPLES.glob("*.java"))
    cited = set(re.findall(r"//\s*from\s+(docs/\S+?\.md)", raw))
    # build the same "logical statement" view of the tests the doc blocks get
    corpus = "\n".join(statements(raw))
    return corpus, cited


def main() -> int:
    corpus, cited_pages = load_examples()
    failures: list[str] = []

    for page in sorted(DOCS.rglob("*.md")):
        blocks = CODE_BLOCK.findall(page.read_text(encoding="utf-8"))
        if not blocks:
            continue
        rel = page.relative_to(ROOT).as_posix()
        if rel not in cited_pages:
            failures.append(f"{rel}: has java example(s) but no test carries `// from {rel}`")
            continue
        for index, block in enumerate(blocks, start=1):
            lines = [line for line in statements(block) if line]
            if not lines:
                continue
            found = sum(1 for line in lines if line in corpus)
            if found / len(lines) < MIN_COVERAGE:
                missing = [line for line in lines if line not in corpus]
                failures.append(
                    f"{rel} block {index}: only {found}/{len(lines)} lines found in an examples test; e.g.\n"
                    + "\n".join(f"      {line}" for line in missing[:3]))

    if failures:
        print("Doc examples out of sync with the tests:\n")
        print("\n".join(f"  - {f}" for f in failures))
        return 1
    print("Every documented java example is exercised by an examples test.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
