"""Validates the catalogue TSVs in app/src/main/assets/content (see docs/CONTENT.md).

    python tools/check_content.py            # every system
    python tools/check_content.py sleep move # only these
"""
import collections
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
DIR = ROOT / "app/src/main/assets/content"
SYSTEMS = {
    "sleep": ["light", "dark", "rhythm", "room", "winddown", "nap"],
    "move": ["walk", "cardio", "strength", "mobility", "neat", "balance"],
    "fuel": ["plants", "protein", "timing", "water", "less", "gut"],
    "shield": ["cold", "heat", "sun", "teeth", "check", "air"],
    "mind": ["focus", "learn", "digital", "create", "read", "novelty"],
    "calm": ["breath", "meditate", "body", "write", "slow", "sense"],
    "joy": ["grateful", "play", "meaning", "savor", "music", "awe"],
    "link": ["friends", "family", "kind", "nature", "community", "touch"],
}
EV = {"A", "B", "C", "D"}
WHEN = {"wake", "morning", "day", "evening", "bed", "any"}
MIN = {"1", "5", "15", "30", "60"}


def text_ok(where, lang, t, limit, errors):
    if not t.strip():
        errors.append(f"{where}: empty {lang}")
    if len(t) > limit:
        errors.append(f"{where}: {lang} is {len(t)} chars (> {limit})")
    if t.rstrip().endswith("."):
        errors.append(f"{where}: {lang} ends with a full stop")
    if lang.startswith("ru") and '"' in t:
        errors.append(f"{where}: straight quotes in {lang}")
    if t != t.strip():
        errors.append(f"{where}: {lang} has edge spaces")


def main(only=None):
    errors, warnings = [], []
    seen = collections.defaultdict(list)
    total = myths = 0
    for s, tags in SYSTEMS.items():
        if only and s not in only:
            continue
        f = DIR / f"{s}.tsv"
        if not f.exists():
            errors.append(f"{f.name}: missing")
            continue
        ids = set()
        per_tag = collections.Counter()
        ev = collections.Counter()
        load = collections.Counter()
        when = collections.Counter()
        quick = 0
        n = 0
        for k, line in enumerate(f.read_text(encoding="utf-8").splitlines(), 1):
            if not line.strip():
                continue
            c = line.split("\t")
            where = f"{f.name}:{k}"
            if len(c) != 17:
                errors.append(f"{where}: {len(c)} columns (want 17)")
                continue
            i, sys_, tag, e, rare, ld, wh, mn, fq, rn, en, rh, eh, rw, ew, rx, ex = c
            num = i.split("-")[-1]
            if not i.startswith(s + "-") or len(num) != 3 or not num.isdigit():
                errors.append(f"{where}: bad id {i!r}")
            if i in ids:
                errors.append(f"{where}: duplicate id {i}")
            ids.add(i)
            if sys_ != s:
                errors.append(f"{where}: sys {sys_!r} in {s}.tsv")
            if tag not in tags:
                errors.append(f"{where}: tag {tag!r} not in {s}")
            if e not in EV:
                errors.append(f"{where}: ev {e!r}")
            if rare not in {"1", "2", "3"}:
                errors.append(f"{where}: rare {rare!r}")
            if ld not in {"1", "2", "3"}:
                errors.append(f"{where}: load {ld!r}")
            if wh not in WHEN:
                errors.append(f"{where}: when {wh!r}")
            if mn not in MIN:
                errors.append(f"{where}: min {mn!r}")
            if fq not in set("1234567"):
                errors.append(f"{where}: freq {fq!r}")
            text_ok(where, "ru_name", rn, 34, errors)
            text_ok(where, "en_name", en, 34, errors)
            text_ok(where, "ru_how", rh, 220, errors)
            text_ok(where, "en_how", eh, 220, errors)
            text_ok(where, "ru_why", rw, 220, errors)
            text_ok(where, "en_why", ew, 220, errors)
            if rx != "-":
                text_ok(where, "ru_warn", rx, 140, errors)
            if ex != "-":
                text_ok(where, "en_warn", ex, 140, errors)
            if (rx == "-") != (ex == "-"):
                errors.append(f"{where}: warn only in one language")
            for key, t in (("ru_name", rn), ("en_name", en), ("ru_how", rh), ("en_how", eh)):
                seen[(key, " ".join(t.lower().split()))].append(where)
            per_tag[tag] += 1
            ev[e] += 1
            load[ld] += 1
            when[wh] += 1
            if mn in {"1", "5"}:
                quick += 1
            n += 1
        total += n
        if n < 120:
            warnings.append(f"{s}: {n} mods (< 120)")
        for t in tags:
            if per_tag[t] < 14:
                warnings.append(f"{s}: tag {t} has {per_tag[t]} (< 14)")
        if quick < 40:
            warnings.append(f"{s}: {quick} mods take ≤ 5 min (< 40)")
        if n and (ev["A"] + ev["B"]) * 100 < 30 * n:
            warnings.append(f"{s}: A+B is {ev['A'] + ev['B']} of {n} (< 30 %)")
        if n and ev["D"] * 100 > 25 * n:
            warnings.append(f"{s}: D is {ev['D']} of {n} (> 25 %)")
        if n and when["any"] * 2 > n:
            warnings.append(f"{s}: when=any is {when['any']} of {n} (> half)")
        if n and load["1"] * 3 < n:
            warnings.append(f"{s}: load 1 is {load['1']} of {n} (< a third)")
        print(f"{s:7} {n:4}  tags {dict(per_tag)}  ev {dict(sorted(ev.items()))}  load {dict(sorted(load.items()))}  quick {quick}")

        mf = DIR / f"myths-{s}.tsv"
        if not mf.exists():
            errors.append(f"{mf.name}: missing")
            continue
        mids = set()
        m = 0
        for k, line in enumerate(mf.read_text(encoding="utf-8").splitlines(), 1):
            if not line.strip():
                continue
            c = line.split("\t")
            where = f"{mf.name}:{k}"
            if len(c) != 6:
                errors.append(f"{where}: {len(c)} columns (want 6)")
                continue
            i, sys_, rn, en, rt, et = c
            if not i.startswith(s + "-m") or not i[len(s) + 2:].isdigit():
                errors.append(f"{where}: bad id {i!r}")
            if i in mids:
                errors.append(f"{where}: duplicate id {i}")
            mids.add(i)
            if sys_ != s:
                errors.append(f"{where}: sys {sys_!r}")
            text_ok(where, "ru_name", rn, 34, errors)
            text_ok(where, "en_name", en, 34, errors)
            text_ok(where, "ru_truth", rt, 220, errors)
            text_ok(where, "en_truth", et, 220, errors)
            m += 1
        myths += m
        if m < 10:
            warnings.append(f"{s}: {m} myths (< 10)")
    for key, where in seen.items():
        if len(where) > 1:
            errors.append(f"duplicate {key[0]} in {', '.join(where)}")
    print(f"\n{total} mods, {myths} myths")
    for w in warnings:
        print("warn:", w)
    for e in errors:
        print("ERROR:", e)
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main(set(sys.argv[1:]) or None))
