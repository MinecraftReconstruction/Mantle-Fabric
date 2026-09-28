package slimeknights.mantle.data.listener;

import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import slimeknights.mantle.Mantle;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.Locale;

/** Same as {@link ISafeManagerReloadListener}, but reloads earlier. Needed to work with some parts of models. */
public interface IEarlySafeManagerReloadListener extends IdentifiableResourceReloadListener {
  /**
   * Fabric requires every reload listener to expose a unique id. API users may create several instances of
   * the same listener, so derive a per-instance id instead of forcing a constructor change.
   */
  @Override
  default ResourceLocation getFabricId() {
    return Mantle.getResource("listener/" + getClass().getSimpleName().toLowerCase(Locale.ROOT) + "/" + Integer.toHexString(System.identityHashCode(this)));
  }

  @Override
  default CompletableFuture<Void> reload(PreparationBarrier stage, ResourceManager resourceManager, ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler, Executor backgroundExecutor, Executor gameExecutor) {
    return CompletableFuture.runAsync(() -> {
//      if (ModLoader.isLoadingStateValid()) {
        onReloadSafe(resourceManager);
//      }
    }, backgroundExecutor).thenCompose(stage::wait);
  }

  /**
   * Safely handle a resource manager reload. Only runs if the mod loading state is valid
   * @param resourceManager  Resource manager
   */
  void onReloadSafe(ResourceManager resourceManager);
}
