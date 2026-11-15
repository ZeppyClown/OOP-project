# Stage 3 diagrams — draw.io and PNG

Every `.drawio` file contains editable UML elements. Class members are children of their class boxes and relationship connectors are attached to their classes, so these move together. The matching PNGs are exported with the installed draw.io application. Styling follows the earlier stages: Helvetica, white class compartments, black connectors and UML lifelines/frames.

Long sequences are split across numbered draw.io pages. PNG numbers identify actual export pages; continued frames repeat the active branch guard on the next page. The focused views are intended for report placement; the complete class/sequence views are navigation references. Check final PDF readability after scaling.

## Class diagrams

| View | Editable source | PNG |
| --- | --- | --- |
| class-control | [class-control.drawio](class-control.drawio) | [PNG](png/class-control.png) |
| class-diagram | [class-diagram.drawio](class-diagram.drawio) | [PNG](png/class-diagram.png) |
| class-entities | [class-entities.drawio](class-entities.drawio) | [PNG](png/class-entities.png) |
| class-menu | [class-menu.drawio](class-menu.drawio) | [PNG](png/class-menu.png) |
| class-overview | [class-overview.drawio](class-overview.drawio) | [PNG](png/class-overview.png) |
| class-staff | [class-staff.drawio](class-staff.drawio) | [PNG](png/class-staff.png) |

## Selected sequence: Happy Hour changes the verdict

Starting conditions: after turn 12, C1/C2 have paid, C3 is seated with satisfaction 28, and C5 arrives on turn 13. Activating on turn 13 versus never activating changes the final result while keeping all staff choices identical. The run uses four paid customers: $49.50 / 67.3 / VICTORY versus $56.10 / 57.8 / DEFEAT. See [scenario and actual fixture outputs](../../stage%203/test-fixtures/FLIP_SCENARIO.md).

| Detail | Editable source | PNG pages |
| --- | --- | --- |
| sequence-outcome-flip-1 | [draw.io](sequence-outcome-flip-1.drawio) | [1](png/sequence-outcome-flip-1-1.png) · [2](png/sequence-outcome-flip-1-2.png) · [3](png/sequence-outcome-flip-1-3.png) · [4](png/sequence-outcome-flip-1-4.png) |
| sequence-outcome-flip-2 | [draw.io](sequence-outcome-flip-2.drawio) | [1](png/sequence-outcome-flip-2-1.png) · [2](png/sequence-outcome-flip-2-2.png) · [3](png/sequence-outcome-flip-2-3.png) |
| sequence-outcome-flip-3 | [draw.io](sequence-outcome-flip-3.drawio) | [1](png/sequence-outcome-flip-3-1.png) · [2](png/sequence-outcome-flip-3-2.png) |
| sequence-outcome-flip | [draw.io](sequence-outcome-flip.drawio) | [1](png/sequence-outcome-flip-1.png) · [2](png/sequence-outcome-flip-2.png) · [3](png/sequence-outcome-flip-3.png) · [4](png/sequence-outcome-flip-4.png) · [5](png/sequence-outcome-flip-5.png) · [6](png/sequence-outcome-flip-6.png) · [7](png/sequence-outcome-flip-7.png) · [8](png/sequence-outcome-flip-8.png) |

## Supplementary VIP priority sequence

This pair changes payment timing/satisfaction; both paths end in defeat. It is supplementary to the selected verdict-flip scenario.

| Detail | Editable source | PNG pages |
| --- | --- | --- |
| sequence-1-start-host | [draw.io](sequence-1-start-host.drawio) | [1](png/sequence-1-start-host-1.png) · [2](png/sequence-1-start-host-2.png) · [3](png/sequence-1-start-host-3.png) |
| sequence-2-waiter-chef | [draw.io](sequence-2-waiter-chef.drawio) | [1](png/sequence-2-waiter-chef-1.png) · [2](png/sequence-2-waiter-chef-2.png) · [3](png/sequence-2-waiter-chef-3.png) |
| sequence-3-serve-pay | [draw.io](sequence-3-serve-pay.drawio) | [1](png/sequence-3-serve-pay-1.png) · [2](png/sequence-3-serve-pay-2.png) |
| sequence-4-result | [draw.io](sequence-4-result.drawio) | [1](png/sequence-4-result.png) |
| sequence-diagram | [draw.io](sequence-diagram.drawio) | [1](png/sequence-diagram-1.png) · [2](png/sequence-diagram-2.png) |

The diagrams were converted from the Stage 3 reference in `Documents/OOP`, then checked against the local Java. Factory calls use a factory participant; success/refusal alternatives and continued fragments remain editable. Selected sequence lifelines for multiple orders/customers are summaries; notes identify the concrete IDs. Optional refusal paths are not extra actions in the valid fixtures. Earlier Stage 2 figures remain in `../stage 2/`.


## Focused report edition

The report edition at [report/diagrams](../../report/diagrams) supplies the printable class and selected-sequence views. It uses end multiplicities, attached class connectors, selected member lists and at most eight lifelines per sequence panel. An explicit turn-loop context is repeated on continued panels, and the final result is outside the loop. These sources and exports supersede the older wide views for report placement; the older complete diagrams remain navigation/reference material and match the earlier recording. See [the rendered report](../../report/Restaurant_Rush_Group_Report.pdf), [panel index](../../report/FIGURE_APPENDIX.md) and [figure-size audit](../../report/evidence/figure-size-audit.json).
