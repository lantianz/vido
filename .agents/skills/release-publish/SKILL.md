---
name: release-publish
description: Publish a Vido Android GitHub Release from an explicitly requested semantic version tag, with preflight checks and post-release verification.
---

# Vido Release Publishing

Use this skill only when the user explicitly asks to publish a named Vido release version. Read `commit-convention` first when a commit is needed before the release.

## Preconditions

- Confirm the requested version follows semantic versioning and form the tag as `v<version>`.
- A tag containing `-` is a prerelease. Do not infer prerelease status from the version number alone.
- Confirm the worktree is clean and the target commit is already on `main` and synchronized with `origin/main`.
- Query the remote tag before creating it. Stop when that exact tag already exists; never overwrite it or assume it can be reused.
- The GitHub Actions release workflow builds the debug APK and injects the tag version into Android `versionName`. Do not edit `app/build.gradle.kts` merely to publish a tag.

## Authorized Release Flow

1. Run `git status --short`, fetch remote tags, and check `git ls-remote --tags origin "v<version>"`.
2. Push pending authorized `main` commits before tagging. Do not include build outputs, signing keys, local SDK configuration, or APK files.
3. Create an annotated tag: `git tag -a v<version> -m "v<version>"`.
4. Push only that tag: `git push origin v<version>`.
5. Verify the `Release Android APK` Actions run completes, then verify the GitHub Release title, generated notes, prerelease state, and APK attachment.

## Failure Handling

- For a build or test failure, inspect the Actions log, fix the cause in a new commit, and publish a new version only after user authorization.
- Do not delete, retarget, force-push, or recreate a release tag as an implicit retry.
- Manual workflow dispatch is a build-only diagnostic path. It does not create a GitHub Release.
