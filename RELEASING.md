# Releasing HotPot

HotPot is released to Maven Central through a tag-driven GitHub Actions workflow.

## Required GitHub Secrets

- `MAVEN_CENTRAL_USERNAME` - Sonatype Central Portal token username
- `MAVEN_CENTRAL_PASSWORD` - Sonatype Central Portal token password
- `SIGNING_KEY`
- `SIGNING_PASSWORD`
- `SIGNING_KEY_ID` (optional)

## Release Steps

1. Generate a Sonatype Central Portal user token in your Sonatype account if you have not already.
2. Set `version=X.Y.Z` in [`gradle.properties`](gradle.properties).
3. Commit and push that version change.
4. Create and push a matching tag:

```bash
git tag vX.Y.Z
git push origin vX.Y.Z
```

5. GitHub Actions validates that:
   - the tag format is exactly `vX.Y.Z`
   - `gradle.properties` is not a `-SNAPSHOT`
   - the tag version matches `gradle.properties`
6. If validation passes, the release workflow runs:

```bash
./gradlew build publishAndReleaseToMavenCentral
```

7. After the release is published, bump [`gradle.properties`](gradle.properties) to the next development version such as `X.Y.(Z+1)-SNAPSHOT`.
