package dev.fluxtweaks.mixin;

import dev.fluxtweaks.FluxTweaks;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class SoundMixin {
  @Inject(method = "handleSoundEvent", at = @At("TAIL"))
  private void flux$shieldSound(ClientboundSoundPacket packet, CallbackInfo ci) {
    if (FluxTweaks.shields != null)
      FluxTweaks.shields.observe(
          packet.getSound().value(), new Vec3(packet.getX(), packet.getY(), packet.getZ()));
  }
}
