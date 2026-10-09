package particleeraser.mixin;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import particleeraser.context.ParticleRenderContext;

import java.lang.reflect.Field;

@Mixin(ParticleEngine.class)
public class ParticleEngineMixin {
    private static Field alphaField;

    private static Field getAlphaField() {
        if (alphaField != null) {
            return alphaField;
        }
        try {
            for (Field field : Particle.class.getDeclaredFields()) {
                if ("alpha".equals(field.getName()) || "f_107230_".equals(field.getName())) {
                    field.setAccessible(true);
                    alphaField = field;
                    return alphaField;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    @Inject(method = {"add", "m_107344_"}, at = @At("HEAD"), remap = false, require = 0)
    private void onAddParticle(Particle particle, CallbackInfo ci) {
        Field field = getAlphaField();
        if (ParticleRenderContext.hasAlpha() && particle != null && field != null) {
            Float alpha = ParticleRenderContext.getAlpha();
            if (alpha != null) {
                try {
                    float current = field.getFloat(particle);
                    field.setFloat(particle, current * alpha);
                } catch (Throwable ignored) {
                }
            }
        }
    }

    @Redirect(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/particle/Particle;getRenderType()Lnet/minecraft/client/particle/ParticleRenderType;"
        ),
        require = 0
    )
    private ParticleRenderType redirectGetRenderType(Particle particle) {
        if (particle == null) {
            return null;
        }
        ParticleRenderType type = particle.getRenderType();
        if (type == ParticleRenderType.PARTICLE_SHEET_OPAQUE || type == ParticleRenderType.PARTICLE_SHEET_LIT) {
            Field field = getAlphaField();
            if (field != null) {
                try {
                    if (field.getFloat(particle) < 0.999f) {
                        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
                    }
                } catch (Throwable ignored) {
                }
            }
        }
        return type;
    }
}
