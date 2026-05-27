package com.amdfix.mixin.voxy;

import com.amdfix.AMDPatchForVoxy;
import com.amdfix.patch.ShaderPatchUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * <strong>Primary fix</strong> for the AMD Polaris depth-sampler bug.
 * <p>
 * This mixin targets {@code HierarchicalOcclusionTraverser.lateStageCompile()},
 * the exact place where Voxy loads the HiZ traversal compute shader and
 * compiles it. We intercept the shader source string after all
 * {@code #import} directives have been resolved but before compilation, and
 * inject the AMD-safe clamp.
 * <p>
 * Why this location instead of the shader loader itself?
 * <ul>
 *   <li>{@code screenspace.glsl} (which contains the buggy {@code texelFetch})
 *       is imported <em>internally</em> by Voxy's private parser. It never
 *       passes through the public {@code ShaderLoader.parse()} method.</li>
 *   <li>Intercepting the fully assembled compute shader guarantees the patch
 *       is applied regardless of how Voxy structures its imports.</li>
 * </ul>
 */
@Mixin(targets = "me.cortex.voxy.client.core.rendering.hierachical.HierarchicalOcclusionTraverser")
public class HierarchicalOcclusionTraverserMixin {

    @ModifyVariable(
            method = "lateStageCompile(Lme/cortex/voxy/client/core/AbstractRenderPipeline;)V",
            at = @At("STORE"),
            ordinal = 1
    )
    private String amdPatch$patchTraversalShader(String scr) {
        if (!ShaderPatchUtil.isPatchable(scr)) {
            return scr;
        }

        AMDPatchForVoxy.LOGGER.info("AMD Patch: Applying HiZ depth-sampler fix to traversal shader.");
        return ShaderPatchUtil.apply(scr);
    }
}
