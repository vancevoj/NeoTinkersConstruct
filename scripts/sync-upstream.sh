#!/usr/bin/env bash
#
# sync-upstream.sh - keep this NeoForge 1.21.1 fork in sync with upstream
# SlimeKnights/TinkersConstruct.
#
# What it does:
#   1. Ensures the `upstream` remote exists and points at SlimeKnights.
#   2. Fetches upstream.
#   3. Fast-forwards / merges the pristine reference branch (default 1.20.1).
#   4. Detects when upstream publishes NEW branches - in particular a future
#      1.21 / NeoForge branch, which is the signal to switch this port over to
#      tracking official directly.
#   5. Prints a summary of what changed upstream so you can cherry-pick the
#      relevant feature/bugfix commits into the live port branch (1.21).
#
# Usage:
#   scripts/sync-upstream.sh                # sync the 1.20.1 reference branch
#   scripts/sync-upstream.sh <ref-branch>   # sync a different reference branch
#
# This script never touches your live port branch automatically; porting a
# vanilla-1.20.1 change into 1.21 is a manual cherry-pick because the two trees
# diverge (different Minecraft version + loader). See UPGRADING.md.

set -euo pipefail

UPSTREAM_URL="https://github.com/SlimeKnights/TinkersConstruct.git"
REF_BRANCH="${1:-1.20.1}"
PORT_BRANCH="1.21"

say()  { printf '\033[1;36m==>\033[0m %s\n' "$*"; }
warn() { printf '\033[1;33m!! \033[0m %s\n' "$*"; }
ok()   { printf '\033[1;32m ok\033[0m %s\n' "$*"; }

# 1. Ensure the upstream remote.
if ! git remote get-url upstream >/dev/null 2>&1; then
  say "Adding upstream remote -> $UPSTREAM_URL"
  git remote add upstream "$UPSTREAM_URL"
else
  current="$(git remote get-url upstream)"
  if [ "$current" != "$UPSTREAM_URL" ]; then
    warn "upstream remote is $current (expected $UPSTREAM_URL); leaving as-is"
  fi
fi

# Snapshot upstream branch list BEFORE fetching so we can diff for new ones.
before="$(git branch -r --list 'upstream/*' | sed 's/^[ *]*//' | sort || true)"

say "Fetching upstream (branches + tags)..."
git fetch --prune --tags upstream

after="$(git branch -r --list 'upstream/*' | sed 's/^[ *]*//' | sort)"

# 4. Detect new upstream branches.
new_branches="$(comm -13 <(printf '%s\n' "$before") <(printf '%s\n' "$after") || true)"
if [ -n "$new_branches" ]; then
  warn "NEW upstream branches appeared:"
  printf '%s\n' "$new_branches" | sed 's/^/      /'
  if printf '%s\n' "$new_branches" | grep -qiE '1\.21|neoforge|neo'; then
    warn "One of these looks like a 1.21 / NeoForge branch."
    warn "That is the trigger to start tracking official directly - see UPGRADING.md, section 'When upstream ships 1.21'."
  fi
fi

# 3. Update the reference branch.
if git show-ref --verify --quiet "refs/heads/$REF_BRANCH"; then
  say "Updating reference branch '$REF_BRANCH' from upstream/$REF_BRANCH"
  start_branch="$(git rev-parse --abbrev-ref HEAD)"
  git switch "$REF_BRANCH"
  if git merge --ff-only "upstream/$REF_BRANCH" 2>/dev/null; then
    ok "Fast-forwarded $REF_BRANCH."
  else
    warn "Cannot fast-forward (local commits on $REF_BRANCH). Merging instead."
    git merge --no-edit "upstream/$REF_BRANCH"
  fi
  git switch "$start_branch"
else
  warn "Local branch '$REF_BRANCH' not found; skipping reference update."
fi

# 5. Summary of upstream commits not yet reflected in the port branch.
if git show-ref --verify --quiet "refs/heads/$PORT_BRANCH"; then
  say "Upstream $REF_BRANCH commits not yet in $PORT_BRANCH (cherry-pick candidates):"
  count="$(git rev-list --count "$PORT_BRANCH..upstream/$REF_BRANCH" 2>/dev/null || echo 0)"
  if [ "$count" = "0" ]; then
    ok "Port branch is up to date with upstream $REF_BRANCH content."
  else
    echo "    $count upstream commit(s) ahead. Most recent:"
    git --no-pager log --oneline -15 "$PORT_BRANCH..upstream/$REF_BRANCH" | sed 's/^/      /'
    echo
    echo "    Cherry-pick a specific fix into the port with:"
    echo "      git switch $PORT_BRANCH && git cherry-pick <hash>"
    echo "    (expect to resolve conflicts; the 1.21 tree has diverged from 1.20.1)"
  fi
fi

ok "Sync complete."
