# Agent analysis

Run the configured Sonar agent analysis after writing files, before handing the change back.
Covers: `AGENTS.md`, `CLAUDE.md`.

## End-of-turn check
Pass every changed project-relative file in one command, at deep depth:

```shell
sonar analyze agentic --project SonarSource_sonar-kotlin --depth DEEP --file <path/to/file1> --file <path/to/file2>
```

When changed paths cannot be enumerated reliably, run `sonar analyze agentic --project SonarSource_sonar-kotlin --depth DEEP` on the Git change set. Per-edit checks may use `STANDARD`, but the final on-disk state requires `DEEP`. Do not run one command per file.

If the project is not configured or the connection is unavailable, report the skip reason and do not retry within the session. Report findings on changed lines, fix them and rerun; leave pre-existing findings on untouched lines out of scope. Do not suppress findings from the handoff.
