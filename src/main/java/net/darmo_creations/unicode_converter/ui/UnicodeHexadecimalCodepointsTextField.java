package net.darmo_creations.unicode_converter.ui;

import javafx.scene.control.*;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.regex.*;

public class UnicodeHexadecimalCodepointsTextField extends TextField implements CodepointField {
  private static final Pattern HEX_CODEPOINT = Pattern.compile("^(?:U\\+)?([\\da-fA-F]+)$");

  @Override
  public List<Integer> getCodepoints() throws CodepointException {
    final List<Integer> codepoints = new ArrayList<>();
    for (final String part : this.getText().split("\\s+")) {
      final Matcher matcher = HEX_CODEPOINT.matcher(part);
      if (matcher.find())
        codepoints.add(Integer.parseInt(matcher.group(1), 16));
      else throw new CodepointException(part);
    }
    return codepoints;
  }

  @Override
  public void setCodepoints(final @NotNull List<Integer> codepoints) {
    final var charsJoiner = new StringJoiner(" ");
    for (final int codepoint : codepoints)
      charsJoiner.add("U+%04X".formatted(codepoint));
    this.setText(charsJoiner.toString());
  }
}
