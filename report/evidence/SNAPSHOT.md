# Report evidence snapshot

Checked 15 November 2026 (SGT).

- Stage 1 tag: `9197f247899df4038a9bcff601aad44d039c8779`.
- Stage 2 tag: `4711c313b455a0d88fb14553c936b0e25560b579`.
- Current main and Victor: `699b3173444e7efae4b324101130d73ce1252577`.
- Stage 3 tag/release: absent at this check.
- Final restoration tree is identical to independently reviewed `f342f8d`; it was republished as four repair commits, not new historical feature development.
- `run-tests.sh.log` and `check-build.sh.log` are the actual final publication checks from 15 November, not fresh runs created by writing the report.
- Change CSV compares exact bytes in production type files across logical package paths, ignoring folder renames. It includes formatting and comment changes.
- Current-main Money differs only in presentation (comments/whitespace and equivalent braced/unbraced formatting); GameView is documentation-only and CustomerStatus comments/whitespace-only.
- Recorded Stage 1/2 totals29/40 are historical project checks; Stage 3 final automated result55/55 is retained here.
