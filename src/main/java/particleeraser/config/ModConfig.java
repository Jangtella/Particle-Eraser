package particleeraser.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import particleeraser.data.SkillRegistry;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ModConfig {
    public static final ModConfig INSTANCE = new ModConfig();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static File configFile;

    private boolean globalEnabled = true;
    private float globalAlpha = 1.0f;
    private float globalCulling = 1.0f;

    private boolean detailedEnabled = true;
    private Map<String, SkillConfig> skills = new ConcurrentHashMap<>();

    public static void init(Path configDir) {
        configFile = configDir.resolve("particle_eraser.json").toFile();
        INSTANCE.load();
    }

    public synchronized void load() {
        if (configFile == null || !configFile.exists()) {
            save();
            return;
        }
        try (FileReader reader = new FileReader(configFile)) {
            ModConfig loaded = GSON.fromJson(reader, ModConfig.class);
            if (loaded != null) {
                this.globalEnabled = loaded.globalEnabled;
                this.globalAlpha = loaded.globalAlpha;
                this.globalCulling = loaded.globalCulling;
                this.detailedEnabled = loaded.detailedEnabled;
                if (loaded.skills != null) {
                    this.skills.clear();
                    this.skills.putAll(loaded.skills);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void save() {
        if (configFile == null) {
            return;
        }
        try {
            File parent = configFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            try (FileWriter writer = new FileWriter(configFile)) {
                GSON.toJson(this, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isGlobalEnabled() {
        return globalEnabled;
    }

    public void setGlobalEnabled(boolean globalEnabled) {
        this.globalEnabled = globalEnabled;
    }

    public float getGlobalAlpha() {
        return globalAlpha;
    }

    public void setGlobalAlpha(float globalAlpha) {
        this.globalAlpha = Math.max(0.0f, Math.min(1.0f, globalAlpha));
    }

    public float getGlobalCulling() {
        return globalCulling;
    }

    public void setGlobalCulling(float globalCulling) {
        this.globalCulling = Math.max(0.0f, Math.min(1.0f, globalCulling));
    }

    public boolean isDetailedEnabled() {
        return detailedEnabled;
    }

    public void setDetailedEnabled(boolean detailedEnabled) {
        this.detailedEnabled = detailedEnabled;
    }

    public Map<String, SkillConfig> getSkills() {
        return skills;
    }

    public SkillConfig getOrCreateSkillConfig(String skillId) {
        return skills.computeIfAbsent(skillId, SkillConfig::new);
    }

    public void resetGlobal() {
        this.globalEnabled = true;
        this.globalAlpha = 1.0f;
        this.globalCulling = 1.0f;
        this.detailedEnabled = true;
        save();
    }

    public void resetAllDetailed() {
        for (SkillConfig config : skills.values()) {
            config.reset();
        }
        save();
    }

    public EffectiveSetting resolve(String skillId, String particleId) {
        if (detailedEnabled) {
            if (skillId != null) {
                SkillConfig cfg = skills.get(skillId);
                if (cfg != null) {
                    if (particleId != null) {
                        SkillConfig.ParticleSetting ps = cfg.getParticleSetting(particleId);
                        if (ps != null && ps.isModified()) {
                            return new EffectiveSetting(ps.isEnabled(), ps.getAlpha(), ps.getCulling());
                        }
                    }
                    if (cfg.isModified()) {
                        return new EffectiveSetting(cfg.isEnabled(), cfg.getAlpha(), cfg.getCulling());
                    }
                }
            }
            if (particleId != null) {
                List<String> candidateSkills = SkillRegistry.INSTANCE.findSkillsByParticle(particleId);
                for (String candidateId : candidateSkills) {
                    SkillConfig cfg = skills.get(candidateId);
                    if (cfg != null) {
                        SkillConfig.ParticleSetting ps = cfg.getParticleSetting(particleId);
                        if (ps != null && ps.isModified()) {
                            return new EffectiveSetting(ps.isEnabled(), ps.getAlpha(), ps.getCulling());
                        }
                        if (cfg.isModified()) {
                            return new EffectiveSetting(cfg.isEnabled(), cfg.getAlpha(), cfg.getCulling());
                        }
                    }
                }
            }
        }

        if (!globalEnabled) {
            return new EffectiveSetting(false, 1.0f, 0.0f);
        }

        return new EffectiveSetting(true, globalAlpha, globalCulling);
    }

    public static class EffectiveSetting {
        private final boolean enabled;
        private final float alpha;
        private final float culling;

        public EffectiveSetting(boolean enabled, float alpha, float culling) {
            this.enabled = enabled;
            this.alpha = alpha;
            this.culling = culling;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public float getAlpha() {
            return alpha;
        }

        public float getCulling() {
            return culling;
        }
    }
}
