package com.amdfix.mixin.voxy;

import com.amdfix.AMDPatchForVoxy;
import com.amdfix.patch.ShaderPatchUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * <strong>Fallback fix</strong> for the AMD Polaris depth-sampler bug.
 * <p>
 * This mixin sits on the public {@code ShaderLoader.parse()} entry point. It
 * applies the same patch as {@link HierarchicalOcclusionTraverserMixin} but
 * operates on <em>any</em> shader that happens to contain the buggy line. This
 * catches edge cases where a future Voxy refactor might call
 * {@code ShaderLoader.parse()} with a different identifier, or where another
 * shader in Voxy's pipeline happens to reuse the same {@code texelFetch} pattern.
 * <p>
 * If the primary mixin ({@code HierarchicalOcclusionTraverserMixin}) has
 * already patched the shader, this fallback will simply find nothing to replace
 * and return the source unchanged — it is harmless and idempotent.
 */
@Mixin(targets = "me.cortex.voxy.client.core.gl.shader.ShaderLoader")
public class ShaderLoaderMixin {

    @Inject(
            method = "parse(Ljava/lang/String;)Ljava/lang/String;",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void amdPatch$patchAnyShader(String id, CallbackInfoReturnable<String> cir) {
        String source = cir.getReturnValue();
        if (!ShaderPatchUtil.isPatchable(source)) {
            return;
        }

        cir.setReturnValue(ShaderPatchUtil.apply(source));
        AMDPatchForVoxy.LOGGER.debug("AMD Patch: Applied fallback HiZ fix to '{}'.", id);
    }
}
