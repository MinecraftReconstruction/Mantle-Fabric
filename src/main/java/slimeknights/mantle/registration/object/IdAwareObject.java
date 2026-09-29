package slimeknights.mantle.registration.object;

import net.minecraft.resources.ResourceLocation;

/** Interface for an object that holds its own name, used to simplify some utilities */
public interface IdAwareObject {
  /** Gets the ID for this object */
  ResourceLocation getId();

  /**
   * Gets the ID for this object. Legacy name kept so code written against the older Mantle (and the
   * Forge API in general) still compiles; new code should use {@link #getId()}.
   */
  default ResourceLocation getRegistryName() {
    return getId();
  }
}
