package dev.fluxtweaks.test.mixin;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(KeyboardHandler.class)
public interface KeyboardProbe {
  @Invoker("keyPress")
  void flux$key(long window, int action, KeyEvent event);
}
