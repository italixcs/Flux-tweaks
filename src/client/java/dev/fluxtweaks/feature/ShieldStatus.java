package dev.fluxtweaks.feature;

import dev.fluxtweaks.FluxTweaks;
import dev.fluxtweaks.config.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;

public final class ShieldStatus {
  private record Transition(int from, int target, long start) {
    int color(long now) {
      return VisualMath.blend(from, target, (now - start) / 180_000_000f);
    }
  }

  private final Map<Integer, Long> disabledUntil = new HashMap<>(), blockingSeen = new HashMap<>();
  private final Map<Long, Transition> transitions = new HashMap<>();

  public void clear() {
    disabledUntil.clear();
    blockingSeen.clear();
    transitions.clear();
  }

  public void tick() {
    Minecraft mc = Minecraft.getInstance();
    if (mc.level == null || !FluxTweaks.active(Feature.SHIELD_STATUS)) {
      clear();
      return;
    }
    long now = mc.level.getGameTime();
    Set<Integer> present = new HashSet<>();
    for (Player player : mc.level.players()) {
      present.add(player.getId());
      if (player.isBlocking()) {
        blockingSeen.put(player.getId(), now);
        disabledUntil.remove(player.getId());
      }
    }
    disabledUntil.entrySet().removeIf(e -> e.getValue() <= now || !present.contains(e.getKey()));
    blockingSeen.entrySet().removeIf(e -> now - e.getValue() > 12 || !present.contains(e.getKey()));
    transitions.keySet().removeIf(key -> !present.contains((int) (key >> 1)));
  }

  public void observe(SoundEvent sound, Vec3 position) {
    Minecraft mc = Minecraft.getInstance();
    if (mc.level == null
        || !FluxTweaks.active(Feature.SHIELD_STATUS)
        || FluxTweaks.config.bool(Feature.SHIELD_STATUS, "self")
        || !sound.location().equals(SoundEvents.SHIELD_BREAK.value().location())) return;
    List<Player> possible =
        mc.level.players().stream()
            .filter(
                p ->
                    p != mc.player
                        && p.isAlive()
                        && p.distanceToSqr(position) < 4
                        && blockingSeen.containsKey(p.getId())
                        && (p.getMainHandItem().is(Items.SHIELD)
                            || p.getOffhandItem().is(Items.SHIELD)))
            .map(p -> (Player) p)
            .sorted(Comparator.comparingDouble(p -> p.distanceToSqr(position)))
            .toList();
    if (possible.isEmpty()
        || (possible.size() > 1
            && Math.abs(
                    possible.get(0).distanceToSqr(position)
                        - possible.get(1).distanceToSqr(position))
                < .25)) return;
    // shortcut: remote stun duration uses vanilla's five-second estimate; upgrade when server
    // timing is available.
    disabledUntil.put(possible.getFirst().getId(), mc.level.getGameTime() + 100);
  }

  public int tint(LivingEntity owner, ItemStack stack) {
    return tint(owner, stack, stack == owner.getOffhandItem());
  }

  public int tint(LivingEntity owner, ItemStack stack, boolean offhand) {
    Minecraft mc = Minecraft.getInstance();
    if (!FluxTweaks.active(Feature.SHIELD_STATUS)
        || !stack.is(Items.SHIELD)
        || !(owner instanceof Player player)
        || (FluxTweaks.config.bool(Feature.SHIELD_STATUS, "self") && owner != mc.player)) return -1;
    boolean disabled =
        player == mc.player
            ? player.getCooldowns().isOnCooldown(stack)
            : disabledUntil.containsKey(player.getId());
    double durability =
        stack.isDamageableItem()
            ? 100.0 * (stack.getMaxDamage() - stack.getDamageValue()) / stack.getMaxDamage()
            : 100;
    String state =
        VisualMath.shield(
            disabled,
            player.isBlocking()
                && (player.getUsedItemHand() == net.minecraft.world.InteractionHand.OFF_HAND)
                    == offhand,
            durability,
            FluxTweaks.config.number(Feature.SHIELD_STATUS, "threshold"));
    int color = FluxTweaks.config.color(Feature.SHIELD_STATUS, state);
    long key = ((long) player.getId() << 1) | (offhand ? 1 : 0), now = System.nanoTime();
    Transition old = transitions.get(key);
    if (!FluxTweaks.config.bool(Feature.SHIELD_STATUS, "transition")) {
      transitions.put(key, new Transition(color, color, now));
      return color;
    }
    if (old == null) {
      old = new Transition(color, color, now);
      transitions.put(key, old);
    } else if (old.target != color) {
      old = new Transition(old.color(now), color, now);
      transitions.put(key, old);
    }
    return old.color(now);
  }
}
