package io.github.thesmoothrere.combattell.client.mixin;

import io.github.thesmoothrere.combattell.client.particle.ParticleManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    // Unique client-only tracker field for each entity instance
    @Unique
    private float clientLastHealth = -1.0F;

    @Inject(
            method = "onSyncedDataUpdated",
            at = @At("HEAD") // Capture the old value BEFORE it is modified
    )
    private void captureOldHealth(EntityDataAccessor<?> accessor, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;

        if (entity.level().isClientSide() && LivingEntityAccessor.getHealthId().equals(accessor) && this.clientLastHealth < 0) {
            this.clientLastHealth = entity.getHealth();
        }

    }

    @Inject(
            method = "onSyncedDataUpdated",
            at = @At("TAIL") // Process after the data tracker updates
    )
    private void onHealthChange(EntityDataAccessor<?> accessor, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;

        if (entity.level().isClientSide() && LivingEntityAccessor.getHealthId().equals(accessor)) {
            float newHealth = entity.getHealth();

            // Check if they actually lost health (damage) rather than healing
            if (this.clientLastHealth != newHealth) {
                float healthDelta = this.clientLastHealth - newHealth;

                // Pass both the entity and the delta down to the manager
                ParticleManager.onHealthChange(entity, healthDelta);
            }

            // Cache the current health value for the next network change event
            this.clientLastHealth = newHealth;
        }
    }
}
