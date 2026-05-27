package com.amdfix.mixin.voxy;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Mixin for {@link me.cortex.voxy.client.core.gl.Capabilities}.
 * <p>
 * Voxy's constructor runs a hardware-capability test on AMD GPUs that calls
 * {@code testDepthSampler()}. On Polaris cards (RX 500 series) this test
 * detects the broken {@code texelFetch} behaviour and throws
 * {@code IllegalStateException("it bork, amd is bork")}, crashing the game
 * before the world even loads.
 * <p>
 * We bypass that test here so Voxy can start. The actual depth-sampler fix
 * is applied later by the HiZ shader patch (see
 * {@link HierarchicalOcclusionTraverserMixin}).
 */
@Mixin(targets = "me.cortex.voxy.client.core.gl.Capabilities")
public class CapabilitiesMixin {

    @Redirect(
            method = "<init>()V",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/cortex/voxy/client/core/gl/Capabilities;testDepthSampler()Z"
            )
    )
    private boolean amdPatch$bypassDepthSamplerTest() {
        return false;
    }
}
