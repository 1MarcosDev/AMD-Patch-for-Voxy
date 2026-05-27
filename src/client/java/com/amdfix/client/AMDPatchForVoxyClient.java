package com.amdfix.client;

import com.amdfix.AMDPatchForVoxy;
import net.fabricmc.api.ClientModInitializer;

public class AMDPatchForVoxyClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		AMDPatchForVoxy.LOGGER.info("AMD Patch for Voxy client initialized.");
	}
}
