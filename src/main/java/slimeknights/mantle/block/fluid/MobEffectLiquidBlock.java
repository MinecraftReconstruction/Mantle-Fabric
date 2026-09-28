package slimeknights.mantle.block.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.MapColor;
import slimeknights.mantle.registration.deferred.FluidDeferredRegister;

import java.util.ArrayList;
import java.util.function.Function;
import java.util.function.Supplier;

/** Liquid block setting the entity on fire */
public class MobEffectLiquidBlock extends LiquidBlock {
  private final Supplier<MobEffectInstance> effect;
  /** The fluid this block represents; vanilla's LiquidBlock keeps its copy private */
  private final FlowingFluid fluid;
  public MobEffectLiquidBlock(Supplier<? extends FlowingFluid> supplier, Properties properties, Supplier<MobEffectInstance> effect) {
    // vanilla's LiquidBlock takes the fluid instance directly; the supplier only exists to defer creation
    super(supplier.get(), properties);
    this.fluid = supplier.get();
    this.effect = effect;
  }

  /**
   * Approximation of Forge's {@code Entity#getFluidTypeHeight} check: the entity must actually overlap the
   * fluid surface instead of merely clipping the block.
   */
  private boolean isInFluid(Level level, BlockPos pos, Entity entity) {
    FluidState state = level.getFluidState(pos);
    return state.getType().isSame(this.fluid) && entity.getY() < pos.getY() + state.getOwnHeight();
  }

  @Override
  public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
    if (isInFluid(level, pos, entity) && entity instanceof LivingEntity living) {
      MobEffectInstance effect = this.effect.get();
      effect.setCurativeItems(new ArrayList<>());
      living.addEffect(effect);
    }
  }

  /** Creates a new block supplier */
  public static Function<Supplier<? extends FlowingFluid>, LiquidBlock> createEffect(MapColor color, int lightLevel, Supplier<MobEffectInstance> effect) {
    return fluid -> new MobEffectLiquidBlock(fluid, FluidDeferredRegister.createProperties(color, lightLevel), effect);
  }
}
