package slimeknights.mantle.registration.deferred;

import io.github.fabricators_of_create.porting_lib.fluids.FluidType;
import io.github.fabricators_of_create.porting_lib.fluids.PortingLibFluids;
import io.github.fabricators_of_create.porting_lib.fluids.wrapper.FabricFluidTypeWrapper;
import io.github.fabricators_of_create.porting_lib.util.RegistryObject;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import slimeknights.mantle.block.fluid.BurningLiquidBlock;
import slimeknights.mantle.block.fluid.MobEffectLiquidBlock;
import slimeknights.mantle.fluid.InvertedFluid;
import slimeknights.mantle.fluid.InvertedFluidType;
import slimeknights.mantle.fluid.TextureFluidType;
import slimeknights.mantle.fluid.UnplaceableFluid;
import slimeknights.mantle.fluid.texture.ClientFluidTextureRegistry;
import slimeknights.mantle.registration.DelayedSupplier;
import slimeknights.mantle.registration.FluidBuilder;
import slimeknights.mantle.registration.RegistrationHelper;
import slimeknights.mantle.registration.object.FlowingFluidObject;
import slimeknights.mantle.registration.object.FluidObject;
import slimeknights.mantle.util.SimpleFlowingFluid;
import slimeknights.mantle.util.SimpleFlowingFluid.Properties;

import javax.annotation.Nullable;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.ArrayList;
import java.util.List;

/**
 * Deferred register solving the nightmare that is registering fluids with Forge and Fabric
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class FluidDeferredRegister extends DeferredRegisterWrapper<Fluid> {
  private final SynchronizedDeferredRegister<FluidType> fluidTypeRegister;
  private final SynchronizedDeferredRegister<Block> blockRegister;
  private final SynchronizedDeferredRegister<Item> itemRegister;
  /** Fluids waiting for their attribute handler to be registered, see {@link #register()} */
  private final List<FluidAttribute> pendingAttributes = new ArrayList<>();

  public FluidDeferredRegister(String modID) {
    super(Registries.FLUID, modID);
    this.fluidTypeRegister = SynchronizedDeferredRegister.create(PortingLibFluids.FLUID_TYPE_REGISTRY, modID);
    this.blockRegister = SynchronizedDeferredRegister.create(Registries.BLOCK, modID);
    this.itemRegister = SynchronizedDeferredRegister.create(Registries.ITEM, modID);
  }

  @Override
  public void register() {
    super.register();
    fluidTypeRegister.register();
    blockRegister.register();
    itemRegister.register();
    // Fabric looks fluid attributes up per fluid, so bridge every Porting Lib FluidType into a Fabric attribute
    // handler. Without this all temperatures/luminosity fall back to the Fabric defaults (300K and no light).
    for (FluidAttribute attribute : pendingAttributes) {
      FluidType type = attribute.type().get();
      if (type != null) {
        Fluid fluid = attribute.fluid().get();
        FluidVariantAttributes.register(fluid, new FabricFluidTypeWrapper(type));
        // Fabric renders fluids through FluidRenderHandlerRegistry rather than the FluidType, so bridge the texture
        // as well. Without a handler, Fabric falls back to the water sprite (making every fluid look like water) and
        // code reading FluidVariantRendering#getSprites crashes on the null sprite array.
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
          ClientFluidTextureRegistry.register(fluid, type);
        }
      }
    }
    pendingAttributes.clear();
  }

  /** Pair of a fluid and the type backing it, so the attributes can be registered after the registry flush */
  private record FluidAttribute(Supplier<? extends Fluid> fluid, Supplier<? extends FluidType> type) {}

  /**
   * Registers a fluid type to the registry
   * @param name  Name of the fluid to register
   * @param sup   Fluid supplier
   * @param <I>   Fluid type
   * @return  Fluid to supply
   */
  public <I extends FluidType> RegistryObject<I> registerType(String name, Supplier<? extends I> sup) {
    return fluidTypeRegister.register(name, sup);
  }

  /**
   * Registers a fluid to the registry
   * @param name  Name of the fluid to register
   * @param sup   Fluid supplier
   * @param <I>   Fluid type
   * @return  Fluid to supply
   */
  public <I extends Fluid> RegistryObject<I> registerFluid(String name, Supplier<? extends I> sup) {
    return register.register(name, sup);
  }

  /** Starts a builder for a fluid */
  public Builder register(String name) {
    return new Builder(name);
  }

  @Accessors(fluent = true)
  @Setter
  public class Builder extends FluidBuilder<Builder> {
    private final String name;
    private final DelayedSupplier<Fluid> stillDelayed = new DelayedSupplier<>();
    /** Name for the common tag, if unset will only get a local tag */
    @Nullable
    private String commonTag = null;

    private Builder(String name) {
      this.name = name;
    }

    /** Adds a common tag to the builder */
    public Builder commonTag() {
      return this.commonTag(name);
    }

    /* Fluid type */

    /** Registers the passed fluid type */
    public Builder type(Supplier<? extends FluidType> type) {
      if (this.type != null) {
        throw new IllegalStateException("Type already created for " + name);
      }
      this.type = fluidTypeRegister.register(name, type);
      return this;
    }

    /** Registers a fluid with the given properties, using the texture fluid type */
    public Builder type(FluidType.Properties properties) {
      return type(() -> new TextureFluidType(properties));
    }

    /** Registers a fluid with the given properties, using the inverted fluid type */
    public Builder invertedType(FluidType.Properties properties) {
      return type(() -> new InvertedFluidType(properties));
    }

    /** Registers a fluid with the given properties, using the texture fluid type */
    public Builder type() {
      return type(FluidType.Properties.create());
    }

    /** Registers a fluid with the given properties, using the inverted fluid type */
    public Builder invertedType() {
      return invertedType(FluidType.Properties.create());
    }


    /* Bucket */

    /** Creates the bucket using the given supplier */
    public Builder bucket(Function<Supplier<? extends Fluid>, Item> constructor) {
      if (this.bucket != null) {
        throw new IllegalStateException("Bucket already created for " + name);
      }
      return bucket(itemRegister.register(name + "_bucket", () -> constructor.apply(stillDelayed)));
    }

    /** Creates the default bucket */
    public Builder bucket() {
      return bucket(itemRegister.register(name + "_bucket", () -> new BucketItem(stillDelayed.get(), RegistrationHelper.BUCKET_PROPS)));
    }


    /* Block */

    /** Creates the block form using the given supplier */
    @SuppressWarnings({"unchecked", "rawtypes"})  // if you are calling this method, you must have a flowing fluid by the end, we throw later if not
    public Builder block(Function<Supplier<? extends FlowingFluid>, LiquidBlock> constructor) {
      if (this.block != null) {
        throw new IllegalStateException("Block already created for " + name);
      }
      return block(blockRegister.register(name + "_fluid", () -> constructor.apply((Supplier<FlowingFluid>)(Supplier)stillDelayed)));
    }

    /** Creates the default block from the given material and light level */
    public Builder block(MapColor color, int lightLevel) {
      // vanilla's LiquidBlock takes the fluid instance, not a supplier
      return block(sup -> new LiquidBlock(sup.get(), createProperties(color, lightLevel)));
    }

    /** Creates a block that lights entities on fire and damages them over time */
    public Builder burningBlock(MapColor color, int lightLevel, int burnTime, float damage) {
      return block(BurningLiquidBlock.createBurning(color, lightLevel, burnTime, damage));
    }

    /** Creates a block that applies an effect to the target entity */
    public Builder mobEffectBlock(MapColor color, int lightLevel, Supplier<MobEffectInstance> effect) {
      return block(MobEffectLiquidBlock.createEffect(color, lightLevel, effect));
    }


    /* Final fluid */

    /** Builds an unplacable fluid with the default constructor */
    public FluidObject<UnplaceableFluid> unplacable() {
      return unplacable(UnplaceableFluid::new);
    }

    /**
     * Builds an unplacable fluid with the passed constructor
     * @param constructor  Constructor taking a fluid type and bucket supplier. Note the bucket supplier may be null.
     * @param <F> Resulting fluid type
     * @return  Fluid object instance
     */
    public <F extends Fluid> FluidObject<F> unplacable(Function<FluidBuilder<?>,F> constructor) {
      if (block != null) {
        throw new IllegalStateException("Cannot build an unplacable fluid with a block form");
      }
      if (type == null) {
        this.type();
      }
      RegistryObject<F> fluid = registerFluid(name, () -> constructor.apply(this));
      stillDelayed.setSupplier(fluid);
      pendingAttributes.add(new FluidAttribute(fluid, type));
      return new FluidObject<>(resource(name), commonTag, type, fluid);
    }

    /** Builds a flowing fluid with the default constructors */
    public FlowingFluidObject<SimpleFlowingFluid> flowing() {
      return flowing(SimpleFlowingFluid.Source::new, SimpleFlowingFluid.Flowing::new);
    }

    /** Builds a flowing fluid with the default constructors */
    public FlowingFluidObject<InvertedFluid> invertedFlowing() {
      return flowing(InvertedFluid.Source::new, InvertedFluid.Flowing::new);
    }

    /**
     * Builds a flowing fluid with the given constructors
     * @param createStill     Still constructor taking forge fluid properties, will contain the type, bucket, block, and flowing forms
     * @param createFlowing   Flowing constructor taking forge fluid properties, will contain the type, bucket, block, and still forms
     * @param <F>  Type of fluids being created
     * @return  Flowing fluid object instance
     */
    public <F extends FlowingFluid> FlowingFluidObject<F> flowing(Function<Properties,? extends F> createStill, Function<Properties,? extends F> createFlowing) {
      if (type == null) {
        this.type();
      }

      // create props with the suppliers
      DelayedSupplier<FlowingFluid> flowingDelayed = new DelayedSupplier<>();
      Properties props = build(type, stillDelayed, flowingDelayed);

      // create fluids now that we have props
      Supplier<F> still = registerFluid(name, () -> createStill.apply(props));
      stillDelayed.setSupplier(still);
      Supplier<F> flowing = registerFluid("flowing_" + name, () -> createFlowing.apply(props));
      flowingDelayed.setSupplier(flowing);
      pendingAttributes.add(new FluidAttribute(still, type));
      pendingAttributes.add(new FluidAttribute(flowing, type));

      // return the final nice object
      return new FlowingFluidObject<>(resource(name), commonTag, type, still, flowing, this.block);
    }
  }

  /** Creates properties for a fluid */
  public static BlockBehaviour.Properties createProperties(MapColor color, int lightLevel) {
    return BlockBehaviour.Properties.of().mapColor(color).replaceable().noCollission().randomTicks().strength(100.0F).lightLevel(state -> lightLevel).pushReaction(PushReaction.DESTROY).noLootTable().liquid().sound(SoundType.EMPTY);
  }
}
