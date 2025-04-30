package net.darmo_creations.unicode_converter;

import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import net.darmo_creations.unicode_converter.config.*;
import net.darmo_creations.unicode_converter.config.theme.*;
import net.darmo_creations.unicode_converter.ui.*;
import net.darmo_creations.unicode_converter.ui.dialogs.*;
import net.darmo_creations.unicode_converter.utils.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.util.*;
import java.util.stream.*;

public class AppController {
  private final Stage stage;
  private final Config config;

  private LanguageSetting currentLanguageSetting;
  private ThemeSetting currentThemeSetting;

  private boolean internalTextUpdate;

  private final AboutDialog aboutDialog;
  private final PlainTextField plainTextField = new PlainTextField();
  private final UnicodeDecimalCodepointsTextField unicodeDecimalCodepointsTextField = new UnicodeDecimalCodepointsTextField();
  private final UnicodeHexadecimalCodepointsTextField unicodeHexadecimalCodepointsTextField = new UnicodeHexadecimalCodepointsTextField();
  private final Set<CodepointField> codepointFields = new HashSet<>();

  public AppController(@NotNull Stage stage, @NotNull Config config) {
    this.stage = Objects.requireNonNull(stage);
    this.config = Objects.requireNonNull(config);

    this.codepointFields.add(this.plainTextField);
    this.codepointFields.add(this.unicodeDecimalCodepointsTextField);
    this.codepointFields.add(this.unicodeHexadecimalCodepointsTextField);

    final Theme theme = config.theme();
    final Image icon = theme.getAppIcon();
    if (icon != null) stage.getIcons().add(icon);
    stage.setMinWidth(400);
    stage.setMinHeight(200);
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

    this.codepointFields.forEach(
        field -> ((TextField) field).textProperty()
            .addListener((observable, oldValue, newValue) -> {
              if (this.internalTextUpdate) return;
              this.internalTextUpdate = true;
              this.updateOtherFields(field);
              this.internalTextUpdate = false;
            })
    );

    gridPane.addRow(
        0,
        new Label(language.translate("text.label")),
        this.plainTextField
    );
    gridPane.addRow(
        1,
        new Label(language.translate("decimal_codepoints.label")),
        this.unicodeDecimalCodepointsTextField
    );
    gridPane.addRow(
        2,
        new Label(language.translate("hex_codepoints.label")),
        this.unicodeHexadecimalCodepointsTextField
    );

    final ColumnConstraints cc2 = new ColumnConstraints();
    cc2.setHgrow(Priority.ALWAYS);
    gridPane.getColumnConstraints().addAll(
        new ColumnConstraints(),
        cc2
    );

    return gridPane;
  }

  private void updateOtherFields(final @NotNull CodepointField sourceField) {
    final Set<CodepointField> otherFields = this.codepointFields.stream()
        .filter(f -> f != sourceField)
        .collect(Collectors.toSet());
    try {
      final List<Integer> codepoints = sourceField.getCodepoints();
      for (final var otherField : otherFields)
        otherField.setCodepoints(codepoints);
    } catch (final CodepointException e) {
      otherFields.forEach(CodepointField::clear);
    }
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
