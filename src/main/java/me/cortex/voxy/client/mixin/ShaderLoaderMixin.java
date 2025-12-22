package me.cortex.voxy.client.mixin;

import me.cortex.voxy.client.core.gl.shader.ShaderLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShaderLoader.class)
public class ShaderLoaderMixin {

    @Inject(method = "parse", at = @At("RETURN"), cancellable = true)
    private static void injectAmdFix(String id, CallbackInfoReturnable<String> cir) {
        String source = cir.getReturnValue();
        
        // Check if the shader contains the code we want to patch
        // The target code is: float sp = texelFetch(hizDepthSampler, ivec2(x, y), ml).r;
        if (source != null && source.contains("float sp = texelFetch(hizDepthSampler, ivec2(x, y), ml).r;")) {
             String fix = """
                    float sp = texelFetch(hizDepthSampler, ivec2(x, y), ml).r;
                    if (sp <= 0.0001f) {
                        sp = 1.0f;
                    }
                    """;
             String patched = source.replace("float sp = texelFetch(hizDepthSampler, ivec2(x, y), ml).r;", fix);
             cir.setReturnValue(patched);
        }
    }
}
