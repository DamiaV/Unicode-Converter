package net.darmo_creations.unicode_converter.ui;

import javafx.scene.control.*;
import org.jetbrains.annotations.*;

import java.nio.*;
import java.nio.charset.*;
import java.util.*;

public class UtfCodesTextField extends TextField implements CodepointField {
  private final boolean asHex;
  private final Charset charset;

  public UtfCodesTextField(boolean asHex, @NotNull Charset charset) {
    this.asHex = asHex;
    this.charset = charset;
  }

  @Override
  public List<Integer> getCodepoints() throws CodepointException {
    final String[] parts = this.getText().split("\\s+");
    final byte[] bytes = new byte[parts.length];

    for (int i = 0; i < parts.length; i++) {
      final String part = parts[i];
      try {
        final int b = this.asHex ? Integer.parseInt(part, 16) : Integer.parseInt(part);
        if (b < 0 || b > 255) throw new CodepointException(part);
        bytes[i] = (byte) b;
      } catch (final NumberFormatException e) {
        throw new CodepointException(part, e);
      }
    }

    try {
      final CharBuffer decodedChars = this.charset.newDecoder().decode(ByteBuffer.wrap(bytes));
      return decodedChars.codePoints().boxed().toList();
    } catch (final CharacterCodingException e) {
      throw new CodepointException(e.getMessage(), e);
    }
  }

  @Override
  public void setCodepoints(final @NotNull List<Integer> codepoints) throws CodepointException {
    final var charsJoiner = new StringJoiner(" ");

    for (final int codepoint : codepoints) {
      final String chars;
      try {
        chars = new String(Character.toChars(codepoint));
      } catch (final IllegalArgumentException e) {
        throw new CodepointException(String.valueOf(codepoint), e);
      }
      for (final byte b : chars.getBytes(this.charset)) {
        final int i = b < 0 ? b + 256 : b;
        charsJoiner.add(this.asHex ? "%02X".formatted(i) : String.valueOf(i));
      }
    }

    this.setText(charsJoiner.toString());
  }
}
