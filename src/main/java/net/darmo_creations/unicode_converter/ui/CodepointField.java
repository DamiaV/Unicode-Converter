package net.darmo_creations.unicode_converter.ui;

import org.jetbrains.annotations.*;

import java.util.*;

public interface CodepointField {
  List<Integer> getCodepoints() throws CodepointException;

  void setCodepoints(@NotNull final List<Integer> codepoints) throws CodepointException;

  void clear();
}
