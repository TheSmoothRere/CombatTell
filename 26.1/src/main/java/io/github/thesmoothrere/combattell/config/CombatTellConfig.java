package io.github.thesmoothrere.combattell.config;

import io.github.thesmoothrere.combattell.Constants;
import io.github.thesmoothrere.combattell.util.ParticleDisplays;
import io.github.thesmoothrere.relib.api.Config;
import io.github.thesmoothrere.relib.api.ConfigApi;
import io.github.thesmoothrere.relib.config.option.DoubleOption;
import io.github.thesmoothrere.relib.config.option.EnumOption;
import io.github.thesmoothrere.relib.config.option.StringOption;

@Config(name = Constants.MOD_ID)
public class CombatTellConfig implements ConfigApi {
    private final EnumOption<ParticleDisplays> particleDisplays = new EnumOption<>("particleDisplays", ParticleDisplays.DAMAGE_HEAL);
    private final StringOption damageColor = new StringOption("damageColor", "#FF0000");
    private final StringOption healColor = new StringOption("healColor", "#00FF00");
    private final DoubleOption partilceLifetime = new DoubleOption("particleLifetime", 1.5, 0.1, 10.0);
    private final DoubleOption particleRiseSpeed = new DoubleOption("particleRiseSpeed", 0.012, 0.0, 5.0); // Lower to hover tighter, raise to drift faster
    private final DoubleOption baseParticleScale = new DoubleOption("baseParticleScale", 0.025, 0.025, 1.0);

    public EnumOption<ParticleDisplays> particleDisplays() {
        return particleDisplays;
    }

    public StringOption damageColor() {
        return damageColor;
    }

    public StringOption healColor() {
        return healColor;
    }

    public DoubleOption partilceLifetime() {
        return partilceLifetime;
    }

    public DoubleOption particleRiseSpeed() {
        return particleRiseSpeed;
    }

    public DoubleOption baseParticleScale() {
        return baseParticleScale;
    }
}
