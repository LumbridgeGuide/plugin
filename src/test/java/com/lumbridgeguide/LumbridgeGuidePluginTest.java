package com.lumbridgeguide;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class LumbridgeGuidePluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(LumbridgeGuidePlugin.class);
		RuneLite.main(args);
	}
}