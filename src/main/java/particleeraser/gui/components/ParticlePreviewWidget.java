package particleeraser.gui.components;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Items;
import particleeraser.data.SkillInfo;
import particleeraser.mixin.ParticleEngineAccessor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;

public class ParticlePreviewWidget extends AbstractWidget {
    private static final float SIN_PITCH = 0.4695f;
    private static final float COS_PITCH = 0.8829f;

    private final BooleanSupplier enabledSupplier;
    private final DoubleSupplier alphaSupplier;
    private final DoubleSupplier cullingSupplier;
    private final List<ResourceLocation> particleTypes = new ArrayList<>();
    private final List<SimulatedParticle3D> particles = new ArrayList<>();
    private final RandomSource rand = RandomSource.create();
    private final List<ResourceLocation> burstTypes = new ArrayList<>();
    private String school = null;
    private float cycleTimer = 0.0f;
    private float yaw = 0.0f;
    private long lastTime = 0;
    private float playbackSpeed = 1.0f;
    private SkillInfo skillInfo = null;
    private String skillId = null;

    public void setPlaybackSpeed(float speed) {
        this.playbackSpeed = Math.max(0.05f, Math.min(2.0f, speed));
    }

    public float getPlaybackSpeed() {
        return this.playbackSpeed;
    }

    public ParticlePreviewWidget(int x, int y, int width, int height,
                                 BooleanSupplier enabledSupplier,
                                 DoubleSupplier alphaSupplier,
                                 DoubleSupplier cullingSupplier) {
        super(x, y, width, height, Component.empty());
        this.enabledSupplier = enabledSupplier;
        this.alphaSupplier = alphaSupplier;
        this.cullingSupplier = cullingSupplier;

        for (int i = 0; i < 30; i++) {
            SimulatedParticle3D p = new SimulatedParticle3D();
            p.reset(new ResourceLocation("minecraft", "flame"), rand, null, null);
            p.age = rand.nextInt(50);
            particles.add(p);
        }
    }

    public void setSkillInfo(SkillInfo skillInfo) {
        this.skillInfo = skillInfo;
        if (skillInfo != null) {
            this.school = skillInfo.getSchool();
            this.skillId = skillInfo.getId();
        }
    }

    public void setSkillId(String skillId) {
        this.skillId = skillId;
    }

    public void setSchool(String school) {
        this.school = school;
        reassignParticleTypes();
    }

    public void setParticleTypes(List<ResourceLocation> types) {
        this.particleTypes.clear();
        this.burstTypes.clear();
        if (types != null && !types.isEmpty()) {
            for (ResourceLocation t : types) {
                boolean isBurst = false;
                if (this.skillInfo != null) {
                    isBurst = this.skillInfo.isBurstParticle(t.toString()) || this.skillInfo.isBurstParticle(t.getPath());
                } else {
                    isBurst = isBurstParticle(t);
                }
                if (isBurst) {
                    if (!this.burstTypes.contains(t)) {
                        this.burstTypes.add(t);
                    }
                } else {
                    if (!this.particleTypes.contains(t)) {
                        this.particleTypes.add(t);
                    }
                }
            }
            if (this.particleTypes.isEmpty() && this.burstTypes.isEmpty()) {
                this.particleTypes.add(new ResourceLocation("minecraft", "flame"));
            }
        } else {
            this.particleTypes.add(new ResourceLocation("minecraft", "flame"));
        }
        reassignParticleTypes();
    }

    public void setParticleNames(List<String> names) {
        List<ResourceLocation> list = new ArrayList<>();
        if (names != null) {
            for (String s : names) {
                if (s == null || s.trim().isEmpty()) {
                    continue;
                }
                String normalized = s.contains(":") ? s : "minecraft:" + s;
                ResourceLocation rl = ResourceLocation.tryParse(normalized);
                if (rl != null) {
                    list.add(rl);
                }
            }
        }
        setParticleTypes(list);
    }

    private void reassignParticleTypes() {
        if (particleTypes.isEmpty()) {
            particles.clear();
            return;
        }

        int totalCount = 0;
        for (ResourceLocation t : particleTypes) {
            SkillInfo.ParticleMeta m = (skillInfo != null) ? skillInfo.getParticleMeta(t.toString()) : null;
            totalCount += (m != null) ? m.count : 25;
        }
        totalCount = Math.max(10, Math.min(300, totalCount));

        while (particles.size() < totalCount) {
            SimulatedParticle3D p = new SimulatedParticle3D();
            particles.add(p);
        }
        while (particles.size() > totalCount) {
            particles.remove(particles.size() - 1);
        }

        for (int i = 0; i < particles.size(); i++) {
            ResourceLocation t = particleTypes.get(i % particleTypes.size());
            particles.get(i).reset(t, rand, school, skillInfo);
            particles.get(i).age = rand.nextInt(Math.max(1, particles.get(i).maxAge));
        }
    }

    public static boolean isBurstParticle(ResourceLocation type) {
        if (type == null) {
            return false;
        }
        String path = type.getPath().toLowerCase();
        return path.contains("explosion") || path.contains("flash") || path.contains("sonic_boom")
                || path.contains("sweep") || path.contains("snowball") || path.contains("slime");
    }

    public static int getSizeForParticle(ResourceLocation type) {
        if (type == null) {
            return 9;
        }
        String path = type.getPath().toLowerCase();
        if (path.contains("explosion") || path.contains("sonic_boom")) {
            return 26;
        } else if (path.contains("flash") || path.contains("sweep")) {
            return 18;
        } else if (path.contains("witch") || path.contains("portal") || path.contains("bubble")) {
            return 7;
        } else if (path.contains("dust") || path.contains("effect")) {
            return 8;
        }
        return 9;
    }

    public static float[] getColorForParticle(ResourceLocation type, String school) {
        if (type == null) {
            return new float[]{1.0f, 1.0f, 1.0f};
        }
        String path = type.getPath().toLowerCase();
        if (path.contains("witch")) {
            return new float[]{0.85f, 0.15f, 0.90f};
        } else if (path.contains("portal")) {
            return new float[]{0.90f, 0.20f, 0.85f};
        } else if (path.contains("enchant")) {
            return new float[]{0.35f, 0.85f, 1.00f};
        } else if (path.contains("wax")) {
            return new float[]{0.95f, 0.65f, 0.35f};
        } else if (path.contains("pillar_rune")) {
            return new float[]{0.85f, 0.30f, 0.95f};
        } else if (path.contains("smoke")) {
            return new float[]{0.45f, 0.45f, 0.45f};
        } else if (path.contains("cloud")) {
            return new float[]{0.90f, 0.90f, 0.95f};
        } else if (path.contains("totem")) {
            return new float[]{0.40f, 0.90f, 0.30f};
        } else if (path.contains("effect")) {
            if (school != null) {
                String s = school.toLowerCase();
                if (s.contains("warlock") || s.contains("shadow")) {
                    return new float[]{0.65f, 0.15f, 0.85f};
                } else if (s.contains("cryo") || s.contains("cold") || s.contains("ice")) {
                    return new float[]{0.30f, 0.80f, 1.00f};
                } else if (s.contains("sorcerer") || s.contains("elementalist") || s.contains("fire")) {
                    return new float[]{1.00f, 0.45f, 0.10f};
                } else if (s.contains("crusader") || s.contains("holy")) {
                    return new float[]{1.00f, 0.90f, 0.30f};
                } else if (s.contains("shaman") || s.contains("lightning")) {
                    return new float[]{0.85f, 0.95f, 0.30f};
                } else if (s.contains("hunter") || s.contains("nature")) {
                    return new float[]{0.30f, 0.90f, 0.30f};
                } else if (s.contains("sanguimancer") || s.contains("blood")) {
                    return new float[]{0.95f, 0.10f, 0.15f};
                } else if (s.contains("brawler") || s.contains("fighter") || s.contains("warrior") || s.contains("rogue")) {
                    return new float[]{0.90f, 0.30f, 0.20f};
                }
            }
            return new float[]{0.50f, 0.75f, 1.00f};
        }
        return new float[]{1.0f, 1.0f, 1.0f};
    }

    public static ResourceLocation resolveSpriteType(ResourceLocation type) {
        if (type == null) {
            return new ResourceLocation("minecraft", "flame");
        }
        String path = type.getPath();
        String ns = type.getNamespace();
        if (path.equals("explosion_emitter")) {
            return new ResourceLocation("minecraft", "explosion");
        }
        if (path.equals("aoul")) {
            return new ResourceLocation("minecraft", "soul");
        }
        if (ns.equals("minecraft") && path.equals("sculk")) {
            return new ResourceLocation("minecraft", "sculk_charge");
        }
        if (ns.equals("bosses_of_mass_destruction") && path.equals("split")) {
            return new ResourceLocation("bosses_of_mass_destruction", "obsidilith_burst");
        }
        if (ns.equals("born_in_chaos_v1") && (path.equals("obsessionpart") || path.equals("obsessed"))) {
            return new ResourceLocation("born_in_chaos_v1", "obsessionpar");
        }
        return type;
    }

    public static String getParticleError(String particle) {
        if (particle == null) {
            return null;
        }
        if (particle.equals("minecraft:aoul")
                || particle.equals("minecraft:sculk")
                || particle.equals("bosses_of_mass_destruction:split")
                || particle.equals("born_in_chaos_v1:obsessionpart")
                || particle.equals("born_in_chaos_v1:obsessed")) {
            return particle;
        }
        return null;
    }

    public static TextureAtlasSprite getSprite(ResourceLocation type, int age, int maxAge, Map<ResourceLocation, ?> spriteSets) {
        if (type == null) {
            return null;
        }
        ResourceLocation lookup = resolveSpriteType(type);
        if (spriteSets != null) {
            Object setObj = spriteSets.get(lookup);
            if (setObj instanceof SpriteSet spriteSet) {
                try {
                    return spriteSet.get(Math.min(age, maxAge), Math.max(1, maxAge));
                } catch (Throwable ignored) {
                }
            }
        }
        if (lookup.getPath().equals("item_slime")) {
            try {
                return Minecraft.getInstance().getItemRenderer().getItemModelShaper().getItemModel(Items.SLIME_BALL).getParticleIcon();
            } catch (Throwable ignored) {
            }
        }
        if (lookup.getPath().equals("item_snowball")) {
            try {
                return Minecraft.getInstance().getItemRenderer().getItemModelShaper().getItemModel(Items.SNOWBALL).getParticleIcon();
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();

        graphics.fill(x, y, x + w, y + h, 0xFF0D0D14);
        graphics.fill(x, y, x + w, y + 1, 0xFF353540);
        graphics.fill(x, y + h - 1, x + w, y + h, 0xFF353540);
        graphics.fill(x, y, x + 1, y + h, 0xFF353540);
        graphics.fill(x + w - 1, y, x + w, y + h, 0xFF353540);

        boolean enabled = enabledSupplier == null || enabledSupplier.getAsBoolean();
        if (!enabled) {
            Font font = Minecraft.getInstance().font;
            graphics.drawCenteredString(font, "OFF", x + w / 2, y + (h - 8) / 2, 0xFF666677);
            return;
        }

        if (particleTypes.isEmpty() && burstTypes.isEmpty()) {
            Font font = Minecraft.getInstance().font;
            graphics.drawCenteredString(font, "No Particles", x + w / 2, y + (h - 8) / 2, 0xFF666677);
            return;
        }

        long now = System.currentTimeMillis();
        float dt = 1.0f;
        if (lastTime != 0) {
            dt = (now - lastTime) / 50.0f;
            if (dt > 2.0f) {
                dt = 2.0f;
            }
            dt *= playbackSpeed;
            if (dt > 0.0f) {
                yaw += dt * 0.025f;
                for (SimulatedParticle3D p : particles) {
                    p.update(dt, rand, school, skillInfo);
                }
            }
        }
        lastTime = now;

        float maxCycle = 22.0f;
        cycleTimer += dt;
        if (cycleTimer >= maxCycle) {
            cycleTimer = 0.0f;
        }

        float culling = (cullingSupplier != null) ? (float) cullingSupplier.getAsDouble() : 1.0f;
        float alpha = (alphaSupplier != null) ? (float) alphaSupplier.getAsDouble() : 1.0f;
        alpha = Math.max(0.0f, Math.min(1.0f, alpha));
        culling = Math.max(0.0f, Math.min(1.0f, culling));

        if (alpha <= 0.0f || culling <= 0.0f) {
            return;
        }

        float activeRadius = 0.0f;
        for (ResourceLocation t : particleTypes) {
            SkillInfo.ParticleMeta m = (skillInfo != null) ? skillInfo.getParticleMeta(t.toString()) : null;
            if (m != null && m.radius > activeRadius) {
                activeRadius = m.radius;
            }
        }
        for (ResourceLocation t : burstTypes) {
            SkillInfo.ParticleMeta m = (skillInfo != null) ? skillInfo.getParticleMeta(t.toString()) : null;
            if (m != null && m.radius > activeRadius) {
                activeRadius = m.radius;
            }
        }
        if (activeRadius <= 0.2f) {
            activeRadius = 2.0f;
        }

        float scale = Math.max(12.0f, Math.min(65.0f, (w * 0.38f) / activeRadius));

        int cx = x + w / 2;
        int cy = y + (int) (h * 0.63f);

        graphics.enableScissor(x + 1, y + 1, x + w - 1, y + h - 1);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        float sinYaw = (float) Math.sin(yaw);
        float cosYaw = (float) Math.cos(yaw);

        for (int step = 0; step < 24; step++) {
            double ga = step * (2.0 * Math.PI / 24.0);
            float gx = (float) (Math.cos(ga) * (activeRadius * 0.95f));
            float gz = (float) (Math.sin(ga) * (activeRadius * 0.95f));
            float grx = gx * cosYaw - gz * sinYaw;
            float grz = gx * sinYaw + gz * cosYaw;
            int gsx = (int) (cx + grx * scale);
            int gsy = (int) (cy + (grz * SIN_PITCH) * scale);
            if (gsx >= x + 1 && gsx < x + w - 1 && gsy >= y + 1 && gsy < y + h - 1) {
                graphics.fill(gsx, gsy, gsx + 1, gsy + 1, 0x33555566);
            }
        }

        ParticleEngine pe = Minecraft.getInstance().particleEngine;
        Map<ResourceLocation, ?> spriteSets = (pe instanceof ParticleEngineAccessor accessor) ? accessor.getSpriteSets() : null;

        List<RenderItem> renderList = new ArrayList<>();

        if (!particleTypes.isEmpty()) {
            int visibleCount = (int) Math.round(particles.size() * culling);
            for (int i = 0; i < visibleCount && i < particles.size(); i++) {
                SimulatedParticle3D p = particles.get(i);
                float rx = p.x * cosYaw - p.z * sinYaw;
                float rz = p.x * sinYaw + p.z * cosYaw;
                float depth = rz * COS_PITCH + p.y * SIN_PITCH;
                int sx = (int) (cx + rx * scale);
                int sy = (int) (cy + (rz * SIN_PITCH - p.y * COS_PITCH) * scale);

                TextureAtlasSprite sprite = getSprite(p.type, p.age, p.maxAge, spriteSets);
                RenderItem item = new RenderItem();
                item.sx = sx;
                item.sy = sy;
                item.depth = depth;
                item.size = p.size;
                item.r = p.r;
                item.g = p.g;
                item.b = p.b;
                item.alpha = alpha;
                item.sprite = sprite;
                renderList.add(item);
            }
        }

        if (!burstTypes.isEmpty()) {
            float burstStart = 4.0f;
            float burstDuration = 10.0f;
            if (cycleTimer >= burstStart && cycleTimer < burstStart + burstDuration) {
                float burstProgress = (cycleTimer - burstStart) / burstDuration;

                for (int bIndex = 0; bIndex < burstTypes.size(); bIndex++) {
                    ResourceLocation burstType = burstTypes.get(bIndex);
                    SkillInfo.ParticleMeta meta = (skillInfo != null)
                            ? skillInfo.getParticleMeta(burstType.toString())
                            : new SkillInfo.ParticleMeta(10, 2.5f, 0.5f, "CIRCLE");

                    float[] bCol = getColorForParticle(burstType, school);
                    int baseCount = Math.max(1, Math.min(300, meta.count));
                    if (burstType.getPath().equals("explosion_emitter")) {
                        baseCount = 1;
                    }
                    int pCount = (int) Math.round(baseCount * culling);
                    if (pCount <= 0) {
                        continue;
                    }

                    TextureAtlasSprite sprite = getSprite(burstType, (int) (burstProgress * 15), 16, spriteSets);
                    if (sprite == null && spriteSets != null) {
                        Object setObj = spriteSets.get(new ResourceLocation("minecraft", "explosion"));
                        if (setObj instanceof SpriteSet spriteSet) {
                            try {
                                sprite = spriteSet.get((int) (burstProgress * 15), 16);
                            } catch (Throwable ignored) {
                            }
                        }
                    }

                    if (burstType.getPath().equals("explosion_emitter")) {
                        int bSize = (int) (getSizeForParticle(burstType) * (0.8f + burstProgress * 0.5f));
                        float py = meta.height * 0.5f;
                        float depth = py * SIN_PITCH;
                        int sx = cx;
                        int sy = (int) (cy - (py * COS_PITCH) * scale);

                        RenderItem item = new RenderItem();
                        item.sx = sx;
                        item.sy = sy;
                        item.depth = depth;
                        item.size = bSize;
                        item.r = bCol[0];
                        item.g = bCol[1];
                        item.b = bCol[2];
                        item.alpha = alpha;
                        item.sprite = sprite;
                        renderList.add(item);
                    } else if (meta.height >= 2.5f) {
                        int dotSize = getSizeForParticle(burstType);
                        float beamHeight = meta.height * Math.min(1.0f, burstProgress * 1.6f);
                        for (int ring = 0; ring < pCount; ring++) {
                            float py = (ring / (float) Math.max(1, pCount - 1)) * beamHeight;
                            double beamAngle = ring * 1.8 + bIndex * 0.7;
                            float bx = (float) (Math.cos(beamAngle) * (meta.radius * 0.35f));
                            float bz = (float) (Math.sin(beamAngle) * (meta.radius * 0.35f));
                            float rx = bx * cosYaw - bz * sinYaw;
                            float rz = bx * sinYaw + bz * cosYaw;
                            float depth = rz * COS_PITCH + py * SIN_PITCH;
                            int sx = (int) (cx + rx * scale);
                            int sy = (int) (cy + (rz * SIN_PITCH - py * COS_PITCH) * scale);

                            RenderItem item = new RenderItem();
                            item.sx = sx;
                            item.sy = sy;
                            item.depth = depth;
                            item.size = dotSize;
                            item.r = bCol[0];
                            item.g = bCol[1];
                            item.b = bCol[2];
                            item.alpha = alpha * (1.0f - burstProgress * 0.3f);
                            item.sprite = sprite;
                            renderList.add(item);
                        }
                    } else {
                        float radiusMult = 1.0f - bIndex * 0.15f;
                        float currentRadius = meta.radius * burstProgress * radiusMult;
                        int dotSize = (int) (getSizeForParticle(burstType) * (1.1f - burstProgress * 0.3f));
                        double angleOffset = bIndex * (Math.PI / Math.max(1, pCount));

                        for (int ring = 0; ring < pCount; ring++) {
                            double angle = ring * (2.0 * Math.PI / pCount) + angleOffset;
                            float bx = (float) (Math.cos(angle) * currentRadius);
                            float bz = (float) (Math.sin(angle) * currentRadius);
                            float py = (meta.height * 0.5f) * (1.0f - burstProgress * 0.4f);
                            float rx = bx * cosYaw - bz * sinYaw;
                            float rz = bx * sinYaw + bz * cosYaw;
                            float depth = rz * COS_PITCH + py * SIN_PITCH;
                            int sx = (int) (cx + rx * scale);
                            int sy = (int) (cy + (rz * SIN_PITCH - py * COS_PITCH) * scale);

                            RenderItem item = new RenderItem();
                            item.sx = sx;
                            item.sy = sy;
                            item.depth = depth;
                            item.size = dotSize;
                            item.r = bCol[0];
                            item.g = bCol[1];
                            item.b = bCol[2];
                            item.alpha = alpha * (1.0f - burstProgress * 0.4f);
                            item.sprite = sprite;
                            renderList.add(item);
                        }
                    }
                }
            }
        }

        renderList.sort(Comparator.comparingDouble(it -> -it.depth));

        for (RenderItem item : renderList) {
            if (item.sprite != null) {
                try {
                    graphics.blit(item.sx - item.size / 2, item.sy - item.size / 2, 0, item.size, item.size, item.sprite, item.r, item.g, item.b, item.alpha);
                } catch (Throwable ignored) {
                }
            } else {
                int a = (int) (item.alpha * 255) << 24;
                int r = (int) (item.r * 255) << 16;
                int g = (int) (item.g * 255) << 8;
                int b = (int) (item.b * 255);
                graphics.fill(item.sx - 2, item.sy - 2, item.sx + 2, item.sy + 2, a | r | g | b);
            }
        }

        graphics.disableScissor();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }

    private static class RenderItem {
        int sx;
        int sy;
        float depth;
        int size;
        float r;
        float g;
        float b;
        float alpha;
        TextureAtlasSprite sprite;
    }

    private static class SimulatedParticle3D {
        float x;
        float y;
        float z;
        float vx;
        float vy;
        float vz;
        int age;
        int maxAge;
        int size;
        float r = 1.0f;
        float g = 1.0f;
        float b = 1.0f;
        ResourceLocation type;
        String school;

        void reset(ResourceLocation type, RandomSource rand, String school, SkillInfo skillInfo) {
            this.type = type;
            this.school = school;
            float[] col = getColorForParticle(type, this.school);
            this.r = col[0];
            this.g = col[1];
            this.b = col[2];
            this.size = getSizeForParticle(type);

            SkillInfo.ParticleMeta meta = (skillInfo != null && type != null)
                    ? skillInfo.getParticleMeta(type.toString())
                    : new SkillInfo.ParticleMeta(10, 2.0f, 0.5f, "CIRCLE");

            float rad = Math.max(0.6f, meta.radius);
            float h = Math.max(0.2f, meta.height);
            double angle = rand.nextFloat() * 6.2831853f;

            if ("CIRCLE_2D_EDGE".equals(meta.shape)) {
                float rDist = rad * (0.85f + rand.nextFloat() * 0.3f);
                this.x = (float) (Math.cos(angle) * rDist);
                this.z = (float) (Math.sin(angle) * rDist);
                this.y = rand.nextFloat() * h;
            } else {
                float rDist = rad * (float) Math.sqrt(rand.nextFloat());
                this.x = (float) (Math.cos(angle) * rDist);
                this.z = (float) (Math.sin(angle) * rDist);
                this.y = rand.nextFloat() * h;
            }

            this.vx = (rand.nextFloat() - 0.5f) * 0.02f;
            this.vy = 0.02f + rand.nextFloat() * 0.04f;
            this.vz = (rand.nextFloat() - 0.5f) * 0.02f;
            this.age = 0;
            this.maxAge = 40 + rand.nextInt(40);
        }

        void update(float dt, RandomSource rand, String school, SkillInfo skillInfo) {
            x += vx * dt;
            y += vy * dt;
            z += vz * dt;
            age += (int) Math.max(1, dt);
            if (age >= maxAge) {
                reset(type, rand, school, skillInfo);
            }
        }
    }
}
