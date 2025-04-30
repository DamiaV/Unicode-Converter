package net.darmo_creations.unicode_converter.ui;

import javafx.scene.control.*;
import org.jetbrains.annotations.*;

import java.util.*;

public class UnicodeDecimalCodepointsTextField extends TextField implements CodepointField {
  @Override
  public List<Integer> getCodepoints() throws CodepointException {
    final List<Integer> codepoints = new ArrayList<>();
    for (final String part : this.getText().split("\\s+"))
      try {
        codepoints.add(Integer.parseInt(part));
      } catch (final NumberFormatException e) {
        throw new CodepointException(part, e);
      }
    return codepoints;
  }

  @Override
  public void setCodepoints(final @NotNull List<Integer> codepoints) {
    final var charsJoiner = new StringJoiner(" ");
    for (final int codepoint : codepoints)
      charsJoiner.add(String.valueOf(codepoint));
    this.setText(charsJoiner.toString());
  }
}
