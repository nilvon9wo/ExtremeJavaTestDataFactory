# Publishing to Maven Central

Coordinates: **`io.github.nilvon9wo:xfty`** and **`io.github.nilvon9wo:xfty-jpa`**
(the `net.nowhereatall.xfty` package name is unrelated to the group id — Central
verifies the group id namespace, not the package).

The namespace `io.github.nilvon9wo` is verified on the
[Central Portal](https://central.sonatype.com/) against the GitHub account
(no DNS).

## Why not OIDC "trusted publishing"?

The prompt asked to follow the same reasoning as this project's C# sibling,
whose NuGet workflow uses **OIDC trusted publishing** — GitHub Actions exchanges
a short-lived OIDC token for a temporary API key, so there is no long-lived
secret in the repo.

**Maven Central does not offer an equivalent (as of 2026-09).** NuGet, npm and
PyPI have trusted publishing; the Central Portal took a different path —
[it now validates Sigstore signatures](https://www.sonatype.com/blog/central-publisher-portal-now-validates-sigstore-signatures),
i.e. keyless signing via GitHub OIDC → Fulcio, but the **upload itself still
authenticates with a Portal token**. There is no "register this GitHub repo as a
trusted publisher" flow.

So the workflow here is as close to that reasoning as the platform currently
allows:

- **Upload auth:** a Central Portal **user token** (revocable, scoped to
  publishing, not a password) in two repo secrets. This is the one unavoidable
  stored credential.
- **Signing:** an in-memory PGP key, also a secret, for now.
- `publish.yml` already declares `permissions: id-token: write`, so the move to
  **keyless Sigstore signing** (dropping the PGP secret) is a
  build-config change only when the Gradle tooling for Portal + Sigstore
  bundles matures. Tracked as the next publishing task.

## Repo secrets to set

| Secret | What it is |
|---|---|
| `MAVEN_CENTRAL_USERNAME` | Portal token *username* — [central.sonatype.com](https://central.sonatype.com/) → your name → **View Account** → **Generate User Token** |
| `MAVEN_CENTRAL_PASSWORD` | Portal token *password* from the same screen |
| `GPG_SIGNING_KEY` | ASCII-armored private key: `gpg --armor --export-secret-keys <KEYID>` (whole block, including the BEGIN/END lines) |
| `GPG_SIGNING_PASSWORD` | passphrase for that key (empty string if none) |

The public half of the GPG key must be pushed to a keyserver
(`gpg --keyserver keyserver.ubuntu.com --send-keys <KEYID>`).

## How a release happens

1. Bump `version` in `gradle.properties` (or just tag — the workflow derives the
   version from the tag and passes `-Pversion=`).
2. `git tag v0.1.0 && git push origin v0.1.0`.
3. `publish.yml` builds, tests, bundles every module into one deployment, and
   uploads it to the Portal as a **draft** (`publishingType = "USER_MANAGED"`).
4. Review the deployment at
   [central.sonatype.com/publishing/deployments](https://central.sonatype.com/publishing/deployments)
   and click **Publish**. Flip `publishingType` to `"AUTOMATIC"` in
   `settings.gradle.kts` once the pipeline is trusted.

## Local dry run

```bash
./gradlew build nmcpZipAggregation
unzip -l build/nmcp/zip/aggregation.zip     # inspect the bundle, no upload
./gradlew publishToMavenLocal                # install into ~/.m2 for a downstream test
```
