package slimeknights.mantle.data.loadable.common;

import com.google.gson.JsonSyntaxException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.mapping.EnumMapLoadable;
import slimeknights.mantle.data.loadable.primitive.ResourceLocationLoadable;
import slimeknights.mantle.util.typed.TypedMap;

import java.util.Map;

/**
 * Special loadable for display contexts.
 * <p>
 * Forge registers {@link ItemDisplayContext} in its own registry so mods can add contexts; on Fabric it is
 * simply the vanilla enum, so this loadable resolves values by their serialised name instead.
 */
public enum DisplayContextLoadable implements ResourceLocationLoadable<ItemDisplayContext> {
  INSTANCE;

  @Override
  public ItemDisplayContext fromKey(ResourceLocation name, String key, TypedMap context) {
    for (ItemDisplayContext value : ItemDisplayContext.values()) {
      if (value.getSerializedName().equals(name.getPath())) {
        return value;
      }
    }
    throw new JsonSyntaxException("Unable to parse " + key + " as ItemDisplayContext does not contain the ID " + name);
  }

  @Override
  public ResourceLocation getKey(ItemDisplayContext object) {
    return new ResourceLocation(object.getSerializedName());
  }

  @Override
  public ItemDisplayContext decode(FriendlyByteBuf buffer, TypedMap context) {
    return buffer.readEnum(ItemDisplayContext.class);
  }

  @Override
  public void encode(FriendlyByteBuf buffer, ItemDisplayContext value) {
    buffer.writeEnum(value);
  }

  @Override
  public <V> Loadable<Map<ItemDisplayContext,V>> mapWithValues(Loadable<V> valueLoadable, int minSize) {
    return new EnumMapLoadable<>(ItemDisplayContext.class, this, valueLoadable, minSize);
  }
}
