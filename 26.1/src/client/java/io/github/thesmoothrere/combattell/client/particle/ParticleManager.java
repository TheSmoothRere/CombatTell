package io.github.thesmoothrere.combattell.client.particle;

import io.github.thesmoothrere.combattell.config.CombatTellConfig;
import io.github.thesmoothrere.combattell.util.ColorUtils;
import io.github.thesmoothrere.combattell.util.TimeUtils;
import io.github.thesmoothrere.relib.config.ConfigManager;
import io.github.thesmoothrere.relib.config.option.DoubleOption;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.util.ArrayListDeque;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

import java.util.Deque;

@Environment(EnvType.CLIENT)
public final class ParticleManager {
    private static final CombatTellConfig CONFIG = ConfigManager.get(CombatTellConfig.class);
    private static final ParticleConfig CONFIG_SCRATCHPAD = new ParticleConfig();

    private static final int DAMAGE_COLOR = 0xFF0000;            // Base RGB color (Red)
    private static final int HEAL_COLOR = 0x00FF00;

    private static final Deque<TextParticle> PARTICLES = new ArrayListDeque<>();

    private static final Vector3d camPos = new Vector3d();
    private static final Vector3d entPos = new Vector3d();
    private static final Vector3d dir = new Vector3d();
    private static final Vector3d side = new Vector3d();
    private static final Vector3d spawn = new Vector3d();

    private static final String[] DAMAGE_CACHE = new String[100];
    private static final String[] HEAL_CACHE = new String[100];
    static {
        for (int i = 0; i < 100; i++) {
            DAMAGE_CACHE[i] = "-" + i + ".0";
            HEAL_CACHE[i] = "+" + i + ".0";
        }
    }

    private ParticleManager() {
        /* This utility class should not be instantiated */
    }

    // TODO: fix particle should not spawning when first join/load the world
    public static void onHealthChange(LivingEntity entity, float healthDelta) {
        if (healthDelta == 0) return; // return early if there is no healthDelta

        boolean isDamage = healthDelta > 0;

        switch (CONFIG.particleDisplays().getValue()) {
            case DAMAGE:
                if (!isDamage) return;
                break;
            case HEAL:
                if (isDamage) return;
                break;
            case DAMAGE_HEAL:
                break;
        }

        float absoluteAmount = Math.abs(healthDelta);
        String health;
        if (absoluteAmount == (int)absoluteAmount && absoluteAmount < 100) {
            health = isDamage ? DAMAGE_CACHE[(int)absoluteAmount] : HEAL_CACHE[(int)absoluteAmount];
        } else {
            health = isDamage ? String.format("-%.1f", absoluteAmount) : String.format("+%.1f", absoluteAmount);
        }

        int color = getHealthDeltaColor(isDamage);
        processTextParticle(entity, health, color);
    }

    private static int getHealthDeltaColor(boolean isDamage) {
        int damageColor = ColorUtils.parseHexColor(
                CONFIG.damageColor().getValue(),
                CONFIG.damageColor().getDefaultValue(),
                DAMAGE_COLOR
        );
        int healColor = ColorUtils.parseHexColor(
                CONFIG.healColor().getValue(),
                CONFIG.healColor().getDefaultValue(),
                HEAL_COLOR
        );

        return isDamage ? damageColor : healColor;
    }

    private static void processTextParticle(LivingEntity entity, String health, int color) {
        Minecraft minecraft = Minecraft.getInstance();
        ensureParticleLimit(minecraft);

        float bbHeight = entity.getBbHeight();
        double baseHeightOffset = bbHeight * CONFIG.torsoHeightMultiplier().getValue();

        // 2. Compute the vector pointing from the entity to the player's camera
        Vec3 vanillaCam = minecraft.gameRenderer.getMainCamera().position();
        Vec3 vanillaEnt = entity.position();

        camPos.set(vanillaCam.x, vanillaCam.y, vanillaCam.z);
        entPos.set(vanillaEnt.x, vanillaEnt.y, vanillaEnt.z);

        camPos.sub(entPos, dir);

        if (dir.lengthSquared() > 0.001) {
            dir.normalize();
            baseHeightOffset += dir.y * (bbHeight * 0.5);
        }

        spawn.set(vanillaEnt.x, vanillaEnt.y + baseHeightOffset, vanillaEnt.z);

        camPos.set(vanillaCam.x, vanillaCam.y, vanillaCam.z);
        camPos.sub(spawn, dir);

        if (dir.lengthSquared() > 0.001) {
            dir.normalize(); // forward3D
            side.set(0.0, 1.0, 0.0).cross(dir).normalize();

            double frontPushDistance = (entity.getBbWidth() / 2.0) + CONFIG.hitboxSafetyBuffer().getValue();
            double randomSideOffset = (entity.getRandom().nextFloat() - 0.5f) * CONFIG.maxSpreadSide().getValue();

            // Apply push modifications directly to mutable coordinates
            spawn.add(dir.x * frontPushDistance, dir.y * frontPushDistance, dir.z * frontPushDistance);
            spawn.add(side.x * randomSideOffset, side.y * randomSideOffset, side.z * randomSideOffset);
        }

        // Passed as a placeholder layout since physics movement now handles custom vertical increments
        float initialScale = CONFIG.baseParticleScale().getValue().floatValue() * entity.getScale();

        Vec3 finalSpawnPos = new Vec3(spawn.x, spawn.y, spawn.z);
        spawnTextParticle(entity, health, color, finalSpawnPos, initialScale, minecraft);
    }

    private static void spawnTextParticle(LivingEntity entity, String health, int color, Vec3 spawnPos, float initialScale, Minecraft minecraft) {
        DoubleOption particleLifetime = CONFIG.particleLifetime();
        CONFIG_SCRATCHPAD.set(
                health,
                initialScale,
                color,
                TimeUtils.secondsToTicks(particleLifetime.getValue(), particleLifetime.getDefaultValue()),
                CONFIG.particleRiseSpeed().getValue()
        );

        TextParticle particle = new TextParticle(
                (ClientLevel) entity.level(),
                spawnPos,
                Vec3.ZERO,
                CONFIG_SCRATCHPAD
        ).setDistanceFactor(CONFIG.distanceFactor().getValue().floatValue()
        ).setMaxCeiling(CONFIG.maxCeiling().getValue().floatValue());

        PARTICLES.add(particle);
        minecraft.particleEngine.add(particle);
    }

    private static void ensureParticleLimit(Minecraft minecraft) {
        PARTICLES.removeIf(Particle::isAlive);

        int particleLimit = switch (minecraft.options.particles().get()) {
            case ALL -> 255;
            case DECREASED -> 127;
            case MINIMAL -> 63;
        };

        while (PARTICLES.size() > particleLimit) {
            TextParticle oldestParticle = PARTICLES.poll();
            if (oldestParticle != null) oldestParticle.remove();
        }
    }
}
