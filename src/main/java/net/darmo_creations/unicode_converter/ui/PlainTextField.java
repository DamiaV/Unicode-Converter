package net.darmo_creations.unicode_converter.ui;

import javafx.scene.control.*;
import org.jetbrains.annotations.*;

import java.util.*;

public class PlainTextField extends TextField implements CodepointField {
  @Override
  public List<Integer> getCodepoints() {
    return this.getText().codePoints().boxed().toList();
  }

  @Override
  public void setCodepoints(final @NotNull List<Integer> codepoints) throws CodepointException {
    final var charsJoiner = new StringBuilder();
    for (final int codepoint : codepoints) {
      try {
        charsJoiner.append(new String(Character.toChars(codepoint)));
      } catch (final IllegalArgumentException e) {
        throw new CodepointException(String.valueOf(codepoint), e);
      }
    }
    this.setText(charsJoiner.toString());
  }
}
