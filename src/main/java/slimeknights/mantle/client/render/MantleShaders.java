package slimeknights.mantle.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import lombok.Getter;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import slimeknights.mantle.config.Config;

import javax.annotation.Nullable;
import slimeknights.mantle.Mantle;
import java.io.IOException;

public class MantleShaders {
  @Nullable
  @Getter
  private static ShaderInstance blockFullBrightShader;
  @Nullable
  @Getter
  private static ShaderInstance fluidShader;

  /** Gets the shader to use for {@link MantleRenderTypes#FLUID_SHADER}, checking the config option to select which shader to use. */
  @Nullable
  public static ShaderInstance getConfiguredFluidShader() {
    if (Config.ENABLE_FLUID_FOG_FIX.get()) {
      return fluidShader;
    }
    return Config.FLUID_USE_TEXT_SHADER.get() ? GameRenderer.getRendertypeTextShader() : GameRenderer.getPositionColorTexLightmapShader();
  }

  public static void registerShaders(CoreShaderRegistrationCallback.RegistrationContext registry) throws IOException {
    registry.register(
      Mantle.getResource("block_fullbright"), DefaultVertexFormat.BLOCK,
      shader -> blockFullBrightShader = shader
    );
    registry.register(
      Mantle.getResource("fluid"), DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
      shader -> fluidShader = shader
    );
  }

  /** True if both of Mantle's core shaders were registered; used by the dev self test */
  public static boolean isRegistered() {
    return blockFullBrightShader != null && fluidShader != null;
  }
}
