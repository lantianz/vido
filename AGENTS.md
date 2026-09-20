# Vido Repository Guide

## Scope

- This is a native Android app using Kotlin, Jetpack Compose, Media3 and Gradle Kotlin DSL.
- Keep changes scoped to the requested behavior. Preserve local-only operation and do not add network permissions unless explicitly requested.
- Do not commit local SDK configuration, generated builds, signing keys, IDE state or release artifacts.

## Verification

- Use JDK 17 for Gradle tasks.
- Do not run a full build unless the request requires a build artifact, CI verification, or a build-related fix.
- When delivering an APK, increment both Android `versionCode` and `versionName`.

## Commits And Releases

- Read `.agents/skills/commit-convention/SKILL.md` before preparing commits or release tags.
- Read `.agents/skills/release-publish/SKILL.md` before any release-related operation.
- Keep commits focused. Use Conventional Commit messages and label Pull Requests so GitHub Release Notes can categorize them.
- Release tags must be semantic and prefixed with `v`, for example `v1.2.3` or `v1.2.3-beta.1`.
- AI may create commits and push ordinary branches only when the user explicitly requests the synchronization. It must not create, push, move, delete, or recreate a release tag unless the user explicitly names the version to publish.
- Before an authorized release, verify a clean worktree, that `main` is synchronized, and that the exact remote tag does not already exist. After pushing the tag, verify the GitHub Actions run and Release attachment.
- On a failed release, inspect the failure first. Do not force-push, retag, or delete a Release/tag as a retry; require explicit user authorization for destructive release recovery.
