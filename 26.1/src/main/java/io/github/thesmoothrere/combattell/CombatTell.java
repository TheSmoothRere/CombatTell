package io.github.thesmoothrere.combattell;

import net.fabricmc.api.ModInitializer;

public class CombatTell implements ModInitializer {
    @Override
    public void onInitialize() {
        Constants.LOGGER.info(Constants.MOD_NAME + " initialized!");
    }
}
