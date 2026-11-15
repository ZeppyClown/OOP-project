# Stage 3 diagrams — draw.io and PNG

| Folder | Contents | Status |
| --- | --- | --- |
| [`report-print/`](#report-print-edition-authoritative-for-the-pdf) | Focused class views and the selected 10-part sequence placed in the group report PDF | **Authoritative print edition** |
| `class/` | Six complete/wide class views | Navigation/reference (older; matches the earlier recording) |
| `sequence/outcome-flip-detail/`, `sequence/outcome-flip-complete/` | Happy Hour verdict-flip sequence, split detail and complete 8-page versions | Navigation/reference |
| `sequence/vip-priority-detail/`, `sequence/vip-priority-complete/` | Supplementary VIP priority sequence, split detail and complete versions | Navigation/reference |

Each folder keeps `.drawio` sources beside a `png/` folder of exports. [`conversion-pages.json`](conversion-pages.json) lists page counts for the navigation views. Index of all stages: [`../README.md`](../README.md).

Every `.drawio` file contains editable UML elements. Class members are children of their class boxes and relationship connectors are attached to their classes, so these move together. The matching PNGs are exported with the installed draw.io application. Styling follows the earlier stages: Helvetica, white class compartments, black connectors and UML lifelines/frames.

Long sequences are split across numbered draw.io pages. PNG numbers identify actual export pages; continued frames repeat the active branch guard on the next page. The focused views are intended for report placement; the complete class/sequence views are navigation references. Check final PDF readability after scaling.

## Navigation class views — `class/`

| View | Editable source | PNG |
| --- | --- | --- |
| class-control | [class-control.drawio](class/class-control.drawio) | [PNG](class/png/class-control.png) |
| class-diagram | [class-diagram.drawio](class/class-diagram.drawio) | [PNG](class/png/class-diagram.png) |
| class-entities | [class-entities.drawio](class/class-entities.drawio) | [PNG](class/png/class-entities.png) |
| class-menu | [class-menu.drawio](class/class-menu.drawio) | [PNG](class/png/class-menu.png) |
| class-overview | [class-overview.drawio](class/class-overview.drawio) | [PNG](class/png/class-overview.png) |
| class-staff | [class-staff.drawio](class/class-staff.drawio) | [PNG](class/png/class-staff.png) |

## Navigation sequence: Happy Hour changes the verdict — `sequence/outcome-flip-*`

Starting conditions: after turn 12, C1/C2 have paid, C3 is seated with satisfaction 28, and C5 arrives on turn 13. Activating on turn 13 versus never activating changes the final result while keeping all staff choices identical. The run uses four paid customers: $49.50 / 67.3 / VICTORY versus $56.10 / 57.8 / DEFEAT. See [scenario and actual fixture outputs](../../stage%203/test-fixtures/FLIP_SCENARIO.md).

| Detail | Editable source | PNG pages |
| --- | --- | --- |
| sequence-outcome-flip-1 | [draw.io](sequence/outcome-flip-detail/sequence-outcome-flip-1.drawio) | [1](sequence/outcome-flip-detail/png/sequence-outcome-flip-1-1.png) · [2](sequence/outcome-flip-detail/png/sequence-outcome-flip-1-2.png) · [3](sequence/outcome-flip-detail/png/sequence-outcome-flip-1-3.png) · [4](sequence/outcome-flip-detail/png/sequence-outcome-flip-1-4.png) |
| sequence-outcome-flip-2 | [draw.io](sequence/outcome-flip-detail/sequence-outcome-flip-2.drawio) | [1](sequence/outcome-flip-detail/png/sequence-outcome-flip-2-1.png) · [2](sequence/outcome-flip-detail/png/sequence-outcome-flip-2-2.png) · [3](sequence/outcome-flip-detail/png/sequence-outcome-flip-2-3.png) |
| sequence-outcome-flip-3 | [draw.io](sequence/outcome-flip-detail/sequence-outcome-flip-3.drawio) | [1](sequence/outcome-flip-detail/png/sequence-outcome-flip-3-1.png) · [2](sequence/outcome-flip-detail/png/sequence-outcome-flip-3-2.png) |
| sequence-outcome-flip | [draw.io](sequence/outcome-flip-complete/sequence-outcome-flip.drawio) | [1](sequence/outcome-flip-complete/png/sequence-outcome-flip-1.png) · [2](sequence/outcome-flip-complete/png/sequence-outcome-flip-2.png) · [3](sequence/outcome-flip-complete/png/sequence-outcome-flip-3.png) · [4](sequence/outcome-flip-complete/png/sequence-outcome-flip-4.png) · [5](sequence/outcome-flip-complete/png/sequence-outcome-flip-5.png) · [6](sequence/outcome-flip-complete/png/sequence-outcome-flip-6.png) · [7](sequence/outcome-flip-complete/png/sequence-outcome-flip-7.png) · [8](sequence/outcome-flip-complete/png/sequence-outcome-flip-8.png) |

## Navigation sequence: supplementary VIP priority — `sequence/vip-priority-*`

This pair changes payment timing/satisfaction; both paths end in defeat. It is supplementary to the selected verdict-flip scenario.

| Detail | Editable source | PNG pages |
| --- | --- | --- |
| sequence-1-start-host | [draw.io](sequence/vip-priority-detail/sequence-1-start-host.drawio) | [1](sequence/vip-priority-detail/png/sequence-1-start-host-1.png) · [2](sequence/vip-priority-detail/png/sequence-1-start-host-2.png) · [3](sequence/vip-priority-detail/png/sequence-1-start-host-3.png) |
| sequence-2-waiter-chef | [draw.io](sequence/vip-priority-detail/sequence-2-waiter-chef.drawio) | [1](sequence/vip-priority-detail/png/sequence-2-waiter-chef-1.png) · [2](sequence/vip-priority-detail/png/sequence-2-waiter-chef-2.png) · [3](sequence/vip-priority-detail/png/sequence-2-waiter-chef-3.png) |
| sequence-3-serve-pay | [draw.io](sequence/vip-priority-detail/sequence-3-serve-pay.drawio) | [1](sequence/vip-priority-detail/png/sequence-3-serve-pay-1.png) · [2](sequence/vip-priority-detail/png/sequence-3-serve-pay-2.png) |
| sequence-4-result | [draw.io](sequence/vip-priority-detail/sequence-4-result.drawio) | [1](sequence/vip-priority-detail/png/sequence-4-result.png) |
| sequence-diagram | [draw.io](sequence/vip-priority-complete/sequence-diagram.drawio) | [1](sequence/vip-priority-complete/png/sequence-diagram-1.png) · [2](sequence/vip-priority-complete/png/sequence-diagram-2.png) |

The diagrams were converted from the Stage 3 reference in `Documents/OOP`, then checked against the local Java. Factory calls use a factory participant; success/refusal alternatives and continued fragments remain editable. Selected sequence lifelines for multiple orders/customers are summaries; notes identify the concrete IDs. Optional refusal paths are not extra actions in the valid fixtures. Earlier stage figures are in [`../stage 1/`](../stage%201/README.md) and [`../stage 2/`](../stage%202/README.md).


## Report print edition (authoritative for the PDF)

The report edition in [`report-print/`](report-print) supplies the printable class and selected-sequence views. It uses end multiplicities, attached class connectors, selected member lists and at most eight lifelines per sequence panel. An explicit turn-loop context is repeated on continued panels, and the final result is outside the loop. These sources and exports supersede the older wide views above for report placement; the older complete diagrams in `class/` and `sequence/` remain navigation/reference material and match the earlier recording. See [the rendered report](../../report/Restaurant_Rush_Group_Report.pdf), [panel index](../../report/FIGURE_APPENDIX.md) and [figure-size audit](../../report/evidence/figure-size-audit.json). Report Figures 1 and 2 (Stage 1 control, Stage 2 customer extension) are historical print pages kept with their own stages: [Stage 1 report-print](../stage%201/report-print) and [Stage 2 report-print](../stage%202/report-print).

Page counts: [report-print/manifest.json](report-print/manifest.json). PNG `-N` suffixes are draw.io page numbers; single-page files have no suffix. Exports use `draw.io -x -f png -p N -s 1.5 -b 15`.

### Print class views — `report-print/class/`

| Source | Pages (PNG) | Used in PDF as |
| --- | --- | --- |
| [class-control-report.drawio](report-print/class/class-control-report.drawio) | [1](report-print/class/png/class-control-report-1.png) · [2](report-print/class/png/class-control-report-2.png) | Figure 3 · 3b |
| [class-entities-report.drawio](report-print/class/class-entities-report.drawio) | [1](report-print/class/png/class-entities-report-1.png) · [2](report-print/class/png/class-entities-report-2.png) · [3](report-print/class/png/class-entities-report-3.png) | Figure 4 · (not printed) · 4b |
| [class-ownership-report.drawio](report-print/class/class-ownership-report.drawio) | [1](report-print/class/png/class-ownership-report.png) | Figure 4c |
| [class-staff-report.drawio](report-print/class/class-staff-report.drawio) | [1](report-print/class/png/class-staff-report.png) | Figure 5 |
| [class-support-report.drawio](report-print/class/class-support-report.drawio) | [1](report-print/class/png/class-support-report-1.png) · [2](report-print/class/png/class-support-report-2.png) | Figure 7 · 5b |
| [class-menu-report.drawio](report-print/class/class-menu-report.drawio) | [1](report-print/class/png/class-menu-report-1.png) · [2](report-print/class/png/class-menu-report-2.png) | Figure 6 · 6b |

### Print selected sequence — `report-print/sequence/`

Twelve sources, 38 PNG panels, all printed in Appendix A as S01–S10 (including the 06b/07b READY panels). Per-panel links are in the [figure appendix](../../report/FIGURE_APPENDIX.md).

| Source | PNG panels |
| --- | --- |
| [sequence-selected-01.drawio](report-print/sequence/sequence-selected-01.drawio) | [1](report-print/sequence/png/sequence-selected-01.png) |
| [sequence-selected-02.drawio](report-print/sequence/sequence-selected-02.drawio) | [1](report-print/sequence/png/sequence-selected-02-1.png) · [2](report-print/sequence/png/sequence-selected-02-2.png) · [3](report-print/sequence/png/sequence-selected-02-3.png) · [4](report-print/sequence/png/sequence-selected-02-4.png) |
| [sequence-selected-03.drawio](report-print/sequence/sequence-selected-03.drawio) | [1](report-print/sequence/png/sequence-selected-03-1.png) · [2](report-print/sequence/png/sequence-selected-03-2.png) · [3](report-print/sequence/png/sequence-selected-03-3.png) |
| [sequence-selected-04.drawio](report-print/sequence/sequence-selected-04.drawio) | [1](report-print/sequence/png/sequence-selected-04-1.png) · [2](report-print/sequence/png/sequence-selected-04-2.png) · [3](report-print/sequence/png/sequence-selected-04-3.png) · [4](report-print/sequence/png/sequence-selected-04-4.png) |
| [sequence-selected-05.drawio](report-print/sequence/sequence-selected-05.drawio) | [1](report-print/sequence/png/sequence-selected-05-1.png) · [2](report-print/sequence/png/sequence-selected-05-2.png) · [3](report-print/sequence/png/sequence-selected-05-3.png) · [4](report-print/sequence/png/sequence-selected-05-4.png) |
| [sequence-selected-06.drawio](report-print/sequence/sequence-selected-06.drawio) | [1](report-print/sequence/png/sequence-selected-06-1.png) · [2](report-print/sequence/png/sequence-selected-06-2.png) · [3](report-print/sequence/png/sequence-selected-06-3.png) · [4](report-print/sequence/png/sequence-selected-06-4.png) |
| [sequence-selected-06b-ready.drawio](report-print/sequence/sequence-selected-06b-ready.drawio) | [1](report-print/sequence/png/sequence-selected-06b-ready.png) |
| [sequence-selected-07.drawio](report-print/sequence/sequence-selected-07.drawio) | [1](report-print/sequence/png/sequence-selected-07-1.png) · [2](report-print/sequence/png/sequence-selected-07-2.png) · [3](report-print/sequence/png/sequence-selected-07-3.png) · [4](report-print/sequence/png/sequence-selected-07-4.png) · [5](report-print/sequence/png/sequence-selected-07-5.png) |
| [sequence-selected-07b-ready.drawio](report-print/sequence/sequence-selected-07b-ready.drawio) | [1](report-print/sequence/png/sequence-selected-07b-ready.png) |
| [sequence-selected-08.drawio](report-print/sequence/sequence-selected-08.drawio) | [1](report-print/sequence/png/sequence-selected-08-1.png) · [2](report-print/sequence/png/sequence-selected-08-2.png) · [3](report-print/sequence/png/sequence-selected-08-3.png) · [4](report-print/sequence/png/sequence-selected-08-4.png) |
| [sequence-selected-09.drawio](report-print/sequence/sequence-selected-09.drawio) | [1](report-print/sequence/png/sequence-selected-09-1.png) · [2](report-print/sequence/png/sequence-selected-09-2.png) · [3](report-print/sequence/png/sequence-selected-09-3.png) |
| [sequence-selected-10.drawio](report-print/sequence/sequence-selected-10.drawio) | [1](report-print/sequence/png/sequence-selected-10-1.png) · [2](report-print/sequence/png/sequence-selected-10-2.png) · [3](report-print/sequence/png/sequence-selected-10-3.png) · [4](report-print/sequence/png/sequence-selected-10-4.png) |
