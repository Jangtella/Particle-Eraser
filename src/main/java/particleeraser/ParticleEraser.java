package particleeraser;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import org.lwjgl.glfw.GLFW;
import particleeraser.config.ModConfig;
import particleeraser.context.ParticleRenderContext;
import particleeraser.data.SkillRegistry;
import particleeraser.gui.ConfigScreen;

@Mod(ParticleEraser.MODID)
public class ParticleEraser {
    public static final String MODID = "particle_eraser";

    public static KeyMapping OPEN_GUI_KEY;

    public ParticleEraser() {
        ModLoadingContext.get().registerExtensionPoint(
                IExtensionPoint.DisplayTest.class,
                () -> new IExtensionPoint.DisplayTest(
                        () -> IExtensionPoint.DisplayTest.IGNORESERVERONLY,
                        (remoteVersion, isServer) -> true
                )
        );

        if (FMLEnvironment.dist != Dist.CLIENT) {
            return;
        }

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::onClientSetup);
        modEventBus.addListener(this::onRegisterKeyMappings);

        MinecraftForge.EVENT_BUS.addListener(this::onClientTick);

        ModConfig.init(FMLPaths.CONFIGDIR.get());

        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, screen) -> new ConfigScreen(screen))
        );
    }

    private void onClientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            SkillRegistry.INSTANCE.initialize();
        });
    }

    private void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        OPEN_GUI_KEY = new KeyMapping(
                "key.particle_eraser.open_gui",
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_KP_MULTIPLY,
                "key.categories.particle_eraser"
        );
        event.register(OPEN_GUI_KEY);
    }

    private void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            ParticleRenderContext.clear();
        } else if (event.phase == TickEvent.Phase.END && OPEN_GUI_KEY != null) {
            while (OPEN_GUI_KEY.consumeClick()) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.screen == null) {
                    mc.setScreen(new ConfigScreen(null));
                }
            }
        }
    }
}
