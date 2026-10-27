package dev.fluxtweaks.feature;

import dev.fluxtweaks.FluxTweaks;
import dev.fluxtweaks.config.ButterflyMotion;
import dev.fluxtweaks.config.ButterflyPlayback;
import dev.fluxtweaks.config.Feature;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.*;

/** Local presentation only: the held stack and all combat remain vanilla. */
public final class ButterflyKnife {
  public record Snapshot(ButterflyMotion.Pose pose, String finish, float scale, boolean glint, boolean left) {}
  private static final Feature FEATURE = Feature.BUTTERFLY_KNIFE;
  private static final ButterflyMotion[] MOTION = {new ButterflyMotion(), new ButterflyMotion()};
  private static final ItemStack[] STACKS = {ItemStack.EMPTY, ItemStack.EMPTY};
  private static final ButterflyPlayback PLAYBACK = new ButterflyPlayback();
  private static String inputBinding = "";
  private static int loopHand = -1;
  private static double frameTime;
  private static boolean reduced;

  private static double now() {
    return System.nanoTime() / 1_000_000_000.0;
  }

  public static boolean sword(ItemStack stack) {
    return stack.is(Items.WOODEN_SWORD) || stack.is(Items.STONE_SWORD)
        || stack.is(Items.COPPER_SWORD) || stack.is(Items.IRON_SWORD)
        || stack.is(Items.GOLDEN_SWORD) || stack.is(Items.DIAMOND_SWORD)
        || stack.is(Items.NETHERITE_SWORD);
  }

  private static boolean available() {
    var mc = Minecraft.getInstance();
    return FluxTweaks.config.enabled(FEATURE) && mc.player != null && mc.level != null
        && mc.player.isAlive() && mc.options.getCameraType().isFirstPerson()
        && !FluxTweaks.active(Feature.FREECAM);
  }

  private static boolean editing() {
    var mc = Minecraft.getInstance();
    return mc.gui.screen() != null || mc.gui.overlay() != null || !mc.isWindowActive();
  }

  private static boolean busy() {
    var mc = Minecraft.getInstance();
    return mc.player.isUsingItem() || mc.player.swinging
        || mc.options.keyAttack.isDown() || mc.options.keyUse.isDown();
  }

  public static void tick() {
    if (!available()) {
      reset();
      return;
    }
    var mc = Minecraft.getInstance();
    boolean preference = FluxTweaks.config.theme.reducedMotion;
    if (preference != reduced) cancel();
    reduced = preference;
    for (var hand : InteractionHand.values()) {
      int index = hand == InteractionHand.MAIN_HAND ? 0 : 1;
      ItemStack stack = mc.player.getItemInHand(hand);
      if (STACKS[index] != stack) {
        if (index == loopHand) cancel();
        STACKS[index] = stack;
        MOTION[index].cancel();
        if (sword(stack) && !editing() && !busy()) MOTION[index].equip(now(), reduced, FluxTweaks.config.number(FEATURE, "speed"));
      }
    }
    if (editing() || busy()) cancel();
  }

  /** Called before the outer hand pass, once for all of Chams' repeated submissions. */
  public static void beginFrame() {
    frameTime = now();
    if (!available() || editing() || busy()) cancel();
  }

  /** Consume real key edges once per client tick, outside all rendering passes. */
  public static void input(boolean allowed, boolean down, boolean pressed, String binding) {
    String mode = FluxTweaks.config.text(FEATURE, "activation");
    String identity = binding + ":" + mode;
    if (!identity.equals(inputBinding)) {
      cancel();
      inputBinding = identity;
    }
    var mc = Minecraft.getInstance();
    boolean eligible = allowed && available() && !editing() && !busy()
        && (sword(mc.player.getMainHandItem()) || sword(mc.player.getOffhandItem()));
    boolean wasRepeating = PLAYBACK.repeating();
    boolean wanted = PLAYBACK.update(mode, eligible, down, pressed);
    if (!eligible || wasRepeating && !PLAYBACK.repeating()) {
      cancel();
      return;
    }
    if (wanted && !inspecting()) inspect();
  }

  public static void inspect() {
    if (!available() || editing() || busy()) return;
    var mc = Minecraft.getInstance();
    int index = sword(mc.player.getMainHandItem()) ? 0 : sword(mc.player.getOffhandItem()) ? 1 : -1;
    if (index < 0) return;
    String style = FluxTweaks.config.text(FEATURE, "inspect");
    if (style.equals("Random")) style = new String[] {"Flip", "Spin", "Flourish"}[ThreadLocalRandom.current().nextInt(3)];
    if (PLAYBACK.repeating()) loopHand = index;
    MOTION[index].inspect(style, now(), FluxTweaks.config.theme.reducedMotion, FluxTweaks.config.number(FEATURE, "speed"));
  }

  public static boolean repeating() {
    return PLAYBACK.repeating();
  }

  public static boolean inspecting() {
    return MOTION[0].inspecting(now()) || MOTION[1].inspecting(now());
  }

  public static void cancel() {
    PLAYBACK.cancel();
    loopHand = -1;
    for (var motion : MOTION) motion.cancel();
  }

  public static void reset() {
    cancel();
    STACKS[0] = STACKS[1] = ItemStack.EMPTY;
  }

  public static Snapshot snapshot(ItemStack stack, ItemDisplayContext display, ItemOwner owner) {
    var mc = Minecraft.getInstance();
    if (!available() || owner != mc.player || !sword(stack)) return null;
    boolean left = display == ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
    if (!left && display != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) return null;
    int index = left == (mc.player.getMainArm() == HumanoidArm.LEFT) ? 0 : 1;
    ItemStack held = index == 0 ? mc.player.getMainHandItem() : mc.player.getOffhandItem();
    if (!ItemStack.isSameItemSameComponents(stack, held)) return null;
    return new Snapshot(MOTION[index].sample(frameTime), FluxTweaks.config.text(FEATURE, "finish"),
        FluxTweaks.config.decimal(FEATURE, "scale"), FluxTweaks.config.bool(FEATURE, "glint"), left);
  }

  private ButterflyKnife() {}
}
