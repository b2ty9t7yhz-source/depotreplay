# Distribution

DepotReplay has two delivery paths: host-native installers and a static browser build.

## Native installers

Run on the operating system you are packaging:

```bash
./gradlew :desktop:jpackageInstaller --no-configuration-cache --no-daemon
```

The task uses the Java 21 `jpackage` tool selected by Gradle and writes the installer plus `SHA256SUMS` under `desktop/build/jpackage/installer/`.

| Build host | Artifact | Notes |
| --- | --- | --- |
| macOS | PKG | Installs `DepotReplay.app` with a bundled runtime |
| Windows | MSI | Offers Start menu and desktop shortcut options |
| Linux | DEB | Adds a desktop/menu entry; `fakeroot` is required to build |

`jpackage` does not cross-compile, so each artifact is built on its target host. The `Distributions` workflow runs the three hosts independently and uploads their artifacts. The packaged application starts with the built-in `city-grid` scenario, so it does not depend on the repository checkout.

macOS package versions must begin with a nonzero component. While the Gradle project is at `0.x.y`, the macOS package version is encoded as `x.y`; other hosts use the Gradle project version directly.

The repository has no Apple Developer ID, Microsoft code-signing certificate, or Linux package-signing key. Produced installers are therefore unsigned. Operating systems may display an unknown-publisher warning. A public release should configure signing, and macOS distribution should add notarization, before the installers are presented as trusted production downloads.

## Browser build

Build the static site:

```bash
./gradlew :web:webDist --no-configuration-cache --no-daemon
```

Serve it over HTTP for local testing:

```bash
python3 -m http.server 8000 --directory web/build/site
```

Open `http://localhost:8000`. Do not open `index.html` directly from the file system; browser security rules can block generated assets.

Everything under `web/build/site/` is static and can be copied to GitHub Pages, Netlify, Cloudflare Pages, an object-storage website, or a normal web server. The manual `Deploy browser game to GitHub Pages` workflow builds and deploys the same directory. Repository settings must select **GitHub Actions** as the Pages source before its first run.

The browser build shares game rules, rendering, keyboard controls, and baselines with desktop. Browser sandboxing intentionally leaves file-backed save/load and replay export in the desktop application.

The gdx-teavm 1.6.1 plugin currently emits Gradle 9 deprecation notices for its internal source-dependency declarations. The Gradle 9.7 build passes; moving this module to Gradle 10 requires an upstream-compatible plugin release or a replacement integration.
