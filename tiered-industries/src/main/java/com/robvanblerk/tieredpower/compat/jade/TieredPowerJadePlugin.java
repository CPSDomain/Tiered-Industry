package com.robvanblerk.tieredpower.compat.jade;

import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

import com.robvanblerk.tieredpower.block.BankControllerBlock;
import com.robvanblerk.tieredpower.block.BatteryBoxBlock;
import com.robvanblerk.tieredpower.block.CableBlock;
import com.robvanblerk.tieredpower.block.FusionControllerBlock;
import com.robvanblerk.tieredpower.block.MachineBlock;
import com.robvanblerk.tieredpower.block.entity.BankControllerBlockEntity;
import com.robvanblerk.tieredpower.block.entity.BatteryBoxBlockEntity;
import com.robvanblerk.tieredpower.block.entity.FusionControllerBlockEntity;
import com.robvanblerk.tieredpower.block.entity.MachineBlockEntity;

/**
 * Jade integration: extra lines when you look at Tiered Industries blocks. Jade already shows stored FE on its own;
 * this adds machine status, redstone mode, upgrades, battery rates, reactor and bank info, and cable rates.
 * Only loaded when Jade is installed.
 */
@WailaPlugin
public class TieredPowerJadePlugin implements IWailaPlugin {
	@Override
	public void register(IWailaCommonRegistration registration) {
		registration.registerBlockDataProvider(TieredPowerProviders.Machine.INSTANCE, MachineBlockEntity.class);
		registration.registerBlockDataProvider(TieredPowerProviders.Battery.INSTANCE, BatteryBoxBlockEntity.class);
		registration.registerBlockDataProvider(TieredPowerProviders.Fusion.INSTANCE, FusionControllerBlockEntity.class);
		registration.registerBlockDataProvider(TieredPowerProviders.Bank.INSTANCE, BankControllerBlockEntity.class);
		registration.registerBlockDataProvider(TieredPowerProviders.Pipe.INSTANCE, com.robvanblerk.tieredpower.block.entity.ItemPipeBlockEntity.class);
		registration.registerBlockDataProvider(TieredPowerProviders.Pipe.INSTANCE, com.robvanblerk.tieredpower.block.entity.FluidPipeBlockEntity.class);
		registration.registerBlockDataProvider(TieredPowerProviders.Generator.INSTANCE, com.robvanblerk.tieredpower.block.entity.SolarPanelBlockEntity.class);
		registration.registerBlockDataProvider(TieredPowerProviders.Generator.INSTANCE, com.robvanblerk.tieredpower.block.entity.WindTurbineBlockEntity.class);
		registration.registerBlockDataProvider(TieredPowerProviders.Generator.INSTANCE, com.robvanblerk.tieredpower.block.entity.FissionReactorBlockEntity.class);
		registration.registerBlockDataProvider(TieredPowerProviders.Generator.INSTANCE, com.robvanblerk.tieredpower.block.entity.FissionControllerBlockEntity.class);
		registration.registerBlockDataProvider(TieredPowerProviders.Generator.INSTANCE, com.robvanblerk.tieredpower.block.entity.GasBurnerGeneratorBlockEntity.class);
		registration.registerBlockDataProvider(TieredPowerProviders.Generator.INSTANCE, com.robvanblerk.tieredpower.block.entity.SteamEngineBlockEntity.class);
		registration.registerBlockDataProvider(TieredPowerProviders.Info.INSTANCE, com.robvanblerk.tieredpower.block.entity.StorageControllerBlockEntity.class);
		registration.registerBlockDataProvider(TieredPowerProviders.Info.INSTANCE, com.robvanblerk.tieredpower.block.entity.DriveBayBlockEntity.class);
		registration.registerBlockDataProvider(TieredPowerProviders.Info.INSTANCE, com.robvanblerk.tieredpower.block.entity.MolecularAssemblerBlockEntity.class);
		registration.registerBlockDataProvider(TieredPowerProviders.Info.INSTANCE, com.robvanblerk.tieredpower.block.entity.StockKeeperBlockEntity.class);
		registration.registerBlockDataProvider(TieredPowerProviders.Info.INSTANCE, com.robvanblerk.tieredpower.block.entity.LightningCollectorBlockEntity.class);
		registration.registerBlockDataProvider(TieredPowerProviders.Info.INSTANCE, com.robvanblerk.tieredpower.block.entity.StormCallerBlockEntity.class);
		registration.registerBlockDataProvider(TieredPowerProviders.Info.INSTANCE, com.robvanblerk.tieredpower.block.entity.GeothermalGeneratorBlockEntity.class);
		registration.registerBlockDataProvider(TieredPowerProviders.Info.INSTANCE, com.robvanblerk.tieredpower.block.entity.WaterWheelBlockEntity.class);
		registration.registerBlockDataProvider(TieredPowerProviders.Info.INSTANCE, com.robvanblerk.tieredpower.core.EnergyPylonBlockEntity.class);
		registration.registerBlockDataProvider(TieredPowerProviders.Info.INSTANCE, com.robvanblerk.tieredpower.core.EnergyCoreBlockEntity.class);
		registration.registerBlockDataProvider(TieredPowerProviders.Info.INSTANCE, com.robvanblerk.tieredpower.industry.DieselGeneratorBlockEntity.class);
		registration.registerBlockDataProvider(TieredPowerProviders.Info.INSTANCE, com.robvanblerk.tieredpower.block.entity.GasTankBlockEntity.class);
		registration.registerBlockDataProvider(TieredPowerProviders.Info.INSTANCE, com.robvanblerk.tieredpower.block.entity.DigitalMinerBlockEntity.class);
		registration.registerBlockDataProvider(TieredPowerProviders.Info.INSTANCE, com.robvanblerk.tieredpower.greenhouse.GreenhouseBlockEntity.class);
		registration.registerBlockDataProvider(TieredPowerProviders.Info.INSTANCE, com.robvanblerk.tieredpower.block.entity.FluidicPlenisherBlockEntity.class);
		registration.registerBlockDataProvider(TieredPowerProviders.Info.INSTANCE, com.robvanblerk.tieredpower.block.entity.LaserDrillBlockEntity.class);
		registration.registerBlockDataProvider(TieredPowerProviders.Info.INSTANCE, com.robvanblerk.tieredpower.block.entity.ReceiverDishBlockEntity.class);
		registration.registerBlockDataProvider(TieredPowerProviders.Info.INSTANCE, com.robvanblerk.tieredpower.block.entity.BufferBlockEntity.class);
		registration.registerBlockDataProvider(TieredPowerProviders.Info.INSTANCE, com.robvanblerk.tieredpower.block.entity.WirelessReceiverBlockEntity.class);
		registration.registerBlockDataProvider(TieredPowerProviders.Info.INSTANCE, com.robvanblerk.tieredpower.block.entity.WirelessSenderBlockEntity.class);
		registration.registerBlockDataProvider(TieredPowerProviders.Info.INSTANCE, com.robvanblerk.tieredpower.block.entity.AntimatterReactorBlockEntity.class);
		registration.registerBlockDataProvider(TieredPowerProviders.Info.INSTANCE, com.robvanblerk.tieredpower.block.entity.PowerReceiverBlockEntity.class);
		registration.registerBlockDataProvider(TieredPowerProviders.Info.INSTANCE, com.robvanblerk.tieredpower.block.entity.PowerTransmitterBlockEntity.class);
		registration.registerBlockDataProvider(TieredPowerProviders.Info.INSTANCE, com.robvanblerk.tieredpower.block.entity.AirSeparatorBlockEntity.class);
		registration.registerBlockDataProvider(TieredPowerProviders.Info.INSTANCE, com.robvanblerk.tieredpower.block.entity.CryoInjectorBlockEntity.class);
		registration.registerBlockDataProvider(TieredPowerProviders.Info.INSTANCE, com.robvanblerk.tieredpower.block.entity.TritiumBreederBlockEntity.class);
		registration.registerBlockDataProvider(TieredPowerProviders.Info.INSTANCE, com.robvanblerk.tieredpower.conveyor.ConveyorBlockEntity.class);
		registration.registerBlockDataProvider(TieredPowerProviders.Info.INSTANCE, com.robvanblerk.tieredpower.drone.DroneStationBlockEntity.class);
		registration.registerBlockDataProvider(TieredPowerProviders.Info.INSTANCE, com.robvanblerk.tieredpower.turbine.TurbineControllerBlockEntity.class);
		registration.registerBlockDataProvider(TieredPowerProviders.Info.INSTANCE, com.robvanblerk.tieredpower.stargate.StargateDialerBlockEntity.class);
		registration.registerBlockDataProvider(TieredPowerProviders.Info.INSTANCE, com.robvanblerk.tieredpower.quantum.QuantumChestBlockEntity.class);
		registration.registerBlockDataProvider(TieredPowerProviders.Info.INSTANCE, com.robvanblerk.tieredpower.redstone.WirelessRedstoneBlockEntity.class);
		registration.registerBlockDataProvider(TieredPowerProviders.Info.INSTANCE, com.robvanblerk.tieredpower.logic.MachineStatusDisplayBlockEntity.class);
	}

	@Override
	public void registerClient(IWailaClientRegistration registration) {
		registration.registerBlockComponent(TieredPowerProviders.Machine.INSTANCE, MachineBlock.class);
		registration.registerBlockComponent(TieredPowerProviders.Battery.INSTANCE, BatteryBoxBlock.class);
		registration.registerBlockComponent(TieredPowerProviders.Fusion.INSTANCE, FusionControllerBlock.class);
		registration.registerBlockComponent(TieredPowerProviders.Bank.INSTANCE, BankControllerBlock.class);
		registration.registerBlockComponent(TieredPowerProviders.Cable.INSTANCE, CableBlock.class);
		registration.registerBlockComponent(TieredPowerProviders.CraftingTierInfo.INSTANCE, com.robvanblerk.tieredpower.block.CraftingCpuBlock.class);
		registration.registerBlockComponent(TieredPowerProviders.CraftingTierInfo.INSTANCE, com.robvanblerk.tieredpower.block.matrix.CraftingAcceleratorBlock.class);
		registration.registerBlockComponent(TieredPowerProviders.CraftingTierInfo.INSTANCE, com.robvanblerk.tieredpower.block.MachineConnectorBlock.class);
		registration.registerBlockComponent(TieredPowerProviders.Generator.INSTANCE, com.robvanblerk.tieredpower.block.SolarPanelBlock.class);
		registration.registerBlockComponent(TieredPowerProviders.Generator.INSTANCE, com.robvanblerk.tieredpower.block.WindTurbineBlock.class);
		registration.registerBlockComponent(TieredPowerProviders.Generator.INSTANCE, com.robvanblerk.tieredpower.block.FissionReactorBlock.class);
		registration.registerBlockComponent(TieredPowerProviders.Generator.INSTANCE, com.robvanblerk.tieredpower.block.FissionControllerBlock.class);
		registration.registerBlockComponent(TieredPowerProviders.Generator.INSTANCE, com.robvanblerk.tieredpower.block.GasBurnerGeneratorBlock.class);
		registration.registerBlockComponent(TieredPowerProviders.Generator.INSTANCE, com.robvanblerk.tieredpower.block.SteamEngineBlock.class);
		registration.registerBlockComponent(TieredPowerProviders.Pipe.INSTANCE, com.robvanblerk.tieredpower.block.FluidPipeBlock.class);
		registration.registerBlockComponent(TieredPowerProviders.Pipe.INSTANCE, com.robvanblerk.tieredpower.block.ItemPipeBlock.class);
		registration.registerBlockComponent(TieredPowerProviders.Info.INSTANCE, com.robvanblerk.tieredpower.block.StorageControllerBlock.class);
		registration.registerBlockComponent(TieredPowerProviders.Info.INSTANCE, com.robvanblerk.tieredpower.block.DriveBayBlock.class);
		registration.registerBlockComponent(TieredPowerProviders.Info.INSTANCE, com.robvanblerk.tieredpower.block.MolecularAssemblerBlock.class);
		registration.registerBlockComponent(TieredPowerProviders.Info.INSTANCE, com.robvanblerk.tieredpower.block.StockKeeperBlock.class);
		registration.registerBlockComponent(TieredPowerProviders.Info.INSTANCE, com.robvanblerk.tieredpower.block.LightningCollectorBlock.class);
		registration.registerBlockComponent(TieredPowerProviders.Info.INSTANCE, com.robvanblerk.tieredpower.block.StormCallerBlock.class);
		registration.registerBlockComponent(TieredPowerProviders.Info.INSTANCE, com.robvanblerk.tieredpower.block.GeothermalGeneratorBlock.class);
		registration.registerBlockComponent(TieredPowerProviders.Info.INSTANCE, com.robvanblerk.tieredpower.block.WaterWheelBlock.class);
		registration.registerBlockComponent(TieredPowerProviders.Info.INSTANCE, com.robvanblerk.tieredpower.core.EnergyCoreBlock.class);
		registration.registerBlockComponent(TieredPowerProviders.Info.INSTANCE, com.robvanblerk.tieredpower.industry.DieselGeneratorBlock.class);
		registration.registerBlockComponent(TieredPowerProviders.Info.INSTANCE, com.robvanblerk.tieredpower.block.GasTankBlock.class);
		registration.registerBlockComponent(TieredPowerProviders.Info.INSTANCE, com.robvanblerk.tieredpower.block.DigitalMinerBlock.class);
		registration.registerBlockComponent(TieredPowerProviders.Info.INSTANCE, com.robvanblerk.tieredpower.greenhouse.GreenhouseBlock.class);
		registration.registerBlockComponent(TieredPowerProviders.Info.INSTANCE, com.robvanblerk.tieredpower.block.FluidicPlenisherBlock.class);
		registration.registerBlockComponent(TieredPowerProviders.Info.INSTANCE, com.robvanblerk.tieredpower.block.LaserDrillBlock.class);
		registration.registerBlockComponent(TieredPowerProviders.Info.INSTANCE, com.robvanblerk.tieredpower.block.LaunchControllerBlock.class);
		registration.registerBlockComponent(TieredPowerProviders.Info.INSTANCE, com.robvanblerk.tieredpower.block.BufferBlock.class);
		registration.registerBlockComponent(TieredPowerProviders.Info.INSTANCE, com.robvanblerk.tieredpower.block.WirelessTransportBlock.class);
		registration.registerBlockComponent(TieredPowerProviders.Info.INSTANCE, com.robvanblerk.tieredpower.block.AntimatterReactorBlock.class);
		registration.registerBlockComponent(TieredPowerProviders.Info.INSTANCE, com.robvanblerk.tieredpower.block.AssemblerPanelBlock.class);
		registration.registerBlockComponent(TieredPowerProviders.Info.INSTANCE, com.robvanblerk.tieredpower.block.PowerReceiverBlock.class);
		registration.registerBlockComponent(TieredPowerProviders.Info.INSTANCE, com.robvanblerk.tieredpower.block.PowerTransmitterBlock.class);
		registration.registerBlockComponent(TieredPowerProviders.Info.INSTANCE, com.robvanblerk.tieredpower.block.AirSeparatorBlock.class);
		registration.registerBlockComponent(TieredPowerProviders.Info.INSTANCE, com.robvanblerk.tieredpower.block.CryoInjectorBlock.class);
		registration.registerBlockComponent(TieredPowerProviders.Info.INSTANCE, com.robvanblerk.tieredpower.block.TritiumBreederBlock.class);
		registration.registerBlockComponent(TieredPowerProviders.Info.INSTANCE, com.robvanblerk.tieredpower.conveyor.ConveyorBlock.class);
		registration.registerBlockComponent(TieredPowerProviders.Info.INSTANCE, com.robvanblerk.tieredpower.drone.DroneStationBlock.class);
		registration.registerBlockComponent(TieredPowerProviders.Info.INSTANCE, com.robvanblerk.tieredpower.turbine.TurbineControllerBlock.class);
		registration.registerBlockComponent(TieredPowerProviders.Info.INSTANCE, com.robvanblerk.tieredpower.stargate.StargateDialerBlock.class);
		registration.registerBlockComponent(TieredPowerProviders.Info.INSTANCE, com.robvanblerk.tieredpower.quantum.QuantumChestBlock.class);
		registration.registerBlockComponent(TieredPowerProviders.Info.INSTANCE, com.robvanblerk.tieredpower.redstone.WirelessRedstoneBlock.class);
		registration.registerBlockComponent(TieredPowerProviders.Info.INSTANCE, com.robvanblerk.tieredpower.logic.MachineStatusDisplayBlock.class);
	}
}
