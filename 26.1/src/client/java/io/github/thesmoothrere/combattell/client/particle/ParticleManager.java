package io.github.thesmoothrere.combattell.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

@Environment(EnvType.CLIENT)
public final class ParticleManager {
    private ParticleManager() {
        /* This utility class should not be instantiated */
    }

    public static void spawnDamageParticle(LivingEntity entity, float damage) {
        String damageText = String.format("%.1f", damage);

        // 1. Base center position of the entity's torso
        Vec3 entityCenter = entity.position().add(0, entity.getBbHeight() * 0.7, 0);

        // 2. Compute the vector pointing from the entity to the player's camera
        Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().position();
        Vec3 toCamera = cameraPos.subtract(entityCenter);

        Vec3 spawnPos = entityCenter;

        if (toCamera.lengthSqr() > 0.001) {
            // Flatten the vector to the horizontal plane to prevent vertical camera angles from skewing the side math
            Vec3 forwardDirection = new Vec3(toCamera.x, 0.0, toCamera.z).normalize();

            // Calculate the camera's relative "right" vector via cross product
            Vec3 relativeRight = new Vec3(0.0, 1.0, 0.0).cross(forwardDirection).normalize();

            // 3. Distance math: Pull it completely out in front of the entity's body
            double frontPushDistance = (entity.getBbWidth() / 2.0) + 0.35;

            // 4. Spread math: Pick a random offset to the left or right (e.g., between -0.35 and +0.35 blocks)
            double randomSideOffset = (entity.getRandom().nextFloat() - 0.55f) * 0.7f;

            // Combine them: Start at center -> Push Forward (towards player) -> Offset Side (left/right)
            spawnPos = entityCenter
                    .add(forwardDirection.scale(frontPushDistance))
                    .add(relativeRight.scale(randomSideOffset));
        }

        // Tiny vertical drift velocity to keep it feeling dynamic
        Vec3 tinyVelocity = new Vec3(0.0, 0.015, 0.0);

        TextParticle particle = new TextParticle(
                (ClientLevel) entity.level(),
                spawnPos,
                tinyVelocity,
                damageText,
                1.0f,
                0xFF0000
        );

        Minecraft.getInstance().particleEngine.add(particle);
    }
}
