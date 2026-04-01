package com.example;

import com.lumbridgeguide.LumbridgeGuidePlugin;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class ExamplePluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(LumbridgeGuidePlugin.class);
		RuneLite.main(args);
	}
}