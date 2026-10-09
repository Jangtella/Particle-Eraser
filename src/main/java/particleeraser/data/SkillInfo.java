package particleeraser.data;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SkillInfo {
    private final String id;
    private final String name;
    private final List<String> particles;
    private final List<String> burstParticles;
    private final Map<String, ParticleMeta> particleMeta;
    private final String type;
    private final String school;

    public SkillInfo(String id, String name, List<String> particles, List<String> burstParticles,
                     Map<String, ParticleMeta> particleMeta, String type, String school) {
        this.id = id;
        this.name = name;
        this.particles = particles != null ? particles : Collections.emptyList();
        this.burstParticles = burstParticles != null ? burstParticles : Collections.emptyList();
        this.particleMeta = particleMeta != null ? particleMeta : Collections.emptyMap();
        this.type = type != null ? type : "Spell";
        this.school = school != null ? school.toLowerCase(Locale.ROOT) : "misc";
    }

    public SkillInfo(String id, String name, List<String> particles, List<String> burstParticles, String type, String school) {
        this(id, name, particles, burstParticles, Collections.emptyMap(), type, school);
    }

    public SkillInfo(String id, String name, List<String> particles, String type, String school) {
        this(id, name, particles, Collections.emptyList(), Collections.emptyMap(), type, school);
    }

    public SkillInfo(String id, String name, List<String> particles, String type) {
        this(id, name, particles, Collections.emptyList(), Collections.emptyMap(), type, "misc");
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<String> getParticles() {
        return particles;
    }

    public List<String> getBurstParticles() {
        return burstParticles;
    }

    public ParticleMeta getParticleMeta(String particle) {
        if (particle == null || particleMeta.isEmpty()) {
            return new ParticleMeta();
        }
        if (particleMeta.containsKey(particle)) {
            return particleMeta.get(particle);
        }
        String normalized = particle.contains(":") ? particle : "minecraft:" + particle;
        if (particleMeta.containsKey(normalized)) {
            return particleMeta.get(normalized);
        }
        String shortName = particle.contains(":") ? particle.substring(particle.indexOf(':') + 1) : particle;
        for (Map.Entry<String, ParticleMeta> e : particleMeta.entrySet()) {
            String k = e.getKey();
            String shortK = k.contains(":") ? k.substring(k.indexOf(':') + 1) : k;
            if (shortK.equalsIgnoreCase(shortName)) {
                return e.getValue();
            }
        }
        return new ParticleMeta();
    }

    public boolean isBurstParticle(String particle) {
        if (particle == null) {
            return false;
        }
        if (burstParticles.contains(particle)) {
            return true;
        }
        String normalized = particle.contains(":") ? particle : "minecraft:" + particle;
        if (burstParticles.contains(normalized)) {
            return true;
        }
        for (String bp : burstParticles) {
            String shortBp = bp.contains(":") ? bp.substring(bp.indexOf(':') + 1) : bp;
            String shortP = particle.contains(":") ? particle.substring(particle.indexOf(':') + 1) : particle;
            if (shortBp.equalsIgnoreCase(shortP)) {
                return true;
            }
        }
        return false;
    }

    public String getType() {
        return type;
    }

    public String getSchool() {
        return school;
    }

    public String getSearchableText() {
        return (name + " " + id + " " + String.join(" ", particles)).toLowerCase(Locale.ROOT);
    }

    public static class ParticleMeta {
        public int count = 10;
        public float radius = 2.0f;
        public float height = 0.5f;
        public String shape = "CIRCLE";

        public ParticleMeta() {
        }

        public ParticleMeta(int count, float radius, float height, String shape) {
            this.count = count;
            this.radius = radius;
            this.height = height;
            this.shape = shape != null ? shape : "CIRCLE";
        }
    }
}
