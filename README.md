# Restaurant Rush — Victor Stage 3 integration branch

This branch starts from the published Stage 2 baseline and moves the current source folder to `stage 3/`. Victor's Stage 3 control, IO, tests and diagrams are added in feature commits. Stage 1 and Stage 2 remain available through their existing tags.

Victor's Stage 3 changes depend on the corresponding Restaurant/recovery, Host/combo/pricing and console/harness changes from Danielle, Abraham and Talon. Until those changes are integrated, this branch is a member contribution, not a runnable Stage 3 release. The full integrated local implementation passed 55 checks and the copied-source/replay checks; that does not establish a standalone build of this partial branch.

Do not treat the inherited Stage 2 EVIDENCE/TRANSITION documents as Stage 3 verification. Final integration must update shared scripts, fixtures, test registration and release evidence, then run:

```sh
sh "stage 3/run-tests.sh"
sh "stage 3/check-build.sh"
```

Victor's selected sequence compares Happy Hour activation on turn 13 with never activating: the integrated fixture outcomes are four paid / $49.50 / 67.3 / VICTORY versus four paid / $56.10 / 57.8 / DEFEAT. The later diagram commit contains editable draw.io sources and PNG exports.
