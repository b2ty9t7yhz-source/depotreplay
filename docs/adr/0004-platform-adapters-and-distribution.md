# ADR 0004: Share the game adapter and build platform-specific distributions

- Status: Accepted
- Date: 2026-08-16

## Context

DepotReplay needs a native desktop experience and a link that can be played without installing Java. Copying the libGDX screen into separate desktop and browser modules would allow controls, score presentation, or dispatch behavior to drift. A single native package cannot run on every operating system because `jpackage` creates host-specific launchers and bundled runtimes.

## Decision

Keep rendering and input in a shared `game` module. The `desktop` and `web` modules provide only platform launchers and capabilities:

- `desktop` uses LWJGL3, file-backed save/load and replay export, PNG capture, and `jpackage`;
- `web` uses gdx-teavm, a built-in validated scenario, and a static HTML/JavaScript shell;
- `core` remains headless and owns every simulation rule.

Build PKG, MSI, and DEB artifacts on macOS, Windows, and Linux runners respectively. Each installer contains a reduced Java 21 runtime and a SHA-256 checksum. Build the browser version as static files that can be served by GitHub Pages or any ordinary static host.

## Consequences

- Desktop and browser runs share the same rendering, controls, engine, strategies, and score code.
- Native installers must be produced on their target operating system; the GitHub Actions matrix performs those builds.
- The browser build is immediately playable but does not expose desktop file save/load or replay export.
- The repository does not contain commercial signing identities. Locally built and CI-built installers are unsigned until a maintainer configures platform signing and notarization outside this project.
