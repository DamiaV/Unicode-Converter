package net.darmo_creations.unicode_converter.ui;

import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import net.darmo_creations.unicode_converter.config.*;
import net.darmo_creations.unicode_converter.config.theme.*;
import org.jetbrains.annotations.*;

public class CopyableTextField extends HBox {
  public CopyableTextField(@NotNull TextField textField, final @NotNull Config config) {
    super(5);
    HBox.setHgrow(textField, Priority.ALWAYS);
    final Button copyButton = new Button();
    copyButton.setGraphic(config.theme().getIcon(Icon.COPY_TO_CLIPBOARD, Icon.Size.SMALL));
    copyButton.setTooltip(new Tooltip(config.language().translate("copyable_text_fiel.copy_button.tooltip")));
    copyButton.setOnAction(event -> {
      final var clipboardContent = new ClipboardContent();
      clipboardContent.putString(textField.getText());
      Clipboard.getSystemClipboard().setContent(clipboardContent);
    });
    this.getChildren().addAll(textField, copyButton);
  }
}
