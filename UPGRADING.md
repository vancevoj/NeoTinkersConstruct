# Keeping this fork in sync with upstream Tinkers' Construct

This NeoForge 1.21.1 port is a derivative of [SlimeKnights/TinkersConstruct].
Upstream is still on **1.20.1 / Forge** and has not shipped a 1.21 or NeoForge
build as of this writing. This document explains how to pull upstream changes in
over time, both now (while upstream is 1.20.1) and later (once upstream ships
its own 1.21 / NeoForge branch).

## Branch layout

| Branch   | Purpose                                                                 |
|----------|-------------------------------------------------------------------------|
| `1.20.1` | Reference mirror of upstream `1.20.1`. Touch as little as possible so it stays a clean tracking branch. |
| `1.21`   | The live NeoForge 1.21.1 port. All porting work lands here. **Default working branch.** |

Remotes:

| Remote     | URL                                              |
|------------|--------------------------------------------------|
| `origin`   | `https://github.com/vancevoj/NeoTinkersConstruct`|
| `upstream` | `https://github.com/SlimeKnights/TinkersConstruct`|

`upstream` is added automatically by the sync script if missing.

## Routine sync (while upstream is still 1.20.1)

Upstream still ships occasional 1.20.1 bug fixes and tweaks. To pull them and see
what is worth carrying into the port:

```bash
scripts/sync-upstream.sh
```

This fetches upstream, fast-forwards the `1.20.1` reference branch, flags any new
upstream branches, and lists the upstream commits not yet reflected in `1.21`.

To carry a specific upstream fix into the port:

```bash
git switch 1.21
git cherry-pick <hash>      # from the list the script prints
# resolve conflicts: 1.21 has diverged (data components, NeoForge APIs, renames)
```

Cherry-pick rather than merge. The `1.21` tree has diverged from `1.20.1` (new
Minecraft version, new loader, data components), so a blind merge produces large
conflict storms. Picking individual feature/bugfix commits keeps conflicts local
and reviewable. Prefer picking **logic/recipe/balance** changes; skip commits
that are purely Forge-API or 1.20.1-rendering plumbing, since the port already
replaced those.

## When upstream ships 1.21 / NeoForge

This is the event the whole "track official" plan is waiting for. `sync-upstream.sh`
watches for it: when a new `upstream/1.21*` or `*neoforge*` branch appears, it prints
a loud warning. When that happens:

1. **Evaluate it.** Official will be more correct and complete than any community
   port. Decide whether to (a) rebase this fork's *own* additions on top of the
   official 1.21 branch, or (b) retire this port and adopt official directly,
   keeping only your custom changes as a thin patch set.
2. Add it as a tracked branch:
   ```bash
   git fetch upstream
   git switch -c 1.21-official upstream/1.21        # name may differ
   ```
3. Diff your port against it to see what is genuinely yours vs. what official now
   provides for free:
   ```bash
   git diff 1.21-official 1.21 -- src/main/java | less
   ```
4. Migrate only your real customizations forward. Everything official now covers
   can be dropped from your maintenance burden.

## The Mantle dependency

Tinkers' Construct depends on **Mantle** (`SlimeKnights/Mantle`), which also has
no 1.21 / NeoForge release. This port is built against a sibling fork,
`vancevoj/Mantle` (branch `1.21`), wired in as a Gradle
[composite build](https://docs.gradle.org/current/userguide/composite_builds.html).
Mantle has its own `scripts/sync-upstream.sh` following the same model. Sync and
upgrade Mantle the same way, and keep the two ports moving together: TConstruct
will not build until the Mantle APIs it uses exist for 1.21.1.

See [`PORTING.md`](PORTING.md) for the full porting roadmap and order of
operations, and [`PORTING_STATUS.md`](PORTING_STATUS.md) for the current state of
the work and the next concrete steps.

[SlimeKnights/TinkersConstruct]: https://github.com/SlimeKnights/TinkersConstruct
