package io.github.thesmoothrere.combattell.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;
import org.jspecify.annotations.NullMarked;

@Environment(EnvType.CLIENT)
public class TextParticleGroup extends ParticleGroup<TextParticle> {
    private final ParticleRenderType particleType;
    final TextParticleRenderState particleRenderState;

    public TextParticleGroup(ParticleEngine engine) {
        super(engine);
        this.particleType = TextParticleRenderType.TEXT;
        this.particleRenderState  = new TextParticleRenderState();
    }

    @Override
    @NullMarked
    public ParticleGroupRenderState extractRenderState(Frustum frustum, Camera camera, float partialTickTime) {
        for (TextParticle particle : this.particles) {
            if (frustum.pointInFrustum(particle.getX(), particle.getY(), particle.getZ())) {
                try {
                    particle.extract(this.particleRenderState, camera, partialTickTime);
                } catch (Exception exception) {
                    CrashReport report = CrashReport.forThrowable(exception, "Rendering Particle");
                    CrashReportCategory category = report.addCategory("Particle being rendered");
                    category.setDetail("Particle", particle::toString);
                    category.setDetail("Particle Type", this.particleType::toString);
                    throw new ReportedException(report);
                }
            }
        }

        return this.particleRenderState;
    }
}
