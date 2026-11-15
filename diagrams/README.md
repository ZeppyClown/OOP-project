# Restaurant Rush diagrams

All diagrams live here, organised by stage. Every view has an editable native draw.io source (`.drawio`) and a PNG export in the neighbouring `png/` folder.

| Stage | Index | Complete design views | Report print edition |
| --- | --- | --- | --- |
| 1 | [stage 1/README.md](stage%201/README.md) | `class/` (5), `sequence/` (1) | `report-print/` — report Figure 1 |
| 2 | [stage 2/README.md](stage%202/README.md) | `class/` (6), `sequence/` (1 file, 5 pages) | `report-print/` — report Figure 2 |
| 3 | [stage 3/README.md](stage%203/README.md) | `class/` (6), `sequence/outcome-flip-*`, `sequence/vip-priority-*` — navigation/reference | `report-print/class/`, `report-print/sequence/` — **authoritative**, Figures 3–7 and Appendix S01–S10 |

The group report renderer ([`../report/render_report.py`](../report/render_report.py)) reads PNGs only from the `report-print/` folders. Exports were made with the draw.io desktop CLI; the report-print pages use `draw.io -x -f png -p <page> -s 1.5 -b 15 -o <png> <source>`.

PNG naming: a `-N` suffix is the draw.io page number of a multi-page source; single-page sources have no suffix.
