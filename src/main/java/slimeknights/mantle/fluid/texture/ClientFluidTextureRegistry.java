package slimeknights.mantle.fluid.texture;

import io.github.fabricators_of_create.porting_lib.fluids.FluidType;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.minecraft.world.level.material.Fluid;

/**
 * Client only bridge registering Mantle fluids with Fabric's {@link FluidRenderHandlerRegistry}.
 * <p>
 * This class must only be touched from a client environment, see {@code FluidDeferredRegister}.
 */
public class ClientFluidTextureRegistry {
  /** Registers a render handler for the given fluid, unless something already registered one */
  public static void register(Fluid fluid, FluidType type) {
    if (FluidRenderHandlerRegistry.INSTANCE.get(fluid) == null) {
      FluidRenderHandlerRegistry.INSTANCE.register(fluid, new ClientFluidTextureHandler(type));
    }
  }
}
