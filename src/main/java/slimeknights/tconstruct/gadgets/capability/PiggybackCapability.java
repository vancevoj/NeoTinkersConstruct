package slimeknights.tconstruct.gadgets.capability;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.capabilities.Capability;
import net.neoforged.neoforge.common.capabilities.CapabilityManager;
import net.neoforged.neoforge.common.capabilities.CapabilityToken;
import net.neoforged.neoforge.common.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.AttachCapabilitiesEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.javafmlmod.FMLJavaModLoadingContext;
import slimeknights.tconstruct.TConstruct;

/** Capability logic */
public class PiggybackCapability {
  private static final ResourceLocation ID = TConstruct.getResource("piggyback");
  public static final Capability<PiggybackHandler> PIGGYBACK = CapabilityManager.get(new CapabilityToken<>() {});

  private PiggybackCapability() {}

  /** Registers this capability */
  public static void register() {
    FMLJavaModLoadingContext.get().getModEventBus().addListener(EventPriority.NORMAL, false, RegisterCapabilitiesEvent.class, PiggybackCapability::register);
    NeoForge.EVENT_BUS.addGenericListener(Entity.class, PiggybackCapability::attachCapability);
  }

  /** Registers the capability with the event bus */
  private static void register(RegisterCapabilitiesEvent event) {
    event.register(PiggybackHandler.class);
  }

  /** Event listener to attach the capability */
  private static void attachCapability(AttachCapabilitiesEvent<Entity> event) {
    if (event.getObject() instanceof Player) {
      event.addCapability(ID, new PiggybackHandler((Player) event.getObject()));
    }
  }
}
