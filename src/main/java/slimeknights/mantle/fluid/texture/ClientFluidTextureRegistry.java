package slimeknights.mantle.fluid.texture;

import io.github.fabricators_of_create.porting_lib.fluids.FluidType;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Client only bridge registering Mantle fluids with Fabric's {@link FluidRenderHandlerRegistry}.
 * <p>
 * This class must only be touched from a client environment, see {@code FluidDeferredRegister}.
 */
public class ClientFluidTextureRegistry {
  /** Handlers by fluid, so the textures can be looked up during a model bake */
  private static final Map<Fluid, ClientFluidTextureHandler> HANDLERS = new HashMap<>();

  /** Registers a render handler for the given fluid, unless something already registered one */
  public static void register(Fluid fluid, FluidType type) {
    if (FluidRenderHandlerRegistry.INSTANCE.get(fluid) == null) {
      ClientFluidTextureHandler handler = new ClientFluidTextureHandler(type);
      FluidRenderHandlerRegistry.INSTANCE.register(fluid, handler);
      HANDLERS.put(fluid, handler);
    }
  }

  /**
   * Gets the still texture of the given fluid, or null if this fluid has no handler from this registry.
   * <p>
   * Needed while baking a model: {@code FluidVariantRendering} reads the global texture atlas, which during a bake
   * still belongs to the previous resource reload (and is empty on the first one), so a model that contains a fluid
   * has to resolve that fluid's texture through the sprite getter of its own bake.
   */
  @Nullable
  public static ResourceLocation getStillTexture(Fluid fluid) {
    ClientFluidTextureHandler handler = HANDLERS.get(fluid);
    return handler == null ? null : handler.stillTexture();
  }
}
