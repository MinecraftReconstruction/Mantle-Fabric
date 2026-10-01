package slimeknights.mantle.fluid;

import io.github.fabricators_of_create.porting_lib.fluids.FluidType;
import net.minecraft.world.level.material.Fluid;

import java.util.HashMap;
import java.util.Map;

/**
 * Helpers for dealing with fluids that have no Porting Lib fluid type.
 * <p>
 * On Forge every fluid carries a {@code FluidType}, so any hook on it can be called unconditionally. Fabric does not
 * have that guarantee: a mod can register a plain {@code Fluid} (milk-lib does exactly that, and Tinkers enables it),
 * and {@code Fluid#getFluidType()} returns null for it. Code that asks the type for behaviour - the vaporize hooks in
 * {@code BucketModule} for example - has to decide what to do, and the sensible answer is to ask a default type: its
 * hooks return the same values the vanilla fallback path uses, so the behaviour matches Forge for every fluid that does
 * not customize them.
 */
public final class FluidTypes {
  private FluidTypes() {}

  /** Default types for fluids that have none, one per fluid since the type is identity based */
  private static final Map<Fluid,FluidType> FALLBACKS = new HashMap<>();

  /** Creates a fluid type with default properties */
  private static FluidType createDefault() {
    return new FluidType(FluidType.Properties.create());
  }

  /**
   * Gets the fluid type of the given fluid, falling back to a default type when the fluid has none.
   * @param fluid  Fluid to look up
   * @return  Fluid type, never null
   */
  public static FluidType getType(Fluid fluid) {
    FluidType type = fluid.getFluidType();
    if (type != null) {
      return type;
    }
    // cannot use computeIfAbsent as the mapping function may be called twice under contention
    synchronized (FALLBACKS) {
      FluidType fallback = FALLBACKS.get(fluid);
      if (fallback == null) {
        fallback = createDefault();
        FALLBACKS.put(fluid, fallback);
      }
      return fallback;
    }
  }
}
