package slimeknights.mantle;

import io.github.fabricators_of_create.porting_lib.entity.events.LivingDeathEvent;
import io.github.fabricators_of_create.porting_lib.entity.events.LivingEntityEvents;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import slimeknights.mantle.datagen.MantleTags;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Handles events for any Mantle driven logic.
 * <p>
 * Upstream registers three Forge events ({@code LivingDeathEvent}, {@code LivingDropsEvent} and
 * {@code PlayerEvent.Clone}) through its event bus. Under Fabric the equivalents are Porting Lib's
 * {@link LivingDeathEvent} and {@link LivingEntityEvents#DROPS}, plus Fabric's {@link ServerPlayerEvents#COPY_FROM}
 * for the player clone. Both Porting Lib events fire at the same point in the death sequence as their Forge
 * counterparts, which matters here: the slot markers have to be written before the drops are computed.
 * <p>
 * Registered from {@link Mantle#onInitialize()} rather than by class-level annotation.
 */
public class MantleEvents {
  /* Soulbound */
  /**
   * NBT key for items to preserve their slot in soulbound. Applied to items tagged {@link MantleTags.Items#SOULBOUND}.
   * May be used by dependencies mods in a death event to make items soulbound for other reasons.
   */
  public static final String SOULBOUND_SLOT = "mantle_soulbound";

  /** Registers the event handlers, called during mod construction */
  public static void init() {
    LivingDeathEvent.DEATH.register(event -> onLivingDeath(event.getEntity()));
    LivingEntityEvents.DROPS.register((entity, source, drops, looting, recentlyHit) -> {
      onPlayerDropItems(entity, drops);
      return false;
    });
    ServerPlayerEvents.COPY_FROM.register((original, clone, alive) -> onPlayerClone(original, clone));
  }

  /** Called when the player dies to store the slot to return items into */
  private static void onLivingDeath(LivingEntity entity) {
    // this is the latest we can add slot markers to the items so we can return them to slots
    if (!entity.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY) && entity instanceof Player player && !(player instanceof FakePlayer)) {
      Inventory inventory = player.getInventory();

      // just iterate the whole inventory, no slot specific behavior
      int totalSize = inventory.getContainerSize();
      for (int i = 0; i < totalSize; i++) {
        ItemStack stack = inventory.getItem(i);
        if (!stack.isEmpty() && stack.is(MantleTags.Items.SOULBOUND)) {
          stack.getOrCreateTag().putInt(SOULBOUND_SLOT, i);
        }
      }
    }
  }

  /** Called when the player dies to store the soulbound items in the original inventory */
  private static void onPlayerDropItems(LivingEntity entity, Collection<ItemEntity> drops) {
    // only care about real players with keep inventory off
    if (!entity.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY) && entity instanceof Player player && !(entity instanceof FakePlayer)) {
      Iterator<ItemEntity> iter = drops.iterator();
      Inventory inventory = player.getInventory();
      List<ItemEntity> takenSlot = new ArrayList<>();
      while (iter.hasNext()) {
        ItemEntity itemEntity = iter.next();
        ItemStack stack = itemEntity.getItem();
        // find items with our soulbound tag set and move them back into the inventory, will move them over later
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(SOULBOUND_SLOT, Tag.TAG_ANY_NUMERIC)) {
          int slot = tag.getInt(SOULBOUND_SLOT);
          // return the tool to its requested slot if possible, remove from the drops
          if (inventory.getItem(slot).isEmpty()) {
            inventory.setItem(slot, stack);
          } else {
            // hold off on handling items that did not get the requested slot for now
            // want to make sure they don't get in the way of items that have not yet been seen
            takenSlot.add(itemEntity);
          }
          iter.remove();
          // don't clear the tag yet, we need it one last time for player clone
        }
      }
      // handle items that did not get their requested slot last, to ensure they don't take someone else's slot while being added to a default
      for (ItemEntity itemEntity : takenSlot) {
        ItemStack stack = itemEntity.getItem();
        if (!inventory.add(stack)) {
          // last resort, somehow we just cannot put the stack anywhere, so drop it on the ground
          // this should never happen, but better to be safe
          // ditch the soulbound slot tag, to prevent item stacking issues
          CompoundTag tag = stack.getTag();
          if (tag != null) {
            tag.remove(SOULBOUND_SLOT);
            if (tag.isEmpty()) {
              stack.setTag(null);
            }
          }
          drops.add(itemEntity);
        }
      }
    }
  }

  /** Called when the new player is created to fetch the soulbound item from the old */
  private static void onPlayerClone(Player original, Player clone) {
    if (original.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY) || original.isSpectator()) {
      return;
    }
    // find items with the soulbound tag set and move them over
    Inventory originalInv = original.getInventory();
    Inventory cloneInv = clone.getInventory();
    int size = Math.min(originalInv.getContainerSize(), cloneInv.getContainerSize()); // not needed probably, but might as well be safe
    List<ItemStack> takenSlot = new ArrayList<>();
    for (int i = 0; i < size; i++) {
      ItemStack stack = originalInv.getItem(i);
      if (!stack.isEmpty()) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(SOULBOUND_SLOT, Tag.TAG_ANY_NUMERIC)) {
          if (cloneInv.getItem(i).isEmpty()) {
            cloneInv.setItem(i, stack);
          } else {
            takenSlot.add(stack);
          }
          // remove the slot tag, clear the tag if needed
          tag.remove(SOULBOUND_SLOT);
          if (tag.isEmpty()) {
            stack.setTag(null);
          }
        }
      }
    }

    // handle items that did not get their requested slot last, to ensure they don't take someone else's slot while being added to a default
    for (ItemStack stack : takenSlot) {
      if (!cloneInv.add(stack)) {
        // last resort, somehow we just cannot put the stack anywhere, so drop it on the ground
        // this should never happen, but better to be safe
        clone.drop(stack, false);
      }
    }
  }
}
