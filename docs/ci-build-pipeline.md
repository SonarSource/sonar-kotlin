# CI build pipeline

The [Build workflow](../.github/workflows/build.yml) builds, tests, analyzes and publishes the plugin, runs the QA suites and promotes the build.
Performance validation runs in the private [sonar-kotlin-pvf](https://github.com/SonarSource/sonar-kotlin-pvf) repository, because a public repository cannot call the private PVF reusable workflows.
It is triggered from here on pushes to the default branch, and on demand on a PR.
Covers: `.github/workflows/build.yml`, `.github/workflows/pvf-comment.yml`.

## Entry points and controls

Build runs on pushes to `master` and `branch-*`, pull requests, manual dispatches and a daily schedule at 02:00 UTC.
A new run cancels an in-progress run for the same PR or ref, except on the default branch.
There, each commit gets its own concurrency group, so runs neither cancel nor wait for each other.

| Control | Effect |
| --- | --- |
| `/pvf` PR comment | Runs performance validation for the PR, see below. |
| `skip-pvf` PR label or `[skip pvf]` in the PR title | Skips the performance validation of the default-branch push that merges the PR. Release automation and the [rule-metadata update workflow](../.github/workflows/rule-metadata-update.yml) add the label to their rule-metadata PRs. |

## Jobs

| Job | Responsibility |
| --- | --- |
| `build` | Builds and tests the plugin, runs the Sonar analysis, deploys to the QA repository and exposes `project-version`. |
| `prepare_pvf` | After a successful `build`, uploads the deployed plugin version as the `candidate-version` artifact. It is a job of its own, so its failure can't fail the build or the promotion. |
| `qa_plugin`, `qa_ruling`, `qa_sq_integration` | Plugin, ruling and SonarQube integration suites. See [Integration tests](integration-tests.md). |
| `ruling_update_notify` | After `qa_ruling`, runs the shared ruling update action, except on PRs from forks. |
| `trigger_performance_validation` | Default-branch pushes only, see below. |
| `promote` | Promotes the build. Fails if `build` or a QA suite failed. |
| `notify_slack` | Reports failures on the default branch, `branch-*` and `feature/*` when the run is not cancelled. |

Neither `prepare_pvf` nor the performance validation trigger is a dependency of `promote` or `notify_slack`.

## Performance validation

`prepare_pvf` runs on every event, as both performance validation triggers read its `candidate-version` artifact.

`trigger_performance_validation` runs on pushes to the default branch only, after a successful `build`.
It resolves the baseline here, as sonar-kotlin-pvf cannot read this repository's artifacts, with the shared [`pvf-resolve-baseline`](https://github.com/SonarSource/core-languages-tooling-public/tree/master/pvf-resolve-baseline) action:

- It skips validation when the merged PR opts out (see the controls above), or when the push only changes rule metadata, `sonarpedia.json` or `.md` files.
- Otherwise the baseline is the `candidate-version` artifact of the previous commit's Build run.
  While that run's `prepare_pvf` job hasn't finished, it waits up to 45 minutes.
  A skipped or failed `prepare_pvf` stops the wait at once.
- It falls back to the newest earlier default-branch run that has the artifact.
  Without a baseline it skips validation with a warning.

With a baseline, the job sets a pending `performance-validation` commit status and sends a `repository_dispatch` to sonar-kotlin-pvf.
The dispatch carries the baseline, the candidate version and the report name, `<sha> <subject> (#<PR>)`, and the dashboard index lists the report as a master push.
sonar-kotlin-pvf reports the result back as the `performance-validation` commit status.
On a regression, a benchmark error or a failed validation, it also comments on the merged PR and mentions whoever merged it.
If the baseline couldn't be resolved (e.g. the GitHub API kept failing), the trigger job itself posts that comment, with the shared [`pvf-notify-merger`](https://github.com/SonarSource/core-languages-tooling-public/tree/master/pvf-notify-merger) action.

On a PR, a comment starting with `/pvf` from a user with write access to the repository (optionally followed by rules, e.g. `/pvf S1234, S5678`) triggers [`pvf-comment.yml`](../.github/workflows/pvf-comment.yml).
It dispatches the validation in sonar-kotlin-pvf with the PR's `candidate-version` artifact as the candidate and `DEV` as the baseline, and sonar-kotlin-pvf comments the dashboard link on the PR.
The dashboard index lists the report as a pull request run.
The PR's Build run must have finished `prepare_pvf` first.
A member or collaborator without write access, or a missing `candidate-version` artifact, gets a reply on the PR explaining why nothing was dispatched.

The nightly run in sonar-kotlin-pvf at 05:10 UTC compares `DEV` with `LATEST_RELEASE`.
Performance Slack notifications are enabled only for scheduled runs.
