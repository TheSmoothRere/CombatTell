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
    private static final double HITBOX_SAFETY_BUFFER = 0.42;   // Distance pushed out past the hitbox boundary
    private static final float SIDE_SPREAD_MAX = 1.3f;         // Max spread range scale for left/right variance
    private static final float SIDE_SPREAD_BIAS = 0.5f;       // Horizontal centering bias (0.5f is perfectly centered)

    private static final float PARTICLE_SCALE = 0.025f;          // Display scale size of the rendered numbers
    private static final int COLOR_DAMAGE = 0xFF0000;            // Base RGB color (Red)
    private static final int COLOR_HEAL = 0x00FF00;

    private static final Deque<TextParticle> PARTICLES = new ArrayListDeque<>();

    private ParticleManager() {
        /* This utility class should not be instantiated */
    }

    public static void spawnTextParticle(LivingEntity entity, float healthDelta) {
        if (healthDelta == 0) return; // return early if there is no healthDelta

        // UPDATED: Determine dynamic color and string prefix markers based on polarity
        int finalColor;
        String finalRenderText;
        float absoluteAmount = Math.abs(healthDelta);

        if (healthDelta > 0) {
            // Health dropped -> Damage
            finalColor = COLOR_DAMAGE;
            finalRenderText = String.format("-%.1f", absoluteAmount);
        } else {
            // Health rose -> Heal
            finalColor = COLOR_HEAL;
            finalRenderText = String.format("+%.1f", absoluteAmount);
        }

        Minecraft minecraft = Minecraft.getInstance();

        float bbHeight = entity.getBbHeight();
        double baseHeightOffset = bbHeight * TORSO_HEIGHT_MULTIPLIER;

        // 2. Compute the vector pointing from the entity to the player's camera
        Vec3 cameraPos = minecraft.gameRenderer.getMainCamera().position();

        Vec3 toCameraFromFeet = cameraPos.subtract(entity.position());

        if (toCameraFromFeet.lengthSqr() > 0.001) {
            Vec3 direction = toCameraFromFeet.normalize();
            // direction.y represents the vertical pitch angle (-1.0 looking straight down, 1.0 looking straight up)
            // If we are looking DOWN (direction.y is positive because camera is higher than feet),
            // we dynamically increase the height offset to push the text up over the entity's head.
            double dynamicVerticalShift = direction.y * (bbHeight * 0.4);
            baseHeightOffset += dynamicVerticalShift;
        }

        Vec3 entityCenter = entity.position().add(0, baseHeightOffset, 0);
        Vec3 toCamera = cameraPos.subtract(entityCenter);

        Vec3 spawnPos = entityCenter;
        double distance = 0.0;

        if (toCamera.lengthSqr() > 0.001) {
            // Flatten the vector to the horizontal plane
            Vec3 forward3D = toCamera.normalize();

            // Capture the actual linear distance to the player camera
            distance = toCamera.length();

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

        // ----- RPG-STYLE DISTANCE SCALING -----
        // 1. Start with the default base particle scale modified by the entity's model scale
        float finalDynamicScale = scaleBasedDistanceFactor(entity, distance);

        TextParticle particle = new TextParticle(
                (ClientLevel) entity.level(),
                spawnPos,
                tinyVelocity,
                finalRenderText,
                finalDynamicScale,
                finalColor
        );

        PARTICLES.add(particle);
        minecraft.particleEngine.add(particle);
    }

    private static float scaleBasedDistanceFactor(LivingEntity entity, double distance) {
        float baseScale = PARTICLE_SCALE * entity.getScale();

        // 2. Calculate a distance multiplier.
        // For example: At 10 blocks away, scale becomes 1.0. At 30 blocks away, scale increases.
        float distanceMultiplier = (float) (distance * 0.15);

        // 3. Clamp the multiplier so text doesn't become tiny up close or giant from chunks away.
        // Min multiplier = 1.0f (won't shrink below normal size up close)
        // Max multiplier = 5.0f (limits maximum size at long range)
        float clampedDistanceScale = Mth.clamp(distanceMultiplier, 1.0F, 5.0F);

        // 4. Combine them for the final layout healthDelta
        return baseScale * clampedDistanceScale;
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
