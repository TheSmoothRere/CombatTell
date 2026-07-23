package io.github.thesmoothrere.combattell.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.ArrayListDeque;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Deque;

@Environment(EnvType.CLIENT)
public final class ParticleManager {
    private static final float TORSO_HEIGHT_MULTIPLIER = 0.65f; // Where vertically on the entity's body to target
    private static final double HITBOX_SAFETY_BUFFER = 0.35;   // Distance pushed out past the hitbox boundary
    private static final float SIDE_SPREAD_MAX = 1.1f;         // Max spread range scale for left/right variance
    private static final float SIDE_SPREAD_BIAS = 0.5f;       // Horizontal centering bias (0.5f is perfectly centered)

    private static final float PARTICLE_SCALE = 0.025f;          // Display scale size of the rendered numbers
    private static final int TEXT_COLOR = 0xFF0000;            // Base RGB color (Red)

    private static final Deque<TextParticle> PARTICLES = new ArrayListDeque<>();

    private ParticleManager() {
        /* This utility class should not be instantiated */
    }

    public static void spawnDamageParticle(LivingEntity entity, float damage) {
        if (damage == 0) return; // return early if there is no damage

        String damageText = String.format("%.1f", damage);

        // 1. Base center position of the entity's torso using our configurable height factor
        Vec3 entityCenter = entity.position().add(0, entity.getBbHeight() * TORSO_HEIGHT_MULTIPLIER, 0);

        Minecraft minecraft = Minecraft.getInstance();

        // 2. Compute the vector pointing from the entity to the player's camera
        Vec3 cameraPos = minecraft.gameRenderer.getMainCamera().position();
        Vec3 toCamera = cameraPos.subtract(entityCenter);

        Vec3 spawnPos = entityCenter;

        if (toCamera.lengthSqr() > 0.001) {
            // Flatten the vector to the horizontal plane
            Vec3 forward3D = toCamera.normalize();

            // Calculate the camera's relative "right" vector
            Vec3 relativeRight = new Vec3(0.0, 1.0, 0.0).cross(forward3D).normalize();

            // 3. Distance math: Pull out in front of the hitbox using the buffer constant
            double frontPushDistance = (entity.getBbWidth() / 2.0) + HITBOX_SAFETY_BUFFER;

            // 4. Spread math: Configurable left/right horizontal deviation
            double randomSideOffset = (entity.getRandom().nextFloat() - SIDE_SPREAD_BIAS) * SIDE_SPREAD_MAX;

            // Combine positions
            spawnPos = entityCenter
                    .add(forward3D.scale(frontPushDistance))
                    .add(relativeRight.scale(randomSideOffset));
        }

        ensureParticleLimit(minecraft);

        // Passed as a placeholder layout since physics movement now handles custom vertical increments
        Vec3 tinyVelocity = Vec3.ZERO;

        float scaleMultiplier = Mth.sqrt(entity.getScale());
        float initialScale = PARTICLE_SCALE * scaleMultiplier;

        TextParticle particle = new TextParticle(
                (ClientLevel) entity.level(),
                spawnPos,
                tinyVelocity,
                damageText,
                initialScale,
                TEXT_COLOR
        );

        PARTICLES.add(particle);
        minecraft.particleEngine.add(particle);
    }

    private static void ensureParticleLimit(Minecraft minecraft) {
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
