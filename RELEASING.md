# Releasing HotPot

HotPot is released to Maven Central through a tag-driven GitHub Actions workflow. The same workflows also publish the standalone Docker image to Docker Hub as `techitung/hotpot`.

## Required GitHub Secrets

- `MAVEN_CENTRAL_USERNAME` - Sonatype Central Portal token username
- `MAVEN_CENTRAL_PASSWORD` - Sonatype Central Portal token password
- `SIGNING_KEY`
- `SIGNING_PASSWORD`
- `DOCKERHUB_USERNAME`
- `DOCKERHUB_TOKEN`

## Snapshot Releases

HotPot can publish `-SNAPSHOT` builds to Maven Central from the `main` branch through [`snapshot.yml`](.github/workflows/snapshot.yml). The same workflow publishes Docker images to Docker Hub.

Before using snapshot publishing, enable `SNAPSHOT` publishing for the `com.coloncmd` namespace in the Sonatype Central Portal.

### Snapshot Flow

1. Set `version=X.Y.Z-SNAPSHOT` in [`gradle.properties`](gradle.properties).
2. Push to `main`, or trigger the `Snapshot Release` workflow manually.
3. GitHub Actions checks whether `gradle.properties` ends with `-SNAPSHOT`.
4. If it does, the workflow runs:

```bash
./gradlew build publishToMavenCentral
```

5. After the Maven snapshot is published, the workflow builds [`standalone/Dockerfile`](standalone/Dockerfile) and pushes:
   - `techitung/hotpot:X.Y.Z-SNAPSHOT`
   - `techitung/hotpot:snapshot`
6. If `gradle.properties` is not a `-SNAPSHOT` version, the workflow exits without publishing.

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

7. After the Maven release is published, the workflow builds [`standalone/Dockerfile`](standalone/Dockerfile) and pushes:
   - `techitung/hotpot:vX.Y.Z`
   - `techitung/hotpot:latest`
8. After the release is published, bump [`gradle.properties`](gradle.properties) to the next development version such as `X.Y.(Z+1)-SNAPSHOT`.
