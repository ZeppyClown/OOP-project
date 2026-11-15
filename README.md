# Restaurant Rush

Current implementation: **Stage 3**, extending the published Stage 2 code with Host, ComboMeal, Happy Hour and pricing policies.

```sh
sh "stage 3/run.sh"
sh "stage 3/run-tests.sh"
sh "stage 3/check-build.sh"
```

Installed JDK 17+ required; no dependencies downloaded.

[Stage 3 guide](stage%203/README.md) · [Member responsibilities](stage%203/MEMBER_FILES.json) · [Design discussion](stage%203/DESIGN.md) · [Transition inventory](stage%203/TRANSITION.md) · [Local evidence](stage%203/EVIDENCE.md) · [Diagrams](diagrams/stage%203/README.md)

| Stage | Runnable published source |
| --- | --- |
| 1 | [stage-1](https://github.com/ZeppyClown/OOP-project/tree/stage-1) |
| 2 | [stage-2](https://github.com/ZeppyClown/OOP-project/tree/stage-2) |
| 3 | [stage-3](https://github.com/ZeppyClown/OOP-project/tree/stage-3) |

[Published 1 → 2 comparison](https://github.com/ZeppyClown/OOP-project/compare/stage-1...stage-2). [Current 2 → 3 file inventory](stage%203/TRANSITION.md). [Published 2 → 3 comparison](https://github.com/ZeppyClown/OOP-project/compare/stage-2...stage-3).

The Stage 3 verdict-flip pair uses the same staff choices. Activate Happy Hour on turn 13: four paid, $49.50, 67.3, VICTORY. Never activate: four paid, $56.10, 57.8, DEFEAT. See [full scenario](stage%203/test-fixtures/FLIP_SCENARIO.md).

Member guides describe technical responsibilities. Personal reflections, approval/signatures, declarations, report PDF readability and final video evidence must come from the members and final submission artifacts.


## Source lineage

The implementation and diagram material carry forward Restaurant Rush work from the team’s earlier repository, [SC2002MACS2G2](https://github.com/abra0006-glitch/SC2002MACS2G2), which is the repository checked out locally as `Documents/OOP`. That material was integrated into [OOP-project](https://github.com/ZeppyClown/OOP-project), with local controller, testing, build-portability, documentation and diagram changes. The November restoration commits repair integration regressions in that existing work; they do not represent the first creation of every restored feature. Source lineage does not establish individual authorship or the extent of AI assistance: each member’s declaration and chronological records must describe their actual work and assistance consistently.

## Report and submission evidence

- [Group PDF report](report/Restaurant_Rush_Group_Report.pdf) and [editable report](report/GROUP_REPORT.md).
- [Printable UML sources and PNGs](report/diagrams/) and [figure index](report/FIGURE_APPENDIX.md).
- Reflections: [Victor](report/reflections/VICTOR.md), [Danielle](report/reflections/DANIELLE.md), [Abraham](report/reflections/ABRAHAM.md), [Talon](report/reflections/TALON.md). Members must confirm their own accounts.
- Complete transition path inventories: [1 to 2](docs/transitions/stage-1-to-stage-2.md), [2 to 3](docs/transitions/stage-2-to-stage-3.md).
- [AI-history index](ai-history/README.md). Each member must supply complete chronological records on their own branch; existing records are not certified complete.
- [Video inspection and remaining checks](report/VIDEO_REVIEW.md). The video is included in the separate submission candidate; this repository does not yet provide a public video link or final measured segment timestamps.

Before course submission: add the seven measured video timestamps required by Section 6.2, verify the submitted-tree launch and tag comparisons are recorded, and include the official signed course declarations. Publishing this tag does not certify those personal/submission items complete.
