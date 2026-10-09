package particleeraser.context;

public class ParticleRenderContext {
    private static final ThreadLocal<Float> CURRENT_ALPHA = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> IN_SPELL_PACKET = new ThreadLocal<>();

    public static void set(float alpha) {
        CURRENT_ALPHA.set(alpha);
        IN_SPELL_PACKET.set(Boolean.TRUE);
    }

    public static Float getAlpha() {
        return CURRENT_ALPHA.get();
    }

    public static boolean hasAlpha() {
        return CURRENT_ALPHA.get() != null;
    }

    public static boolean isInSpellPacket() {
        return Boolean.TRUE.equals(IN_SPELL_PACKET.get());
    }

    public static void clear() {
        CURRENT_ALPHA.remove();
        IN_SPELL_PACKET.remove();
    }
}
