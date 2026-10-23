package dev.fluxtweaks.config;

public enum Feature {
  HAND_CHAMS("hand_chams", "Hand Chams"),
  VIEW_MODELS("view_models", "View Models"),
  SPOTIFY("spotify", "Spotify HUD"),
  HIT_PARTICLES("hit_particles", "Hit Particles"),
  JUMP_CIRCLE("jump_circle", "Jump Circle"),
  AMBIENCE("ambience", "Ambience"),
  SHIELD_STATUS("shield_status", "Shield Status"),
  FREELOOK("freelook", "Freelook"),
  FREECAM("freecam", "Freecam"),
  FULLBRIGHT("fullbright", "Fullbright"),
  LOW_FIRE("low_fire", "Low Fire"),
  ZOOM("zoom", "Zoom"),
  BLOCK_OVERLAY("block_overlay", "Block Overlay"),
  HIT_COLOR("hit_color", "Hit Color");
  public final String id, title;

  Feature(String id, String title) {
    this.id = id;
    this.title = title;
  }

  public boolean hasAction() {
    return camera() || this == ZOOM;
  }

  public boolean camera() {
    return this == FREELOOK || this == FREECAM;
  }
}
