package com.amdfix;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AMDPatchForVoxy implements ModInitializer {
	public static final String MOD_ID = "amd-patch-for-voxy";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("AMD Patch for Voxy initialized.");
	}
}
