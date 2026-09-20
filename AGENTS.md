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
- Keep commits focused. Use Conventional Commit messages and label Pull Requests so GitHub Release Notes can categorize them.
- Release tags must be semantic and prefixed with `v`, for example `v1.2.3` or `v1.2.3-beta.1`.
