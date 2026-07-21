package com.amdfix.patch;

/**
 * Centralizes the AMD HiZ depth-sampler patch constants and logic.
 * <p>
 * Keeping everything in one place makes updates trivial: if Voxy ever changes
 * the shader line we target, only this file needs to be edited.
 */
public final class ShaderPatchUtil {

    private ShaderPatchUtil() {
        // utility class — not instantiable
    }

    /**
     * The exact GLSL line inside Voxy's {@code screenspace.glsl} that samples
     * the HiZ depth texture. The patch looks for this literal string and
     * replaces it with {@link #PATCHED_CODE}.
     */
    public static final String ORIGINAL_LINE =
            "float sp = texelFetch(hizDepthSampler, ivec2(x, y), ml).r;";

    /**
     * Replacement GLSL block. Values {@code <= 0.2f} are forced to {@code FAR}
     * (the far plane) so that driver-bogus near-zero reads cannot corrupt the
     * {@code REDUCTION} (min/max) operation used for HiZ occlusion culling.
     * <p>
     * Why use the {@code FAR} macro instead of a hardcoded {@code 1.0f}?
     * <p>
     * Minecraft 26.2 introduced a reversed depth buffer
     * ("Rendering now uses a reversed depth buffer"), which changes the
     * semantics of depth values. Voxy's HiZ shader adapts to both depth
     * conventions via the {@code USE_REVERSE_Z} macro (see Voxy's
     * {@code depthutils.glsl}):
     * <ul>
     *   <li><strong>Standard-Z</strong> (MC 26.1.2 and earlier):
     *       {@code FAR = 1.0f}, {@code REDUCTION = max}. Setting bogus values
     *       to {@code FAR} pushes {@code pointSample} to the far plane, so the
     *       region reads as "empty/far away" and nodes are not culled
     *       (conservative).</li>
     *   <li><strong>Reverse-Z</strong> (MC 26.2+): {@code FAR = 0.0f},
     *       {@code REDUCTION = min}. Setting bogus values to {@code FAR} pulls
     *       {@code pointSample} to the far plane (0.0), so the region reads as
     *       "empty/far away" and nodes are not culled (conservative).</li>
     * </ul>
     * The previous implementation hardcoded {@code 1.0f}, which in Reverse-Z
     * mode equals {@code NEAR} (the near plane). Combined with
     * {@code REDUCTION = min}, this made the region read as "full of nearby
     * geometry", causing <em>every</em> node behind it to be culled and Voxy
     * to appear completely disabled on MC 26.2+.
     * <p>
     * Threshold {@code 0.2f} was chosen because it covers:
     * <ul>
     *   <li>Sky/empty pixels where depth may read as {@code 0.0}</li>
     *   <li>Close-up walls that are legitimate near-plane values</li>
     *   <li>Noise introduced by the AMD Polaris driver bug</li>
     * </ul>
     */
    public static final String PATCHED_CODE = """
            float sp = texelFetch(hizDepthSampler, ivec2(x, y), ml).r;
            if (sp <= 0.2f) {
                sp = FAR;
            }
            """;

    /**
     * Applies the HiZ patch to the given shader source.
     *
     * @param source the raw shader source loaded by Voxy
     * @return the patched source, or the original source if the target line
     *         was not found (e.g. Voxy updated its implementation)
     */
    public static String apply(String source) {
        if (source == null || !source.contains(ORIGINAL_LINE)) {
            return source;
        }
        return source.replace(ORIGINAL_LINE, PATCHED_CODE);
    }

    /**
     * Checks whether the given shader source still contains the unpatched line.
     * Useful for logging warnings when Voxy updates break our patch.
     */
    public static boolean isPatchable(String source) {
        return source != null && source.contains(ORIGINAL_LINE);
    }
}
