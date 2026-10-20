"""Restore PPC data barriers omitted by the x86-oriented generated C++.

Writes Android-only source copies; never modifies the upstream checkout.
"""
import argparse
from pathlib import Path
import re

BARRIER = re.compile(r"(?m)^([ \t]*)// (sync|lwsync|eieio)[ \t]*\r?$")


def ordered_source(source):
    return BARRIER.subn(
        lambda match: match.group(0).rstrip("\r") + "\n" + match.group(1)
        + "__atomic_thread_fence(__ATOMIC_SEQ_CST);", source
    )


def prepare(source_dir, output_dir):
    output_dir.mkdir(parents=True, exist_ok=True)
    total = 0
    names = []
    for path in sorted(source_dir.glob("simpsons_recomp.*.cpp")):
        text = path.read_text(encoding="utf-8")
        fixed, count = ordered_source(text)
        if not count:
            continue
        target = output_dir / path.name
        if not target.exists() or target.read_text(encoding="utf-8") != fixed:
            target.write_text(fixed, encoding="utf-8", newline="\n")
        total += count
        names.append(path.name)
    # CMake consumes only this list, so obsolete copies cannot enter a build.
    manifest = "set(TSG_ORDERED_SOURCE_NAMES\n" + "".join(f'  "{n}"\n' for n in names) + ")\n"
    target = output_dir / "sources.cmake"
    if not target.exists() or target.read_text(encoding="utf-8") != manifest:
        target.write_text(manifest, encoding="utf-8")
    print(f"ARM64 ordering: {total} PPC data barriers restored in {len(names)} source files")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source_dir", type=Path)
    parser.add_argument("output_dir", type=Path)
    args = parser.parse_args()
    prepare(args.source_dir, args.output_dir)
