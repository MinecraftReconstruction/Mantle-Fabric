package slimeknights.mantle.client.model.util;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Keeps track of the render layers a baked model was built from.
 * <p>
 * Forge models hand their per-layer {@code RenderType} to {@code CompositeModel.Builder#addQuads(renderType, quads)},
 * but Porting Lib 2.3.16 dropped that parameter (Fabric has no per-quad render type: Indigo picks one render type per
 * block or item, from {@code BlockRenderLayerMap}/{@code ItemBlockRenderTypes}). The layers are therefore recorded
 * here instead, so that the places that <em>do</em> control their own draw call - the book structure preview - can
 * still honour them, and so a port can register the matching block/item render layer.
 * <p>
 * Entries are held weakly: baked models live in the model manager and are replaced on every resource reload.
 */
public final class ModelLayers {
  private ModelLayers() {}

  /** One render layer of a model: the type it asked for and the quads that belong to it */
  public record Layer(RenderType renderType, List<BakedQuad> quads) {}

  private static final Map<BakedModel,List<Layer>> LAYERS = Collections.synchronizedMap(new WeakHashMap<>());

  /** Records the layers of a baked model; called by the model loaders that build layered models */
  public static void put(BakedModel model, List<Layer> layers) {
    if (!layers.isEmpty()) {
      LAYERS.put(model, List.copyOf(layers));
    }
  }

  /** Gets the layers of the given model, or null if it was not built from layers */
  @Nullable
  public static List<Layer> get(BakedModel model) {
    return LAYERS.get(model);
  }
}
