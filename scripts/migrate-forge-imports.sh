#!/usr/bin/env bash
#
# migrate-forge-imports.sh - mechanical Forge -> NeoForge package/class renames.
#
# Applies ONLY the documented, deterministic 1:1 renames that are correct at the
# import level. It does NOT fix APIs that were semantically reworked (capabilities,
# networking payloads, fluid stacks, model loaders, RegistryObject, ForgeRegistries,
# crafting conditions) - those still need manual work and will remain as
# "cannot find symbol" errors after this runs. That is expected: this script's job
# is to clear the easy 60-70% so the real reworks stand out.
#
# Reference: NeoForge "Forge to NeoForge" primer. Package convention:
#   net.minecraftforge.eventbus.* -> net.neoforged.bus.*
#   net.minecraftforge.fml.*      -> net.neoforged.fml.*
#   net.minecraftforge.api.*      -> net.neoforged.api.*
#   net.minecraftforge.*          -> net.neoforged.neoforge.*   (common/client/event/fluids/items/network/registries/...)
# Plus class renames: MinecraftForge -> NeoForge, ForgeConfigSpec -> ModConfigSpec.
#
# Usage:  scripts/migrate-forge-imports.sh [dir]   (default: src/main/java)
# Idempotent. Review the diff before committing.

set -euo pipefail
TARGET="${1:-src/main/java}"

if [ ! -d "$TARGET" ]; then echo "No such dir: $TARGET" >&2; exit 1; fi

mapfile -t files < <(grep -rlE 'net\.minecraftforge|MinecraftForge|ForgeConfigSpec|new ResourceLocation' "$TARGET" --include='*.java' || true)
echo "Rewriting ${#files[@]} files under $TARGET ..."

for f in "${files[@]}"; do
  perl -0777 -pi -e '
    # Order matters: specific prefixes before the catch-all.
    s/\bnet\.minecraftforge\.eventbus\b/net.neoforged.bus/g;
    s/\bnet\.minecraftforge\.fml\b/net.neoforged.fml/g;
    s/\bnet\.minecraftforge\.api\b/net.neoforged.api/g;
    s/\bnet\.minecraftforge\b/net.neoforged.neoforge/g;
    # Class renames (after the package moves, only bare class usages remain).
    s/\bMinecraftForge\b/NeoForge/g;
    s/\bForgeConfigSpec\b/ModConfigSpec/g;
    # Vanilla 1.21: ResourceLocation constructors are private. Convert the simple
    # cases (no nested parens/commas in args); complex ones are left for manual fixing.
    s/\bnew\s+ResourceLocation\s*\(\s*([^(),]+?)\s*,\s*([^(),]+?)\s*\)/ResourceLocation.fromNamespaceAndPath($1, $2)/g;
    s/\bnew\s+ResourceLocation\s*\(\s*("[^"()]*"|[A-Za-z_][\w.]*)\s*\)/ResourceLocation.parse($1)/g;
  ' "$f"
done

echo "Done. Remaining net.minecraftforge references (should be 0):"
grep -rE 'net\.minecraftforge' "$TARGET" --include='*.java' -c 2>/dev/null | awk -F: '{s+=$2} END{print s+0}'
echo "Now fix the semantic reworks (see docs/forge-to-neoforge.md): capabilities,"
echo "networking, fluids, RegistryObject->DeferredHolder, ForgeRegistries, conditions, model loaders."
