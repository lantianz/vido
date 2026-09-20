---
name: commit-convention
description: Prepare focused Conventional Commits and release tags for the Vido Android repository, including the labels required for generated GitHub Release Notes.
---

# Vido Commit Convention

Use this skill when creating commits, preparing a Pull Request, or creating a release tag in this repository.

## Commit Messages

Use one focused Conventional Commit per logical change:

```text
<type>(<optional scope>): <short imperative summary>
```

Allowed types are `feat`, `fix`, `docs`, `chore`, `build`, `ci`, `refactor`, and `test`.

- Use `feat` for user-visible features and `fix` for bug fixes.
- Use `ci` for GitHub Actions changes and `build` for Gradle, dependency or signing configuration.
- Do not mix unrelated refactors, generated files, APKs, local SDK configuration, or signing material into a commit.

## Pull Request Labels

GitHub Release Notes categories are label-driven. Add one relevant label before merging:

| Change type | Preferred label |
| --- | --- |
| New feature | `feature` or `enhancement` |
| Bug fix | `bug` or `fix` |
| Documentation | `documentation` |
| Maintenance or dependency update | `chore` or `dependencies` |
| Exclude from notes | `skip-changelog` |

Conventional Commit prefixes do not automatically apply GitHub labels. Add labels manually unless a separate labeler workflow is introduced.

## Releases

Release tags must use `v<semantic-version>`, such as `v1.2.3` or `v1.2.3-beta.1`. Pushing one triggers the release workflow. Tags that contain `-` create prerelease GitHub Releases.
