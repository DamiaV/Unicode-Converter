package net.darmo_creations.unicode_converter;

import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import net.darmo_creations.unicode_converter.config.*;
import net.darmo_creations.unicode_converter.config.theme.*;
import net.darmo_creations.unicode_converter.ui.dialogs.*;
import net.darmo_creations.unicode_converter.utils.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.util.*;
import java.util.regex.*;

public class AppController {
  private final Stage stage;
  private final Config config;

  private LanguageSetting currentLanguageSetting;
  private ThemeSetting currentThemeSetting;

  private boolean internalTextUpdate;

  private final AboutDialog aboutDialog;
  private final TextField charsTextField = new TextField();
  private final TextField decimalCodepointsTextField = new TextField();
  private final TextField hexCodepointsTextField = new TextField();

  public AppController(@NotNull Stage stage, @NotNull Config config) {
    this.stage = Objects.requireNonNull(stage);
    this.config = Objects.requireNonNull(config);
    final Theme theme = config.theme();
    final Image icon = theme.getAppIcon();
    if (icon != null) stage.getIcons().add(icon);
    stage.setResizable(false);
    stage.setTitle(App.NAME);

    this.aboutDialog = new AboutDialog(config);

    final Scene scene = new Scene(new VBox(this.createMenuBar(), this.createContent()));
    stage.setScene(scene);
    theme.getStyleSheets().forEach(path -> scene.getStylesheets().add(path.toExternalForm()));
  }

  private Node createMenuBar() {
    final Language language = this.config.language();
    final Theme theme = this.config.theme();

    //

    final Menu appMenu = new Menu(language.translate("menu.app"));

    final Menu langsMenu = new Menu(
        language.translate("menu.app.language"),
        theme.getIcon(Icon.LANGUAGES, Icon.Size.SMALL)
    );

    final List<Pair<LanguageSetting, String>> languagesSettings = new ArrayList<>();
    languagesSettings.add(new Pair<>(LanguageSetting.SYSTEM, language.translate("language.system")));
    Arrays.stream(LanguageSetting.LANGUAGES)
        .map(ls -> new Pair<>(ls, Config.getLanguage(ls).name()))
        .sorted(Comparator.comparing(Pair::right))
        .forEach(languagesSettings::add);
    final ToggleGroup tg1 = new ToggleGroup();
    for (final var e : languagesSettings) {
      final LanguageSetting languageSetting = e.left();
      final String langName = e.right();
      final RadioMenuItem languageMenuItem = new RadioMenuItem();
      tg1.getToggles().add(languageMenuItem);
      languageMenuItem.setText(langName);
      final boolean isCurrent = this.config.languageSetting() == languageSetting;
      if (isCurrent) this.currentLanguageSetting = languageSetting;
      languageMenuItem.setSelected(isCurrent);
      languageMenuItem.setOnAction(event -> this.onLanguageSettingSelection(languageSetting));
      langsMenu.getItems().add(languageMenuItem);
    }

    appMenu.getItems().add(langsMenu);

    final Menu themesMenu = new Menu(
        language.translate("menu.app.theme"),
        theme.getIcon(Icon.THEMES, Icon.Size.SMALL)
    );

    final ToggleGroup tg2 = new ToggleGroup();
    for (final var themeSetting : ThemeSetting.values()) {
      final RadioMenuItem themeMenuItem = new RadioMenuItem();
      tg2.getToggles().add(themeMenuItem);
      themeMenuItem.setText(language.translate("theme." + themeSetting.id()));
      final boolean isCurrent = this.config.themeSetting() == themeSetting;
      if (isCurrent) this.currentThemeSetting = themeSetting;
      themeMenuItem.setSelected(isCurrent);
      themeMenuItem.setOnAction(event -> this.onThemeSettingSelection(themeSetting));
      themesMenu.getItems().add(themeMenuItem);
    }

    appMenu.getItems().add(themesMenu);

    final MenuItem quitMenuItem = new MenuItem();
    quitMenuItem.setText(language.translate("menu.app.quit"));
    quitMenuItem.setGraphic(theme.getIcon(Icon.QUIT, Icon.Size.SMALL));
    quitMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.ESCAPE));
    quitMenuItem.setOnAction(event -> this.stage.close());
    appMenu.getItems().add(quitMenuItem);

    //

    final Menu helpMenu = new Menu(language.translate("menu.help"));

    final MenuItem aboutMenuItem = new MenuItem();
    aboutMenuItem.setText(language.translate("menu.help.about"));
    aboutMenuItem.setGraphic(theme.getIcon(Icon.ABOUT, Icon.Size.SMALL));
    aboutMenuItem.setOnAction(event -> this.onAboutAction());
    helpMenu.getItems().add(aboutMenuItem);

    return new MenuBar(appMenu, helpMenu);
  }

  private Node createContent() {
    final Language language = this.config.language();

    final GridPane gridPane = new GridPane();
    gridPane.setPadding(new Insets(5));
    gridPane.setHgap(5);
    gridPane.setVgap(5);

    this.charsTextField.textProperty().addListener((observable, oldValue, newValue) -> {
      if (this.internalTextUpdate) return;
      this.internalTextUpdate = true;
      this.fromChars(newValue);
      this.internalTextUpdate = false;
    });
    this.decimalCodepointsTextField.textProperty().addListener((observable, oldValue, newValue) -> {
      if (this.internalTextUpdate) return;
      this.internalTextUpdate = true;
      this.fromDecimalCodepoints(newValue);
      this.internalTextUpdate = false;
    });
    this.hexCodepointsTextField.textProperty().addListener((observable, oldValue, newValue) -> {
      if (this.internalTextUpdate) return;
      this.internalTextUpdate = true;
      this.fromHexCodepoints(newValue);
      this.internalTextUpdate = false;
    });

    gridPane.addRow(
        0,
        new Label(language.translate("text.label")),
        this.charsTextField
    );
    gridPane.addRow(
        1,
        new Label(language.translate("decimal_codepoints.label")),
        this.decimalCodepointsTextField
    );
    gridPane.addRow(
        2,
        new Label(language.translate("hex_codepoints.label")),
        this.hexCodepointsTextField
    );

    return gridPane;
  }

  private void fromChars(@NotNull String rawValue) {
    final StringJoiner decimalJoiner = new StringJoiner(" ");
    final StringJoiner hexJoiner = new StringJoiner(" ");
    rawValue.codePoints().forEach(c -> {
      decimalJoiner.add(String.valueOf(c));
      hexJoiner.add("U+" + "%04X".formatted(c));
    });
    this.decimalCodepointsTextField.setText(decimalJoiner.toString());
    this.hexCodepointsTextField.setText(hexJoiner.toString());
  }

  private void fromDecimalCodepoints(@NotNull String rawValue) {
    final StringBuilder charsJoiner = new StringBuilder();
    final StringJoiner hexJoiner = new StringJoiner(" ");
    for (final String part : rawValue.strip().split("\\s+")) {
      final int c;
      try {
        c = Integer.parseInt(part);
      } catch (final NumberFormatException e) {
        this.charsTextField.setText("");
        this.hexCodepointsTextField.setText("");
        break;
      }
      try {
        charsJoiner.append(new String(Character.toChars(c)));
      } catch (final IllegalArgumentException e) {
        this.charsTextField.setText("");
        this.decimalCodepointsTextField.setText("");
        break;
      }
      hexJoiner.add("U+" + "%04X".formatted(c));
    }
    this.charsTextField.setText(charsJoiner.toString());
    this.hexCodepointsTextField.setText(hexJoiner.toString());
  }

  private static final Pattern HEX_CODEPOINT = Pattern.compile("^(?:U\\+)?([\\da-fA-F]+)$");

  private void fromHexCodepoints(@NotNull String rawValue) {
    final StringBuilder charsJoiner = new StringBuilder();
    final StringJoiner decimalJoiner = new StringJoiner(" ");
    for (final String part : rawValue.strip().split("\\s+")) {
      final int c;
      final Matcher matcher = HEX_CODEPOINT.matcher(part);
      if (matcher.find())
        c = Integer.parseInt(matcher.group(1), 16);
      else {
        this.charsTextField.setText("");
        this.decimalCodepointsTextField.setText("");
        break;
      }
      try {
        charsJoiner.append(new String(Character.toChars(c)));
      } catch (final IllegalArgumentException e) {
        this.charsTextField.setText("");
        this.decimalCodepointsTextField.setText("");
        break;
      }
      decimalJoiner.add(String.valueOf(c));
    }
    this.charsTextField.setText(charsJoiner.toString());
    this.decimalCodepointsTextField.setText(decimalJoiner.toString());
  }

  public void show() {
    this.stage.show();
  }

  private void onAboutAction() {
    this.aboutDialog.showAndWait();
  }

  private void onLanguageSettingSelection(@NotNull LanguageSetting languageSetting) {
    if (languageSetting == this.currentLanguageSetting) return;
    try {
      this.config.withLanguage(languageSetting).save();
      this.currentLanguageSetting = languageSetting;
      Alerts.info(
          this.config,
          "alert.needs_restart.header",
          null,
          null
      );
    } catch (final IOException e) {
      App.LOGGER.exception(e);
      Alerts.error(
          this.config,
          "alert.save_error.header",
          null,
          null
      );
    }
  }

  private void onThemeSettingSelection(@NotNull ThemeSetting themeSetting) {
    if (themeSetting == this.currentThemeSetting) return;
    try {
      this.config.withTheme(themeSetting).save();
      this.currentThemeSetting = themeSetting;
      Alerts.info(
          this.config,
          "alert.needs_restart.header",
          null,
          null
      );
    } catch (final IOException e) {
      App.LOGGER.exception(e);
      Alerts.error(
          this.config,
          "alert.save_error.header",
          null,
          null
      );
    }
  }
}
