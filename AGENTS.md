# AGENTS.md — AMD Patch for Voxy

> **Purpose:** Compatibility patch for [Voxy](https://github.com/MCRcortex/voxy) that fixes
> flickering LOD chunks on AMD GPUs (especially Polaris / RX 500 series).
>
> **Maintainer persona:** Act as a senior Java programmer and experienced Minecraft
> Fabric mod developer. Every change must keep the codebase readable, easy to
> understand, and easy to maintain.

---

## Project Overview

Voxy uses a **Hierarchical Z-Buffer (HiZ)** compute shader to cull invisible LOD
chunks. On AMD Polaris GPUs (e.g. RX 580) the driver returns bogus near-zero
values for `texelFetch` on depth textures. This corrupts the `REDUCTION`
(min/max) operation, causing chunks to flicker as the camera moves.

This mod patches the problem in two ways:

1. **Prevents a startup crash** — Voxy's `Capabilities` class detects the broken
   sampler and throws `IllegalStateException("it bork, amd is bork")`. We bypass
   that test so the game can load.
2. **Fixes the shader** — We intercept the HiZ traversal compute-shader source
   before Voxy compiles it, and inject a clamp that forces buggy near-zero
   depth reads to `1.0f` (far plane). This makes the culling more conservative
   (render rather than incorrectly cull).

---

## Architecture

```
src/main/java/com/amdfix/
├── AMDPatchForVoxy.java                 # Mod entrypoint + shared logger
├── patch/
│   └── ShaderPatchUtil.java             # Centralized patch constants & logic
└── mixin/voxy/
    ├── CapabilitiesMixin.java           # Bypasses the AMD crash test
    ├── HierarchicalOcclusionTraverserMixin.java   # PRIMARY shader patch
    └── ShaderLoaderMixin.java           # FALLBACK shader patch (harmless)

src/client/java/com/amdfix/client/
└── AMDPatchForVoxyClient.java           # Client-side entrypoint

src/main/resources/
├── fabric.mod.json                      # Mod metadata, depends, mixins
├── amd-patch-for-voxy.mixins.json       # Common mixins (CapabilitiesMixin)
├── amd-patch-for-voxy.client.mixins.json # Client-only mixins (shader patches)
└── assets/amd-patch-for-voxy/icon.png
```

### Why two shader mixins?

| Mixin | Role | Rationale |
|-------|------|-----------|
| `HierarchicalOcclusionTraverserMixin` | **Primary** | Intercepts the shader string inside `lateStageCompile()`, the exact point where Voxy assembles the compute shader. This is reliable because `#import` directives are already resolved. |
| `ShaderLoaderMixin` | **Fallback** | Intercepts `ShaderLoader.parse()` for any shader that happens to contain the target line. If Voxy refactors its loading pipeline, this may catch the patch. It is idempotent — if the primary mixin already patched the string, this one does nothing. |

**Never remove the primary mixin.** The fallback is optional insurance.

---

## The Patch Logic (single source of truth)

All patch constants and the replacement logic live in:

```
com.amdfix.patch.ShaderPatchUtil
```

```java
public static final String ORIGINAL_LINE =
    "float sp = texelFetch(hizDepthSampler, ivec2(x, y), ml).r;";

public static final String PATCHED_CODE = """
    float sp = texelFetch(hizDepthSampler, ivec2(x, y), ml).r;
    if (sp <= 0.2f) {
        sp = FAR;
    }
    """;
```

### Threshold rationale (`0.2f`)

The value `0.2f` (compared against the **raw** sample) was chosen to cover:
- Sky / empty pixels where depth reads as `0.0` (driver bug)
- Close-up walls that are legitimate near-plane values
- Driver noise on Polaris hardware

**If you change this value, update the Javadoc in `ShaderPatchUtil` and the README.**

### Why `FAR` instead of a literal `1.0f`

Voxy's `voxy:util/depthutils.glsl` defines the depth convention two ways via the
`USE_REVERSE_Z` compile flag:

| | Standard depth | Reverse-Z (Minecraft 26.2) |
|-------------|----------------|-----------------------------|
| `NEAR`      | `0.0`          | `1.0`                       |
| `FAR`       | `1.0`          | `0.0`                       |
| `REDUCTION` | `max`          | `min`                       |

Hardcoding `sp = 1.0f` only means "far plane" under **standard** depth. Under
**reverse-Z** it pins bogus reads to the **near** plane, so the `min` reduction
marks every distant region as occluded and **all LOD sections vanish**. The AMD
driver returns raw near-zero values regardless of convention, so snapping
near-zero samples to the `FAR` macro is correct in both cases and always errs
toward rendering. `FAR` is in scope because `screenspace.glsl` imports
`depthutils.glsl` before the patched line.

**Do not replace `FAR` with a numeric literal** — it will break under whichever
depth convention it wasn't written for.

---

## Updating Minecraft / Fabric Versions

1. Edit `gradle.properties`:
   - `minecraft_version`
   - `loader_version`
   - `loom_version`
   - `fabric_api_version`
2. Edit `fabric.mod.json`:
   - `depends.minecraft`
   - `depends.fabricloader`
   - `depends.java`
3. Run `./gradlew clean build` and test.

**Do NOT add `mappings loom.officialMojangMappings()`** unless you know the
environment is obfuscated. The template this project is based on uses raw MCP
names and adding official mappings will break the build.

---

## What to Touch When Voxy Updates

### Scenario A: Voxy changes the exact GLSL line

1. Open `ShaderPatchUtil.java`.
2. Update `ORIGINAL_LINE` to match the new Voxy source.
3. Update `PATCHED_CODE` accordingly.
4. Run `./gradlew clean build` and test in-game.

**You only need to edit ONE file.**

### Scenario B: Voxy restructures shader loading

1. Check if `HierarchicalOcclusionTraverser.lateStageCompile()` still exists.
   - If yes, verify the method descriptor hasn't changed.
   - If no, find the new location where the HiZ compute shader is compiled
     and retarget `HierarchicalOcclusionTraverserMixin`.
2. Keep `ShaderLoaderMixin` as fallback — it may continue working.

### Scenario C: Voxy removes the `Capabilities` crash test

- If Voxy no longer calls `testDepthSampler()`, `CapabilitiesMixin` becomes a
  no-op. You can leave it; it does no harm.

---

## Code Style & Maintainability Rules

**Every agent working on this codebase MUST follow these rules:**

1. **Single source of truth**
   - Never duplicate `ORIGINAL_LINE` or `PATCHED_CODE` in multiple files.
   - If a value is needed in two mixins, extract it to `ShaderPatchUtil`.

2. **Descriptive naming**
   - Mixin methods: `amdPatch$<action><Subject>()` (e.g. `amdPatch$patchTraversalShader`)
   - Constants: `SCREAMING_SNAKE_CASE` with explanatory Javadoc
   - Classes: `NounMixin` for mixins, `NounUtil` for utilities

3. **Javadoc every public / package-private element**
   - Explain **why** the code exists, not just what it does.
   - Reference related classes (e.g. "see {@link HierarchicalOcclusionTraverserMixin}").

4. **Fail gracefully**
   - If the target line is missing, log a **warning** and return the original
     source unchanged. Never crash because Voxy updated.

5. **Minimal diffs**
   - When editing, change the smallest possible surface area.
   - Do not reformat entire files unless explicitly asked.

6. **No magic numbers / strings without context**
   - Thresholds, identifiers, and method descriptors must be constants with
     documentation.

7. **Preserve the template structure**
   - Do not rename `src/main` / `src/client` source sets.
   - Do not move `fabric.mod.json` or mixin config files.

---

## Build & Test

```bash
# Windows PowerShell
$env:JAVA_HOME = "C:\Users\<you>\AppData\Roaming\PrismLauncher\java\java-runtime-epsilon"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat clean build

# Output JAR:
# build/libs/amd-patch-for-voxy-<version>.jar
```

Install the JAR alongside Voxy in your Fabric `mods/` folder and launch.

---

## License

MIT — see `LICENSE`.
