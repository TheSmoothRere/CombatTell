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

        Vec3 spawnPos = entity.position().add(0, entity.getBbHeight() * 0.75, 0);
        Vec3 velocity = new Vec3(
                (entity.getRandom().nextFloat() - 0.5) * 0.1,
                0.15,
                (entity.getRandom().nextFloat() - 0.5) * 0.1
        );

        TextParticle particle = new TextParticle(
                (ClientLevel) entity.level(),
                spawnPos,
                velocity,
                damageText,
                1.0f,
                0xFF0000
        );

        Minecraft.getInstance().particleEngine.add(particle);
    }
}
