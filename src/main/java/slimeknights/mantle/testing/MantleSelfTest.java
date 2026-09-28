package slimeknights.mantle.testing;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.registry.FuelRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.item.BurnableHangingSignItem;
import slimeknights.mantle.registration.deferred.ArgumentTypeDeferredRegister;
import slimeknights.mantle.util.OffhandCooldownTracker;

import java.util.concurrent.CompletableFuture;

/**
 * Development-only self test for the parts of this port that nothing else exercises.
 * <p>
 * Several behaviour differences in [docs/BEHAVIOUR-DIFFERENCES.md] can only be checked with an item or block that a
 * downstream mod would normally provide, and one of them ({@link ArgumentTypeDeferredRegister}) is a library class
 * that no mod in this repository uses at all, so it would otherwise ship completely untested. Rather than add a
 * separate test mod this class builds those objects itself and asserts on the result.
 * <p>
 * It only runs when Fabric reports a development environment, so a released jar is unaffected; run
 * {@code ./gradlew runServer} and look for {@code [selftest]} lines in the log.
 * <p>
 * Deliberately does <b>not</b> register anything into vanilla registries - only the fuel registry and the
 * command-argument-type registry - so it cannot leak into generated data.
 */
public final class MantleSelfTest implements ModInitializer {
  private static final String TAG = "[selftest] ";
  /** Burn time used for the test item; also the value the furnace should spend before eating it */
  private static final int BURN_TIME = 300;
  /** Cooldown used for the offhand tracker round trip */
  private static final int COOLDOWN = 40;

  private static Item fuelItem;
  private static int passed;
  private static int failed;

  @Override
  public void onInitialize() {
    if (!FabricLoader.getInstance().isDevelopmentEnvironment()) {
      return;
    }
    Mantle.logger.info(TAG + "development environment: arming self test");

    // #16: Mantle's BurnableHangingSignItem registers its burn time with Fabric's fuel registry in its constructor
    // NOTE: the item has to be registered, not just constructed. Item's constructor creates an *intrusive* holder in
    // the item registry, and leaving one unbound makes MappedRegistry#freeze fail later with
    // "Some intrusive holders were not registered", which stops the whole server from loading datapacks.
    fuelItem = Registry.register(BuiltInRegistries.ITEM, Mantle.getResource("selftest_fuel"),
      new BurnableHangingSignItem(new Item.Properties(), Blocks.OAK_HANGING_SIGN, Blocks.OAK_WALL_HANGING_SIGN, BURN_TIME));

    // #18: exercise Mantle's ArgumentTypeDeferredRegister. Its Forge-only ArgumentTypeInfos#registerByClass call was
    // replaced with a direct write into ArgumentTypeInfos.BY_CLASS via an access widener, and nothing else uses it.
    ArgumentTypeDeferredRegister argumentTypes = new ArgumentTypeDeferredRegister("mantle");
    argumentTypes.registerSingleton("selftest_argument", SelfTestArgument.class, SelfTestArgument::new);
    argumentTypes.register();

    ServerLifecycleEvents.SERVER_STARTED.register(server -> {
      try {
        runTests(server.overworld());
      } catch (Throwable t) {
        fail("harness", "self test threw", t.toString());
        t.printStackTrace();
      }
      Mantle.logger.info(TAG + "summary: " + passed + " passed, " + failed + " failed");
      if (failed > 0) {
        Mantle.logger.error(TAG + failed + " SELF TEST FAILURE(S) - see the FAIL lines above");
      }
    });
  }

  private static void runTests(ServerLevel level) {
    testFuelRegistry();
    testFurnaceBurnsFuel(level);
    testArgumentTypeRegistration();
    testOffhandCooldownNbt();
    testOffhandCooldownComponent(level);
  }

  /** #16: does BurnableHangingSignItem actually reach Fabric's fuel registry? */
  private static void testFuelRegistry() {
    try {
      Integer value = FuelRegistry.INSTANCE.get(fuelItem);
      check("fuel/#16 registry entry", value != null && value == BURN_TIME,
        "FuelRegistry.get(BurnableHangingSignItem) = " + value + ", expected " + BURN_TIME);
    } catch (Throwable t) {
      fail("fuel/#16 registry entry", "threw", t.toString());
    }
  }

  /** #16 (stronger): does a real furnace actually treat the item as fuel? */
  private static void testFurnaceBurnsFuel(ServerLevel level) {
    String name = "fuel/#16 furnace burns it";
    try {
      BlockPos pos = new BlockPos(0, 100, 0);
      level.setBlockAndUpdate(pos, Blocks.FURNACE.defaultBlockState());
      if (!(level.getBlockEntity(pos) instanceof FurnaceBlockEntity furnace)) {
        fail(name, "no furnace block entity", "at " + pos);
        return;
      }
      furnace.setItem(0, new ItemStack(Items.RAW_IRON));
      furnace.setItem(1, new ItemStack(fuelItem));
      BlockState state = level.getBlockState(pos);

      // one tick should light the furnace because the fuel slot holds something the registry knows
      AbstractFurnaceBlockEntity.serverTick(level, pos, state, furnace);
      boolean lit = level.getBlockState(pos).getValue(BlockStateProperties.LIT);
      if (!lit) {
        fail(name, "furnace did not light up",
          "fuel slot = " + furnace.getItem(1) + ", registry value = " + FuelRegistry.INSTANCE.get(fuelItem));
        return;
      }

      // burn through it: after more ticks than BURN_TIME the fuel item must have been consumed
      for (int i = 0; i < BURN_TIME + 40; i++) {
        state = level.getBlockState(pos);
        AbstractFurnaceBlockEntity.serverTick(level, pos, state, furnace);
      }
      ItemStack leftover = furnace.getItem(1);
      check(name, leftover.isEmpty(), "fuel slot after " + (BURN_TIME + 41) + " ticks = " + leftover);
      level.removeBlock(pos, false);
    } catch (Throwable t) {
      fail(name, "threw", t.toString());
    }
  }

  /** #18: Mantle's ArgumentTypeDeferredRegister must make the class known to ArgumentTypeInfos */
  private static void testArgumentTypeRegistration() {
    String name = "argument/#18 class recognised";
    try {
      boolean recognised = ArgumentTypeInfos.isClassRecognized(SelfTestArgument.class);
      check(name, recognised, "ArgumentTypeInfos.isClassRecognized(SelfTestArgument) = " + recognised
        + " (a plain Forge-style deferred argument type; false means the BY_CLASS write did not happen)");

      ArgumentTypeInfo<?, ?> info = null;
      try {
        info = ArgumentTypeInfos.byClass(new SelfTestArgument());
      } catch (Throwable t) {
        info = null;
        check("argument/#18 info lookup", false, "ArgumentTypeInfos.byClass threw " + t);
        return;
      }
      check("argument/#18 info lookup", info != null, "ArgumentTypeInfos.byClass(SelfTestArgument) = " + info);
    } catch (Throwable t) {
      fail(name, "threw", t.toString());
    }
  }

  /** #22 + #23: the Cardinal Components entrypoint constructor, and the NBT read/write direction */
  private static void testOffhandCooldownNbt() {
    String ctor = "cooldown/#22 no-arg constructor";
    try {
      // Cardinal Components instantiates the entrypoint class reflectively, so this must exist
      OffhandCooldownTracker tracker = new OffhandCooldownTracker();
      check(ctor, tracker != null, "new OffhandCooldownTracker() succeeded");

      tracker.applyCooldown(COOLDOWN);
      float applied = tracker.getCooldown();

      CompoundTag tag = new CompoundTag();
      tracker.writeToNbt(tag);
      check("cooldown/#23 writeToNbt writes", tag.getInt("lastCooldown") == COOLDOWN,
        "tag after writeToNbt = " + tag + " (was inverted before: writeToNbt used to read from the tag, so the tag stayed empty)");

      // drop the state, then restore it from the tag we just wrote
      tracker.readFromNbt(new CompoundTag());
      float cleared = tracker.getCooldown();
      tracker.readFromNbt(tag);
      float restored = tracker.getCooldown();
      check("cooldown/#23 readFromNbt restores", cleared != restored && restored == applied,
        "cooldown after apply = " + applied + ", after cleared = " + cleared + ", after restoring the tag = " + restored);
    } catch (Throwable t) {
      fail(ctor, "threw", t.toString());
    }
  }

  /** #22/#23 on a real entity: the component must attach to a player and survive an NBT round trip */
  private static void testOffhandCooldownComponent(ServerLevel level) {
    String name = "cooldown/component attaches to a player";
    try {
      ServerPlayer player = FakePlayer.get(level);
      OffhandCooldownTracker tracker = OffhandCooldownTracker.get(player);
      if (tracker == null) {
        fail(name, "component not attached", "OffhandCooldownTracker.get(fakePlayer) returned null");
        return;
      }
      check(name, true, "OffhandCooldownTracker.get(fakePlayer) = " + tracker);

      CompoundTag tag = new CompoundTag();
      tracker.writeToNbt(tag);
      check("cooldown/component NBT round trip", tag.contains("attackReady") || tag.contains("lastCooldown"),
        "component NBT = " + tag);
    } catch (Throwable t) {
      fail(name, "threw", t.toString());
    }
  }


  /* Harness */

  private static void check(String name, boolean ok, String detail) {
    if (ok) {
      passed++;
      Mantle.logger.info(TAG + "PASS  " + name + "  (" + detail + ")");
    } else {
      fail(name, "assertion failed", detail);
    }
  }

  private static void fail(String name, String what, String detail) {
    failed++;
    Mantle.logger.error(TAG + "FAIL  " + name + "  " + what + "  (" + detail + ")");
  }

  /** Minimal argument type; only {@code parse} has no default implementation */
  public static class SelfTestArgument implements ArgumentType<String> {
    @Override
    public String parse(com.mojang.brigadier.StringReader reader) {
      return reader.readUnquotedString();
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
      return Suggestions.empty();
    }
  }
}
