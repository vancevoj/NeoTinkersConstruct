package slimeknights.tconstruct.library.modifiers;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.netty.handler.codec.DecoderException;
import lombok.RequiredArgsConstructor;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import slimeknights.mantle.network.packet.ISimplePacket;
import slimeknights.mantle.network.packet.IThreadsafePacket;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.impl.ComposableModifier;
import slimeknights.tconstruct.library.utils.GenericTagUtil;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/** Packet to sync modifiers */
@RequiredArgsConstructor
public class UpdateModifiersPacket implements IThreadsafePacket {
  public static final Type<UpdateModifiersPacket> TYPE = new Type<>(TConstruct.getResource("update_modifiers"));
  public static final StreamCodec<RegistryFriendlyByteBuf,UpdateModifiersPacket> STREAM_CODEC = ISimplePacket.codec(UpdateModifiersPacket::new);

  /** Collection of all modifiers */
  private final Map<ModifierId,Modifier> allModifiers;
  /** Map of all modifier tags */
  private final Map<TagKey<Modifier>,List<Modifier>> tags;
  /** Collection of non-redirect modifiers */
  private Collection<ComposableModifier> modifiers;
  /** Map of modifier redirect ID pairs */
  private Map<ModifierId,ModifierId> redirects;
  /** Map of enchantment to modifier pair */
  private final Map<Holder<Enchantment>,Modifier> enchantmentMap;
  /** Collection of all enchantment tag mappings */
  private final Map<TagKey<Enchantment>, Modifier> enchantmentTagMappings;

  /** Ensures both the modifiers and redirects lists are calculated, allows one packet to be used multiple times without redundant work */
  private void ensureCalculated() {
    if (this.modifiers == null || this.redirects == null) {
      ImmutableList.Builder<ComposableModifier> modifiers = ImmutableList.builder();
      ImmutableMap.Builder<ModifierId,ModifierId> redirects = ImmutableMap.builder();
      for (Entry<ModifierId,Modifier> entry : allModifiers.entrySet()) {
        ModifierId id = entry.getKey();
        Modifier value = entry.getValue();
        ModifierId actual = value.getId();
        if (id.equals(actual)) {
          // we can't sync anything that is not composable
          if (value instanceof ComposableModifier composable) {
            modifiers.add(composable);
          } else {
            TConstruct.LOG.warn("Unable to sync modifier {} as its not ComposableModifier; got class {}", id, value.getClass().getName());
          }
        } else {
          redirects.put(id, actual);
        }
      }
      this.modifiers = modifiers.build();
      this.redirects = redirects.build();
    }
  }

  /** Gets a modifier by the given ID, falling back to the map if needed */
  private static Modifier getModifier(Map<ModifierId,Modifier> modifiers, ModifierId id) {
    Modifier modifier = ModifierManager.INSTANCE.getStatic(id);
    if (modifier == ModifierManager.INSTANCE.getDefaultValue()) {
      modifier = modifiers.get(id);
      if (modifier == null) {
        throw new DecoderException("Unknown modifier " + id);
      }
    }
    return modifier;
  }

  public UpdateModifiersPacket(FriendlyByteBuf buffer) {
    // read in modifiers
    int size = buffer.readVarInt();
    Map<ModifierId,Modifier> modifiers = new HashMap<>();
    for (int i = 0; i < size; i++) {
      ModifierId id = new ModifierId(buffer.readUtf(Short.MAX_VALUE));
      try {
        Modifier modifier = ComposableModifier.LOADER.decode(buffer, ModifierManager.contextBuilder(id).build());
        modifier.setId(id);
        modifiers.put(id, modifier);
      } catch (RuntimeException e) {
        TConstruct.LOG.error("Failed to decode modifier with ID {}", id, e);
        throw e;
      }
    }
    // read in redirects
    size = buffer.readVarInt();
    for (int i = 0; i < size; i++) {
      ModifierId from = new ModifierId(buffer.readUtf(Short.MAX_VALUE));
      modifiers.put(from, getModifier(modifiers, new ModifierId(buffer.readUtf(Short.MAX_VALUE))));
    }
    this.allModifiers = modifiers;
    this.tags = GenericTagUtil.decodeTags(buffer, ModifierManager.REGISTRY_KEY, id -> getModifier(modifiers, new ModifierId(id)));

    // read in enchantment to modifier mapping. enchantments are a datapack registry in 1.21, so resolve holders from the buffer's registry access
    RegistryAccess registryAccess = ((RegistryFriendlyByteBuf)buffer).registryAccess();
    var enchantmentRegistry = registryAccess.registryOrThrow(Registries.ENCHANTMENT);
    ImmutableMap.Builder<Holder<Enchantment>,Modifier> enchantmentBuilder = ImmutableMap.builder();
    size = buffer.readVarInt();
    for (int i = 0; i < size; i++) {
      ResourceLocation enchantmentId = buffer.readResourceLocation();
      Holder<Enchantment> holder = enchantmentRegistry.getHolder(enchantmentId)
        .orElseThrow(() -> new DecoderException("Unknown enchantment " + enchantmentId));
      enchantmentBuilder.put(holder, getModifier(modifiers, new ModifierId(buffer.readResourceLocation())));
    }
    enchantmentMap = enchantmentBuilder.build();
    ImmutableMap.Builder<TagKey<Enchantment>, Modifier> enchantmentTagBuilder = ImmutableMap.builder();
    size = buffer.readVarInt();
    for (int i = 0; i < size; i++) {
      enchantmentTagBuilder.put(
        TagKey.create(Registries.ENCHANTMENT, buffer.readResourceLocation()),
        getModifier(modifiers, new ModifierId(buffer.readResourceLocation())));
    }
    enchantmentTagMappings = enchantmentTagBuilder.build();
  }

  @Override
  public void encode(FriendlyByteBuf buffer) {
    ensureCalculated();
    // write modifiers
    buffer.writeVarInt(modifiers.size());
    for (ComposableModifier modifier : modifiers) {
      ResourceLocation id = modifier.getId();
      buffer.writeResourceLocation(id);
      try {
        ComposableModifier.LOADER.encode(buffer, modifier);
      } catch (RuntimeException e) {
        // improve error logging
        TConstruct.LOG.error("Failed to encode modifier with ID {}", id, e);
        throw e;
      }
    }
    // write redirects
    buffer.writeVarInt(redirects.size());
    for (Entry<ModifierId,ModifierId> entry : redirects.entrySet()) {
      buffer.writeResourceLocation(entry.getKey());
      buffer.writeResourceLocation(entry.getValue());
    }
    GenericTagUtil.encodeTags(buffer, Modifier::getId, this.tags);

    // enchantment mapping. write the enchantment key, resolved back to a holder on the receiving end
    buffer.writeVarInt(enchantmentMap.size());
    for (Entry<Holder<Enchantment>,Modifier> entry : enchantmentMap.entrySet()) {
      buffer.writeResourceLocation(entry.getKey().unwrapKey().orElseThrow().location());
      buffer.writeResourceLocation(entry.getValue().getId());
    }
    buffer.writeVarInt(enchantmentTagMappings.size());
    for (Entry<TagKey<Enchantment>, Modifier> entry : enchantmentTagMappings.entrySet()) {
      buffer.writeResourceLocation(entry.getKey().location());
      buffer.writeResourceLocation(entry.getValue().getId());
    }
  }

  @Override
  public Type<UpdateModifiersPacket> type() {
    return TYPE;
  }

  @Override
  public void handleThreadsafe(IPayloadContext context) {
    ModifierManager.INSTANCE.updateModifiersFromServer(allModifiers, tags, enchantmentMap, enchantmentTagMappings);
  }
}
