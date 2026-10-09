package particleeraser.data;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SkillRegistry {
    public static final SkillRegistry INSTANCE = new SkillRegistry();
    private static final Gson GSON = new Gson();

    private final Map<String, SkillInfo> skillsById = new ConcurrentHashMap<>();
    private final Map<String, List<String>> particleToSkills = new ConcurrentHashMap<>();
    private boolean initialized = false;

    public synchronized void initialize() {
        if (initialized) {
            return;
        }
        loadEmbeddedSkills();
        loadDynamicExileDbSkills();
        buildParticleIndex();
        initialized = true;
    }

    private void loadEmbeddedSkills() {
        InputStream is = getClass().getResourceAsStream("/data/cte2_skills.json");
        if (is == null) {
            is = getClass().getClassLoader().getResourceAsStream("data/cte2_skills.json");
        }
        if (is == null) {
            return;
        }
        try (InputStream stream = is) {
            List<SkillJsonModel> list = GSON.fromJson(
                    new InputStreamReader(stream, StandardCharsets.UTF_8),
                    new TypeToken<List<SkillJsonModel>>() {}.getType()
            );
            if (list != null) {
                for (SkillJsonModel model : list) {
                    if (model.id != null) {
                        Map<String, SkillInfo.ParticleMeta> metaMap = new HashMap<>();
                        if (model.particle_meta != null) {
                            for (Map.Entry<String, ParticleMetaJson> entry : model.particle_meta.entrySet()) {
                                ParticleMetaJson pj = entry.getValue();
                                if (pj != null) {
                                    metaMap.put(entry.getKey(), new SkillInfo.ParticleMeta(pj.count, pj.radius, pj.height, pj.shape));
                                }
                            }
                        }
                        SkillInfo existing = skillsById.get(model.id);
                        if (existing != null) {
                            Set<String> merged = new LinkedHashSet<>(existing.getParticles());
                            if (model.particles != null) {
                                merged.addAll(model.particles);
                            }
                            Set<String> mergedBurst = new LinkedHashSet<>(existing.getBurstParticles());
                            if (model.burst_particles != null) {
                                mergedBurst.addAll(model.burst_particles);
                            }
                            skillsById.put(model.id, new SkillInfo(model.id, model.name != null ? model.name : existing.getName(), new ArrayList<>(merged), new ArrayList<>(mergedBurst), metaMap, model.type != null ? model.type : existing.getType(), model.school != null ? model.school : existing.getSchool()));
                        } else {
                            skillsById.put(model.id, new SkillInfo(model.id, model.name, model.particles, model.burst_particles, metaMap, model.type, model.school));
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadDynamicExileDbSkills() {
        try {
            Class<?> exileDbClass = Class.forName("com.robertx22.mine_and_slash.database.registry.ExileDB");
            Method spellsMethod = exileDbClass.getMethod("Spells");
            Object spellsContainer = spellsMethod.invoke(null);
            if (spellsContainer != null) {
                Method getAllMethod = spellsContainer.getClass().getMethod("getAll");
                Map<?, ?> spellsMap = (Map<?, ?>) getAllMethod.invoke(spellsContainer);
                if (spellsMap != null) {
                    for (Map.Entry<?, ?> entry : spellsMap.entrySet()) {
                        String id = String.valueOf(entry.getKey());
                        Object spellObj = entry.getValue();
                        String locName = id;
                        try {
                            locName = (String) spellObj.getClass().getField("loc_name").get(spellObj);
                        } catch (Exception ignored) {
                        }
                        if (!skillsById.containsKey(id)) {
                            String school = id.startsWith("merc_") ? "merc" : "misc";
                            skillsById.put(id, new SkillInfo(id, locName, Collections.emptyList(), "Spell", school));
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private void buildParticleIndex() {
        particleToSkills.clear();
        for (SkillInfo skill : skillsById.values()) {
            for (String particle : skill.getParticles()) {
                particleToSkills.computeIfAbsent(particle, k -> new ArrayList<>()).add(skill.getId());
            }
        }
    }

    public List<SkillInfo> getAllSkills() {
        List<SkillInfo> list = new ArrayList<>(skillsById.values());
        list.sort(Comparator.comparing(SkillInfo::getName, String.CASE_INSENSITIVE_ORDER));
        return list;
    }

    public SkillInfo getSkill(String id) {
        return skillsById.get(id);
    }

    public List<String> findSkillsByParticle(String particleId) {
        if (particleId == null) {
            return Collections.emptyList();
        }
        List<String> list = particleToSkills.get(particleId);
        if (list == null && !particleId.contains(":")) {
            list = particleToSkills.get("minecraft:" + particleId);
        }
        return list != null ? list : Collections.emptyList();
    }

    private static class SkillJsonModel {
        String id;
        String name;
        List<String> particles;
        List<String> burst_particles;
        Map<String, ParticleMetaJson> particle_meta;
        String type;
        String school;
    }

    private static class ParticleMetaJson {
        int count = 10;
        float radius = 2.0f;
        float height = 0.5f;
        String shape = "CIRCLE";
    }
}
