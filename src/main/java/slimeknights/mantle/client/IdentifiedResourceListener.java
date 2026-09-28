package slimeknights.mantle.client;

import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Wraps a plain {@link PreparableReloadListener} so it can be registered with Fabric, which requires every
 * listener to expose a unique id. Forge had no such requirement, so several Mantle listeners are plain
 * reload listeners.
 */
public record IdentifiedResourceListener(ResourceLocation id, PreparableReloadListener listener) implements IdentifiableResourceReloadListener {
  @Override
  public ResourceLocation getFabricId() {
    return id;
  }

  @Override
  public CompletableFuture<Void> reload(PreparationBarrier barrier, ResourceManager resourceManager, ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler, Executor backgroundExecutor, Executor gameExecutor) {
    return listener.reload(barrier, resourceManager, preparationsProfiler, reloadProfiler, backgroundExecutor, gameExecutor);
  }

  @Override
  public String getName() {
    return listener.getName();
  }
}
