package slimeknights.mantle.util;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Condition context to use when data has already been loaded, used in books for processing their conditions for instance. */
public enum DataLoadedConditionContext/* implements ICondition.IContext*/ {
  INSTANCE;

//  @Override
  public <T> Collection<Holder<T>> getTag(TagKey<T> key) {
    Registry<T> registry = RegistryHelper.getRegistry(key.registry());
    if (registry != null) {
      Optional<HolderSet.Named<T>> tag = registry.getTag(key);
      if (tag.isPresent()) {
        return contents(tag.get());
      }
    }
    return Set.of();
  }

//  @Override
  public <T> Map<ResourceLocation,Collection<Holder<T>>> getAllTags(ResourceKey<? extends Registry<T>> key) {
    Registry<T> registry = RegistryHelper.getRegistry(key);
    if (registry != null) {
      return registry.getTags().collect(Collectors.toMap(entry -> entry.getFirst().location(), entry -> contents(entry.getSecond())));
    }
    return Map.of();
  }

  /** Forge opens up HolderSet.Named#contents; vanilla only exposes the set through iteration */
  private static <T> Collection<Holder<T>> contents(HolderSet.Named<T> set) {
    List<Holder<T>> holders = new ArrayList<>();
    set.forEach(holders::add);
    return holders;
  }
}
