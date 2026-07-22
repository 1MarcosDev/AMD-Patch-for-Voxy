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
     * Replacement GLSL block. Raw samples {@code <= 0.2f} are forced to Voxy's
     * {@code FAR} macro so that driver-bogus near-zero reads cannot corrupt the
     * {@code REDUCTION} (min/max) operation used for HiZ occlusion culling.
     * <p>
     * <strong>Why {@code FAR} and not a literal {@code 1.0f}:</strong> Voxy's
     * {@code depthutils.glsl} defines the depth convention two ways depending on
     * the {@code USE_REVERSE_Z} compile flag. Under standard depth {@code FAR}
     * is {@code 1.0} and {@code REDUCTION} is {@code max}; under reverse-Z (used
     * by Minecraft 26.2's renderer) {@code FAR} is {@code 0.0} and
     * {@code REDUCTION} is {@code min}. Hardcoding {@code 1.0f} pins bogus reads
     * to the <em>near</em> plane under reverse-Z, which makes the culler treat
     * every distant region as occluded and hides all LOD sections. Using the
     * {@code FAR} macro is correct in both conventions — the AMD driver returns
     * raw near-zero values regardless of convention, so "snap near-zero to
     * {@code FAR}" always errs toward rendering (conservative culling).
     * <p>
     * Threshold {@code 0.2f} (compared against the raw sample) covers:
     * <ul>
     *   <li>Sky/empty pixels where depth may read as {@code 0.0}</li>
     *   <li>Close-up walls that are legitimate near-plane values</li>
     *   <li>Noise introduced by the AMD Polaris driver bug</li>
     * </ul>
     * {@code FAR} is defined in {@code voxy:util/depthutils.glsl}, which is
     * imported by {@code screenspace.glsl} before this line, so it is always in
     * scope at the injection point.
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
