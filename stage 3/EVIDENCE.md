# Stage 3 local verification

Checked 14 November 2026, installed JDK 17 at `/opt/homebrew/opt/openjdk@17`. Baseline HEAD: `4711c313b455a0d88fb14553c936b0e25560b579` (published Stage 2). Results below are for uncommitted local Stage 3 changes, not a Stage 3 commit/tag.

```sh
JAVA_HOME=/opt/homebrew/opt/openjdk@17 sh "stage 3/run-tests.sh"
JAVA_HOME=/opt/homebrew/opt/openjdk@17 sh "stage 3/check-build.sh"
```

- Full suite: **55 passed, 0 failed, 55 total**, exit 0. Includes 18 Stage3ControllerTests scenarios, recovery, aggregate ownership/order wrappers, Host/combo/pricing, inherited timing and real console replays.
- Build check: exit 0; plain copied source and source with spaces/quotes/backslash each pass the suite, seven CLI replays, redirected stdin, activation prompt/duration checks and EOF in all four input roles. Installed JDK on PATH without JAVA_HOME passes; invalid JAVA_HOME gives the expected actionable error.
- 55 includes the reference's 50 registered checks plus the existing local ControlTests wrapper and four extra console checks. Registered checks wrap many internal assertions/scenarios.

| Actual CLI fixture | Paid | Revenue | Average paid satisfaction | Result |
| --- | ---: | ---: | ---: | --- |
| victory / victory-invalid | 5 | $73.38 | 89.4 | VICTORY |
| defeat | 0 | $0.00 | N/A | DEFEAT |
| vip-priority | 1 | $6.48 | 100.0 | DEFEAT |
| regular-priority | 1 | $6.48 | 95.0 | DEFEAT |
| flip-happy-hour-on | 4 | $49.50 | 67.3 | VICTORY |
| flip-happy-hour-never | 4 | $56.10 | 57.8 | DEFEAT |

Happy Hour displays remaining 2 at activation, 1 on the next turn, then 0. Activation prompts stop after use. EOF reports Happy Hour, Host, Waiter or Chef and produces no final verdict. Earlier changes in the interrupted turn are preserved.

Reference code: `/Users/victorchua/Documents/OOP/stage-3`. Stage 3 features were adapted into the existing Stage 2 implementation; local test additions, build portability and exact cents arithmetic are retained. Compiled-production comparison with `javap -p -c -constants`: 37 of 38 classes are identical to the reference. Order differs only in the inherited validation-message spacing; its logic is identical. See TRANSITION for the logical file inventory and members/ for responsibility mappings.

Claude independent code review is requested after this verification; its result is separate from the test results above. Member personal reflections/approval, final report PDF readability, video and a published Stage 3 release remain outside this local code verification.

## Diagram format update

All 15 Stage 3 diagrams now use native editable draw.io shapes and PNG exports (15 draw.io files, 34 PNG pages). XML page counts and export counts are checked; original Mermaid/SVG copies were replaced. Visual checks cover class relationships, message direction, frame continuation and label/arrow separation. This conversion changes no gameplay code. Claude reviewed the conversion and identified incorrect continued-alt guards, remaining combined calls and class-editing limitations. These were corrected locally: continued fragments now carry their active branch; calls are separated; class rows are children and connectors have source/target attachments. Duplicate continuation markers and stale cross-references are corrected too. The corrected files were structurally and visually checked locally; no new Claude approval is claimed.
