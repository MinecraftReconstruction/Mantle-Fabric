package slimeknights.mantle.recipe.ingredient;

import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public abstract class AbstractIngredient extends Ingredient implements CustomIngredient {

  private final Ingredient.Value[] values;
  @Nullable
  private List<ItemStack> itemStacks;

  /**
   * Empty constructor, for the sake of dynamic ingredients
   */
  protected AbstractIngredient() {
    this(Stream.of());
  }

  /**
   * Value constructor, for ingredients that have some vanilla representation
   */
  protected AbstractIngredient(Stream<? extends Ingredient.Value> values) {
    super(values);
    this.values = values.toArray(Ingredient.Value[]::new);
  }

  /**
   * Clears the cached matching stacks. Subclasses with their own caches override this and call
   * super; the cache is also cleared automatically when the ingredient becomes invalid.
   */
  protected void invalidate() {
    this.itemStacks = null;
  }

  @Override
  public List<ItemStack> getMatchingStacks() {
    if (this.itemStacks == null) {
      this.itemStacks = Arrays.stream(this.values).flatMap((value) -> value.getItems().stream()).distinct().toList();
    }

    return this.itemStacks;
  }

  @Override
  public boolean requiresTesting() {
    return !isSimple();
  }

  /**
   * Since this ingredient is a vanilla ingredient as well, the vanilla form is just itself.
   * (Fabric's default would wrap it in another layer.)
   */
  @Override
  public Ingredient toVanilla() {
    return this;
  }

  public abstract boolean isSimple();
}
