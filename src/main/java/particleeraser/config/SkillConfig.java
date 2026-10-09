package particleeraser.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SkillConfig {
    private String skillId;
    private boolean enabled = true;
    private float alpha = 1.0f;
    private float culling = 1.0f;
    private boolean modified = false;
    private Map<String, ParticleSetting> particles = new ConcurrentHashMap<>();

    public SkillConfig() {
    }

    public SkillConfig(String skillId) {
        this.skillId = skillId;
    }

    public String getSkillId() {
        return skillId;
    }

    public void setSkillId(String skillId) {
        this.skillId = skillId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        this.modified = true;
    }

    public float getAlpha() {
        return alpha;
    }

    public void setAlpha(float alpha) {
        this.alpha = Math.max(0.0f, Math.min(1.0f, alpha));
        this.modified = true;
    }

    public float getCulling() {
        return culling;
    }

    public void setCulling(float culling) {
        this.culling = Math.max(0.0f, Math.min(1.0f, culling));
        this.modified = true;
    }

    public boolean isModified() {
        if (modified) {
            return true;
        }
        for (ParticleSetting ps : particles.values()) {
            if (ps.isModified()) {
                return true;
            }
        }
        return false;
    }

    public void setModified(boolean modified) {
        this.modified = modified;
    }

    public Map<String, ParticleSetting> getParticles() {
        return particles;
    }

    public ParticleSetting getOrCreateParticleSetting(String particleId) {
        return particles.computeIfAbsent(particleId, k -> new ParticleSetting(this.enabled, this.alpha, this.culling, false));
    }

    public ParticleSetting getParticleSetting(String particleId) {
        return particles.get(particleId);
    }

    public void reset() {
        this.enabled = true;
        this.alpha = 1.0f;
        this.culling = 1.0f;
        this.modified = false;
        this.particles.clear();
    }

    public static class ParticleSetting {
        private boolean enabled = true;
        private float alpha = 1.0f;
        private float culling = 1.0f;
        private boolean modified = false;

        public ParticleSetting() {
        }

        public ParticleSetting(boolean enabled, float alpha, float culling, boolean modified) {
            this.enabled = enabled;
            this.alpha = alpha;
            this.culling = culling;
            this.modified = modified;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
            this.modified = true;
        }

        public float getAlpha() {
            return alpha;
        }

        public void setAlpha(float alpha) {
            this.alpha = Math.max(0.0f, Math.min(1.0f, alpha));
            this.modified = true;
        }

        public float getCulling() {
            return culling;
        }

        public void setCulling(float culling) {
            this.culling = Math.max(0.0f, Math.min(1.0f, culling));
            this.modified = true;
        }

        public boolean isModified() {
            return modified;
        }

        public void setModified(boolean modified) {
            this.modified = modified;
        }

        public void reset() {
            this.enabled = true;
            this.alpha = 1.0f;
            this.culling = 1.0f;
            this.modified = false;
        }
    }
}
