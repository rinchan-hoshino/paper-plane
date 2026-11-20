# Development

## Prerequisites

- JDK 17 (for `1.20.1`)
- JDK 21 (for `1.21.1`)
- JDK 25 (for `26.1.2`)
- Python 3.10+

## Building & Verifying All 6 Targets

Run the unified build & verification matrix:

```bash
python3 scripts/build-matrix.py
```

This script:
1. Runs Python contract tests in `tests/`.
2. Builds and runs all JUnit tests for all 6 targets defined in `matrix.json`:
   - `1.20.1` (`fabric`, `forge`)
   - `1.21.1` (`fabric`, `neoforge`)
   - `26.1.2` (`fabric`, `neoforge`)
3. Verifies every output JAR contains `icon.png`, all 4 localizations (`en_us`, `zh_cn`, `zh_tw`, `ja_jp`), all 4 origami item models & 8 item textures, and zero `rinlib`, `soggy`, `FlightModel`, or compile-only stub residue.

To build a subset:

```bash
python3 scripts/build-matrix.py --minecraft 1.21.1 --loader neoforge
```
