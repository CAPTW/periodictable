#!/usr/bin/env python3
import json
import pathlib
import sys


def fail(msg: str) -> None:
    print(f"[asset-validate] FAIL: {msg}", file=sys.stderr)
    sys.exit(1)


def load_json(path: pathlib.Path):
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except Exception as exc:
        fail(f"cannot parse {path}: {exc}")


def require_keys(obj: dict, keys: list[str], label: str) -> None:
    missing = [k for k in keys if k not in obj]
    if missing:
        fail(f"{label} missing keys: {', '.join(missing)}")


def main() -> None:
    repo = pathlib.Path(__file__).resolve().parents[1]
    assets = repo / "app" / "src" / "main" / "assets"
    elements_path = assets / "elements.json"
    isotopes_path = assets / "isotopes.json"
    glossary_path = assets / "glossary.json"

    if not elements_path.exists():
        fail("elements.json not found")
    if not isotopes_path.exists():
        fail("isotopes.json not found")
    if not glossary_path.exists():
        fail("glossary.json not found")

    elements = load_json(elements_path)
    isotopes = load_json(isotopes_path)
    glossary = load_json(glossary_path)

    if not isinstance(elements, list) or len(elements) < 127:
        fail("elements.json must be a list with at least 127 entries")
    if not isinstance(isotopes, list) or len(isotopes) < 80:
        fail("isotopes.json must be a list with at least 80 entries")
    if not isinstance(glossary, list) or len(glossary) < 170:
        fail("glossary.json must be a list with at least 170 entries")

    element_required = [
        "atomicNumber",
        "symbol",
        "name",
        "nameKo",
        "latinName",
        "englishPronunciation",
        "protonCount",
        "electronCount",
        "electronShells",
        "block",
        "meltingPoint",
        "boilingPoint",
        "electricalType",
        "crystalStructure",
        "hazardHealth",
        "hazardFlammability",
        "hazardReactivity",
        "abundanceUniverse",
        "dataSource",
        "dataUpdatedAt",
    ]
    isotope_required = [
        "atomicNumber",
        "massNumber",
        "symbol",
        "isStable",
        "halfLife",
        "decayMode",
        "applicationTags",
    ]
    glossary_required = ["id", "termKo", "termEn", "definition", "category"]

    seen_atomic = set()
    for idx, element in enumerate(elements):
        if not isinstance(element, dict):
            fail(f"elements[{idx}] is not an object")
        require_keys(element, element_required, f"elements[{idx}]")
        atomic = element["atomicNumber"]
        if not isinstance(atomic, int):
            fail(f"elements[{idx}].atomicNumber must be int")
        if atomic in seen_atomic:
            fail(f"duplicate atomicNumber in elements: {atomic}")
        seen_atomic.add(atomic)

    expected_atomic = set(range(1, 128))
    if not expected_atomic.issubset(seen_atomic):
        missing = sorted(expected_atomic.difference(seen_atomic))
        fail(f"elements missing atomic numbers: {missing}")

    for idx, isotope in enumerate(isotopes):
        if not isinstance(isotope, dict):
            fail(f"isotopes[{idx}] is not an object")
        require_keys(isotope, isotope_required, f"isotopes[{idx}]")
        atomic = isotope["atomicNumber"]
        if not isinstance(atomic, int) or atomic < 1 or atomic > 127:
            fail(f"isotopes[{idx}].atomicNumber must be 1..127")
        tags = isotope["applicationTags"]
        if not isinstance(tags, list):
            fail(f"isotopes[{idx}].applicationTags must be array")

    glossary_ids = set()
    for idx, term in enumerate(glossary):
        if not isinstance(term, dict):
            fail(f"glossary[{idx}] is not an object")
        require_keys(term, glossary_required, f"glossary[{idx}]")
        term_id = term["id"]
        if term_id in glossary_ids:
            fail(f"duplicate glossary id: {term_id}")
        glossary_ids.add(term_id)

    needed_glossary = {
        "isotope",
        "ionization_energy",
        "electron_affinity",
        "crystal_structure",
        "nfpa_diamond",
        "cas_registry_number",
    }
    missing_terms = sorted(needed_glossary.difference(glossary_ids))
    if missing_terms:
        fail(f"glossary missing required terms: {missing_terms}")

    print("[asset-validate] PASS")


if __name__ == "__main__":
    main()
