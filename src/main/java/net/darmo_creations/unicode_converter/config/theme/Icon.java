package net.darmo_creations.unicode_converter.config.theme;

import org.jetbrains.annotations.*;

import java.util.*;

/**
 * Enumeration of all icons used throughout the app.
 */
public enum Icon {
  LANGUAGES("text_language"),
  THEMES("smartart_change_color_gallery"),
  QUIT("door_in"),
  ABOUT("information"),

  COPY_TO_CLIPBOARD("clipboard_sign"),
  ;

  private final String baseName;

  Icon(@NotNull String baseName) {
    this.baseName = Objects.requireNonNull(baseName);
  }

  /**
   * The base name of this icon.
   */
  public String baseName() {
    return this.baseName;
  }

  /**
   * Enumeration of possible icon sizes.
   */
  public enum Size {
    /**
     * 16x16 pixels size.
     */
    SMALL(16),
    /**
     * 32x32 pixels size.
     */
    BIG(32),
    ;

    private final int pixels;

    Size(int pixels) {
      this.pixels = pixels;
    }

    /**
     * The width/height in pixels.
     */
    public int pixels() {
      return this.pixels;
    }
  }
}
