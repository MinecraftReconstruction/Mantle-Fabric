package slimeknights.mantle.fluid.texture;

import io.github.fabricators_of_create.porting_lib.fluids.FluidType;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

/**
 * Fabric reimplementation of Forge's {@code IClientFluidTypeExtensions} as a {@link FluidRenderHandler}.
 * <p>
 * Forge asks a {@link FluidType} for its client textures through {@code IClientFluidTypeExtensions}; Fabric instead
 * looks up a {@link FluidRenderHandler} in {@code FluidRenderHandlerRegistry} for every fluid. This class bridges the
 * two, reading the textures from Mantle's {@link FluidTextureManager} (the same data Forge's
 * {@code ClientTextureFluidType} used).
 * <p>
 * Note that unlike {@link slimeknights.mantle.registration.FluidAttributeClientHandler} this class resolves the
 * sprites on demand instead of caching them in {@code reloadTextures}: Fabric calls {@code reloadTextures} during the
 * vanilla fluid renderer reload, which runs before Mantle's resource reload listeners have parsed
 * {@code mantle/fluid_texture}, so a cached copy would be one reload behind.
 */
public class ClientFluidTextureHandler implements FluidRenderHandler {
  private final FluidType type;

  public ClientFluidTextureHandler(FluidType type) {
    this.type = type;
  }

  @Override
  public TextureAtlasSprite[] getFluidSprites(@Nullable BlockAndTintGetter view, @Nullable BlockPos pos, FluidState state) {
    FluidTexture data = FluidTextureManager.getData(this.type);
    TextureAtlasSprite still = sprite(data.still());
    TextureAtlasSprite flowing = sprite(data.flowing());
    ResourceLocation overlay = data.overlay();
    if (overlay == null) {
      return new TextureAtlasSprite[] { still, flowing };
    }
    return new TextureAtlasSprite[] { still, flowing, sprite(overlay) };
  }

  @Override
  public int getFluidColor(@Nullable BlockAndTintGetter view, @Nullable BlockPos pos, FluidState state) {
    return FluidTextureManager.getColor(this.type);
  }

  @Override
  public void reloadTextures(TextureAtlas textureAtlas) {
    // sprites are resolved on demand, see the class javadoc
  }

  /** Resolves a sprite from the block atlas */
  private static TextureAtlasSprite sprite(ResourceLocation location) {
    return Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(location);
  }
}
