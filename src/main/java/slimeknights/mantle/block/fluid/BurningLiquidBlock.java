package slimeknights.mantle.block.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.MapColor;
import slimeknights.mantle.registration.deferred.FluidDeferredRegister;

import java.util.function.Function;
import java.util.function.Supplier;

/** Liquid block setting the entity on fire */
public class BurningLiquidBlock extends LiquidBlock {
  /** Burn time in seconds. Lava uses 15 */
  private final int burnTime;
  /** Damage from being in the fluid, lava uses 4 */
  private final float damage;
  /** The fluid this block represents; vanilla's LiquidBlock keeps its copy private */
  private final FlowingFluid fluid;
  public BurningLiquidBlock(Supplier<? extends FlowingFluid> supplier, Properties properties, int burnTime, float damage) {
    // vanilla's LiquidBlock takes the fluid instance directly; the supplier only exists to defer creation
    super(supplier.get(), properties);
    this.fluid = supplier.get();
    this.burnTime = burnTime;
    this.damage = damage;
  }

  /**
   * Approximation of Forge's {@code Entity#getFluidTypeHeight} check: the entity must actually overlap the
   * fluid surface instead of merely clipping the block.
   */
  private boolean isInFluid(Level level, BlockPos pos, Entity entity) {
    FluidState state = level.getFluidState(pos);
    return state.getType().isSame(this.fluid) && entity.getY() < pos.getY() + state.getOwnHeight();
  }

  @SuppressWarnings("deprecation")  // useless annotation on block methods
  @Override
  public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
    if (!entity.fireImmune() && isInFluid(level, pos, entity)) {
      entity.setSecondsOnFire(burnTime);
      if (entity.hurt(entity.damageSources().lava(), damage)) {
        entity.playSound(SoundEvents.GENERIC_BURN, 0.4F, 2.0F + level.random.nextFloat() * 0.4F);
      }
    }
  }

  /** Creates a new block supplier */
  public static Function<Supplier<? extends FlowingFluid>, LiquidBlock> createBurning(MapColor color, int lightLevel, int burnTime, float damage) {
    return fluid -> new BurningLiquidBlock(fluid, FluidDeferredRegister.createProperties(color, lightLevel), burnTime, damage);
  }
}
