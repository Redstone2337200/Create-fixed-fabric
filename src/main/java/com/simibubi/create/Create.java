package com.simibubi.create;

import java.util.Random;

import io.github.tropheusj.milk.Milk;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

import org.slf4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import com.simibubi.create.api.behaviour.BlockSpoutingBehaviour;
import com.simibubi.create.compat.Mods;
import com.simibubi.create.compat.computercraft.ComputerCraftProxy;
import com.simibubi.create.compat.trinkets.Trinkets;
import com.simibubi.create.content.contraptions.ContraptionMovementSetting;
import com.simibubi.create.content.decoration.palettes.AllPaletteBlocks;
import com.simibubi.create.content.equipment.potatoCannon.BuiltinPotatoProjectileTypes;
import com.simibubi.create.content.fluids.tank.BoilerHeaters;
import com.simibubi.create.content.kinetics.TorquePropagator;
import com.simibubi.create.content.kinetics.fan.processing.AllFanProcessingTypes;
import com.simibubi.create.content.kinetics.mechanicalArm.AllArmInteractionPointTypes;
import com.simibubi.create.content.redstone.displayLink.AllDisplayBehaviours;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler;
import com.simibubi.create.content.schematics.ServerSchematicLoader;
import com.simibubi.create.content.trains.GlobalRailwayManager;
import com.simibubi.create.content.trains.bogey.BogeySizes;
import com.simibubi.create.content.trains.track.AllPortalTracks;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.advancement.AllTriggers;
import com.simibubi.create.foundation.block.CopperRegistries;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.data.DamageTypeTagGen;
import com.simibubi.create.foundation.data.TagGen;
import com.simibubi.create.foundation.data.TagLangGen;
import com.simibubi.create.foundation.data.recipe.MechanicalCraftingRecipeGen;
import com.simibubi.create.foundation.data.recipe.ProcessingRecipeGen;
import com.simibubi.create.foundation.data.recipe.SequencedAssemblyRecipeGen;
import com.simibubi.create.foundation.data.recipe.StandardRecipeGen;
import com.simibubi.create.foundation.events.CommonEvents;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipHelper.Palette;
import com.simibubi.create.foundation.item.TooltipModifier;
import com.simibubi.create.foundation.ponder.FabricPonderProcessing;
import com.simibubi.create.foundation.recipe.AllIngredients;
import com.simibubi.create.foundation.utility.AttachedRegistry;
import com.simibubi.create.infrastructure.command.ServerLagger;
import com.simibubi.create.infrastructure.config.AllConfigs;
import com.simibubi.create.infrastructure.data.CreateDatagen;
import com.simibubi.create.infrastructure.data.CreateRecipeSerializerTagsProvider;
import com.simibubi.create.infrastructure.data.GeneratedEntriesProvider;
import com.simibubi.create.infrastructure.worldgen.AllBiomeModifiers;
import com.simibubi.create.infrastructure.worldgen.AllFeatures;
import com.simibubi.create.infrastructure.worldgen.AllPlacementModifiers;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public class Create implements ModInitializer {

	public static final String ID = "create";
	public static final String NAME = "Create";
	public static final String VERSION = "0.5.1f";

	public static final Logger LOGGER = LogUtils.getLogger();

	public static final Gson GSON = new GsonBuilder().setPrettyPrinting()
		.disableHtmlEscaping()
		.create();

	/** Use the {@link Random} of a local {@link Level} or {@link Entity} or create one */
	@Deprecated
	public static final Random RANDOM = new Random();

	/**
	 * <b>Other mods should not use this field!</b> If you are an addon developer, create your own instance of
	 * {@link CreateRegistrate}.
	 */
	public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(ID);

	static {
		REGISTRATE.setTooltipModifierFactory(item -> {
			return new ItemDescription.Modifier(item, Palette.STANDARD_CREATE)
				.andThen(TooltipModifier.mapNull(KineticStats.create(item)));
		});
	}

	public static final ServerSchematicLoader SCHEMATIC_RECEIVER = new ServerSchematicLoader();
	public static final RedstoneLinkNetworkHandler REDSTONE_LINK_NETWORK_HANDLER = new RedstoneLinkNetworkHandler();
	public static final TorquePropagator TORQUE_PROPAGATOR = new TorquePropagator();
	public static final GlobalRailwayManager RAILWAYS = new GlobalRailwayManager();
	public static final ServerLagger LAGGER = new ServerLagger();

	@Override
	public void onInitialize() { // onCtor
		AllSoundEvents.prepare();
		AllTags.init();
		AllCreativeModeTabs.register();
		AllBlocks.register();
		AllItems.register();
		AllFluids.register();
		AllPaletteBlocks.register();
		AllMenuTypes.register();
		AllEntityTypes.register();
		AllBlockEntityTypes.register();
		AllEnchantments.register();
		AllRecipeTypes.register();

		// fabric exclusive, squeeze this in here to register before stuff is used
		REGISTRATE.register();

		AllParticleTypes.register();
		AllStructureProcessorTypes.register();
		AllEntityDataSerializers.register();
		AllPackets.registerPackets();
		AllFeatures.register();
		AllPlacementModifiers.register();

		AllConfigs.register();

		// FIXME: some of these registrations are not thread-safe
		AllMovementBehaviours.registerDefaults();
		AllInteractionBehaviours.registerDefaults();
		AllPortalTracks.registerDefaults();
		AllDisplayBehaviours.registerDefaults();
		ContraptionMovementSetting.registerDefaults();
		AllArmInteractionPointTypes.register();
		AllFanProcessingTypes.register();
		BlockSpoutingBehaviour.registerDefaults();
		BogeySizes.init();
		AllBogeyStyles.register();
		// ----

		ComputerCraftProxy.register();

		Milk.enableMilkFluid();
		CopperRegistries.inject();

		Create.init();
//		modEventBus.addListener(EventPriority.LOW, CreateDatagen::gatherData); // CreateData entrypoint
		AllSoundEvents.register();

		// causes class loading issues or something
		// noinspection Convert2MethodRef
		Mods.TRINKETS.executeIfInstalled(() -> () -> Trinkets.init());

		// fabric exclusive
		AllIngredients.register();
		CommonEvents.register();
		AllPackets.getChannel().initServerListener();
		FabricPonderProcessing.init();
		AllBiomeModifiers.bootstrap(); // moved out of datagen
	}

	public static void init() {
		AllFluids.registerFluidInteractions();

//		event.enqueueWork(() -> {
			// TODO: custom registration should all happen in one place
			// Most registration happens in the constructor.
			// These registrations use Create's registered objects directly so they must run after registration has finished.
			BuiltinPotatoProjectileTypes.register();
			BoilerHeaters.registerDefaults();
			AllFluids.registerFluidInteractions();
//		--

			// fabric: AttachedRegistry.unwrapAll moved to RegistryMixin
			AllAdvancements.register();
			AllTriggers.register();
//		});
	}

	public static ResourceLocation asResource(String path) {
		return new ResourceLocation(ID, path);
	}

}

