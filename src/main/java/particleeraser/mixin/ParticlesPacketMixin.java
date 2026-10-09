package particleeraser.mixin;

import com.robertx22.library_of_exile.packets.ExilePacketContext;
import com.robertx22.mine_and_slash.database.data.spells.components.packets.ParticlesPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import particleeraser.config.ModConfig;
import particleeraser.context.ParticleRenderContext;

@Mixin(value = ParticlesPacket.class, remap = false)
public class ParticlesPacketMixin {
    @Shadow
    public ParticlesPacket.Data data;

    @Inject(method = "onReceived", at = @At("HEAD"), cancellable = true, require = 0)
    private void onReceivedHead(ExilePacketContext ctx, CallbackInfo ci) {
        if (this.data == null || this.data.particle == null) {
            return;
        }

        ModConfig.EffectiveSetting setting = ModConfig.INSTANCE.resolve(null, this.data.particle);

        if (!setting.isEnabled() || setting.getCulling() <= 0.0f) {
            ci.cancel();
            return;
        }

        if (setting.getCulling() < 1.0f) {
            float expected = this.data.amount * setting.getCulling();
            int base = (int) expected;
            float fraction = expected - base;
            int count = base + (Math.random() < fraction ? 1 : 0);
            this.data.amount = count;
            if (this.data.amount <= 0) {
                ci.cancel();
                return;
            }
        }

        ParticleRenderContext.set(setting.getAlpha());
    }

    @Inject(method = "onReceived", at = @At("RETURN"), require = 0)
    private void onReceivedReturn(ExilePacketContext ctx, CallbackInfo ci) {
        ParticleRenderContext.clear();
    }
}
