package slimeknights.mantle.item;

import net.minecraft.world.item.HangingSignItem;
import net.fabricmc.fabric.api.registry.FuelRegistry;
import net.minecraft.world.level.block.Block;

public class BurnableHangingSignItem extends HangingSignItem {
  private final int burnTime;
  public BurnableHangingSignItem(Properties propertiesIn, Block hangingBlock, Block wallBlock, int burnTime) {
    super(hangingBlock, wallBlock, propertiesIn);
    this.burnTime = burnTime;
    // NOTE(porting): Forge's Item#getBurnTime(ItemStack, RecipeType) hook has no Fabric counterpart. The closest
    //  equivalent is Fabric's static fuel registry, which stores one value per item and ignores the recipe type.
    //  See docs/BEHAVIOUR-DIFFERENCES.md.
    FuelRegistry.INSTANCE.add(this, burnTime);
  }
}
