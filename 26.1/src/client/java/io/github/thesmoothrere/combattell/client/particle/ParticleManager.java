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
import net.minecraft.util.ArrayListDeque;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Deque;

@Environment(EnvType.CLIENT)
public final class ParticleManager {
    private static final CombatTellConfig CONFIG = ConfigManager.get(CombatTellConfig.class);

    private static final float TORSO_HEIGHT_MULTIPLIER = 0.65f; // Where vertically on the entity's body to target
    private static final double HITBOX_SAFETY_BUFFER = 0.42;   // Distance pushed out past the hitbox boundary
    private static final float SIDE_SPREAD_MAX = 1.3f;         // Max spread range scale for left/right variance
    private static final float SIDE_SPREAD_BIAS = 0.5f;       // Horizontal centering bias (0.5f is perfectly centered)

    private static final int DAMAGE_COLOR = 0xFF0000;            // Base RGB color (Red)
    private static final int HEAL_COLOR = 0x00FF00;

    private static final Deque<TextParticle> PARTICLES = new ArrayListDeque<>();

    private ParticleManager() {
        /* This utility class should not be instantiated */
    }

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
        String health = isDamage ? String.format("-%.1f", absoluteAmount) : String.format("+%.1f", absoluteAmount);
        int color = getHealtDeltaColor(isDamage);

        processTextParticle(entity, health, color);
    }

    private static int getHealtDeltaColor(boolean isDamage) {
        int damageColor = ColorUtils.parseHexColor(CONFIG.damageColor().getValue(), CONFIG.damageColor().getDefaultValue(), DAMAGE_COLOR);
        int healColor = ColorUtils.parseHexColor(CONFIG.healColor().getValue(), CONFIG.healColor().getDefaultValue(), HEAL_COLOR);
        return isDamage ? damageColor : healColor;
    }

    private static void processTextParticle(LivingEntity entity, String health, int color) {
        Minecraft minecraft = Minecraft.getInstance();
        ensureParticleLimit(minecraft);

        float bbHeight = entity.getBbHeight();
        double baseHeightOffset = bbHeight * TORSO_HEIGHT_MULTIPLIER;

        // 2. Compute the vector pointing from the entity to the player's camera
        Vec3 cameraPos = minecraft.gameRenderer.getMainCamera().position();

        Vec3 toCameraFromFeet = cameraPos.subtract(entity.position());

        if (toCameraFromFeet.lengthSqr() > 0.001) {
            Vec3 direction = toCameraFromFeet.normalize();
            // direction.y represents the vertical pitch angle (-1.0 looking straight down, 1.0 looking straight up)
            // If we are looking DOWN (direction.y is positive because camera is higher than feet),
            // we dynamically increase the height offset to push the health up over the entity's head.
            double dynamicVerticalShift = direction.y * (bbHeight * 0.5);
            baseHeightOffset += dynamicVerticalShift;
        }

        Vec3 entityCenter = entity.position().add(0, baseHeightOffset, 0);
        Vec3 toCamera = cameraPos.subtract(entityCenter);

        Vec3 spawnPos = entityCenter;

        if (toCamera.lengthSqr() > 0.001) {
            // Flatten the vector to the horizontal plane
            Vec3 forward3D = toCamera.normalize();

            Vec3 relativeSide = new Vec3(0.0, 1.0, 0.0).cross(forward3D).normalize();

            // 3. Distance math: Pull out in front of the hitbox using the buffer constant
            double frontPushDistance = (entity.getBbWidth() / 2.0) + HITBOX_SAFETY_BUFFER;

            // 4. Spread math: Configurable left/right horizontal deviation
            double randomSideOffset = (entity.getRandom().nextFloat() - SIDE_SPREAD_BIAS) * SIDE_SPREAD_MAX;

            // Combine positions
            spawnPos = entityCenter
                    .add(forward3D.scale(frontPushDistance))
                    .add(relativeSide.scale(randomSideOffset));
        }

        // Passed as a placeholder layout since physics movement now handles custom vertical increments
        float initialScale = CONFIG.baseParticleScale().getValue().floatValue() * entity.getScale();

        spawnTextParticle(entity, health, color, spawnPos, initialScale, minecraft);
    }

    private static void spawnTextParticle(LivingEntity entity, String health, int color, Vec3 spawnPos, float initialScale, Minecraft minecraft) {
        DoubleOption partilceLifetime = CONFIG.partilceLifetime();
        TextParticle.Data particleData = new TextParticle.Data(
                health,
                initialScale,
                color,
                TimeUtils.secondsToTicks(
                        partilceLifetime.getValue(),
                        partilceLifetime.getDefaultValue()
                ),
                CONFIG.particleRiseSpeed().getValue()
        );

        TextParticle particle = new TextParticle(
                (ClientLevel) entity.level(),
                spawnPos,
                Vec3.ZERO,
                particleData
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
