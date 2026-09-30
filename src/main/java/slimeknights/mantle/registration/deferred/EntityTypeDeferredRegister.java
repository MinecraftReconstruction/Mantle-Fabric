package slimeknights.mantle.registration.deferred;

import io.github.fabricators_of_create.porting_lib.util.LazySpawnEggItem;
import io.github.fabricators_of_create.porting_lib.util.RegistryObject;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import slimeknights.mantle.registration.object.EntityObject;

import java.util.function.Supplier;

/**
 * Deferred register for an entity, building the type from a builder instance and adding an egg
 */
@SuppressWarnings("unused")
public class EntityTypeDeferredRegister extends DeferredRegisterWrapper<EntityType<?>> {

  private final SynchronizedDeferredRegister<Item> itemRegistry;
  public EntityTypeDeferredRegister(String modID) {
    super(Registries.ENTITY_TYPE, modID);
    itemRegistry = SynchronizedDeferredRegister.create(Registries.ITEM, modID);
  }

  @Override
  public void register() {
    super.register();
    itemRegistry.register();
  }

  /**
   * Registers a entity type for the given entity type builder
   * @param name  Entity name
   * @param sup   Entity builder instance
   * @param <T>   Entity class type
   * @return  Entity registry object
   */
  public <T extends Entity> RegistryObject<EntityType<T>> register(String name, Supplier<EntityType.Builder<T>> sup) {
    return register.register(name, () -> sup.get().build(resourceName(name)));
  }

  /**
   * Registers a entity type for the given Fabric entity type builder
   * <p>
   * Fabric's builder is the only way to express {@code forceTrackedVelocityUpdates}, the Fabric equivalent of Forge's
   * {@code setShouldReceiveVelocityUpdates} (vanilla's {@code EntityType.Builder} has no such setter and
   * {@code EntityType#trackDeltas} has a fixed default). Kept as an overload next to the vanilla one so either builder
   * can be used.
   * @param name  Entity name
   * @param sup   Entity builder instance
   * @param <T>   Entity class type
   * @return  Entity registry object
   */
  public <T extends Entity> RegistryObject<EntityType<T>> register(String name, FabricEntityTypeBuilder<T> builder) {
    return register.register(name, builder::build);
  }

  /**
   * Registers a entity type for the given entity type builder, and registers a spawn egg for it
   * @param name       Entity name
   * @param sup        Entity builder instance
   * @param primary    Primary egg color
   * @param secondary  Secondary egg color
   * @param <T>   Entity class type
   * @return  Entity registry object
   */
  public <T extends Mob> EntityObject<T> registerWithEgg(String name, Supplier<EntityType.Builder<T>> sup, int primary, int secondary) {
    RegistryObject<EntityType<T>> object = register(name, sup);
    return new EntityObject<>(object, itemRegistry.register(name + "_spawn_egg", () -> new LazySpawnEggItem(object, primary, secondary, new Item.Properties())));
  }

  /**
   * Registers a entity type for the given Fabric entity type builder, and registers a spawn egg for it
   * @param name       Entity name
   * @param sup        Entity builder instance
   * @param primary    Primary egg color
   * @param secondary  Secondary egg color
   * @param <T>   Entity class type
   * @return  Entity registry object
   * @see #register(String, Supplier)
   */
  public <T extends Mob> EntityObject<T> registerWithEgg(String name, FabricEntityTypeBuilder<T> builder, int primary, int secondary) {
    RegistryObject<EntityType<T>> object = register(name, builder);
    return new EntityObject<>(object, itemRegistry.register(name + "_spawn_egg", () -> new LazySpawnEggItem(object, primary, secondary, new Item.Properties())));
  }
}
