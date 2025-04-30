package net.darmo_creations.unicode_converter.config;

import net.darmo_creations.unicode_converter.*;
import net.darmo_creations.unicode_converter.config.theme.*;
import net.darmo_creations.unicode_converter.utils.*;
import org.ini4j.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.util.*;

/**
 * This class holds configuration options for the whole application.
 * <p>
 * All options except debug are mutable at runtime. But some will require the configuration to be saved to disk
 * and the application to be restarted to apply.
 */
public final class Config implements Cloneable {
  private static final Map<LanguageSetting, Language> LANGUAGES = new EnumMap<>(LanguageSetting.class);

  private static final File SETTINGS_FILE = new File("settings.ini");

  private static final String APP_SECTION = "App";
  private static final String LANGUAGE_OPTION = "language";
  private static final String THEME_OPTION = "theme";

  /**
   * Load the configuration from the settings file.
   * <p>
   * It loads all available resource bundles and themes.
   *
   * @return A configuration object.
   * @throws IOException     If an IO error occurs.
   * @throws ConfigException If the file could not be parsed correctly.
   */
  public static Config loadConfig() throws IOException, ConfigException {
    loadLanguages();
    Theme.loadThemes();

    final Wini ini = getOrCreateIniFile();

    final LanguageSetting languageSetting = StringUtils.stripNullable(ini.get(APP_SECTION, LANGUAGE_OPTION))
        .flatMap(LanguageSetting::fromId)
        .orElse(LanguageSetting.SYSTEM);

    final ThemeSetting themeSetting = StringUtils.stripNullable(ini.get(APP_SECTION, THEME_OPTION))
        .flatMap(ThemeSetting::fromId)
        .orElse(ThemeSetting.SYSTEM);

    try {
      return new Config(
          languageSetting,
          themeSetting
      );
    } catch (final IllegalArgumentException e) {
      throw new ConfigException(e);
    }
  }

  /**
   * Return the Ini file designated by {@link #SETTINGS_FILE}. If the file does not exist, it is created.
   *
   * @return The {@link Wini} wrapper object.
   * @throws IOException If the file does not exist and could not be created.
   */
  private static Wini getOrCreateIniFile() throws IOException {
    if (!SETTINGS_FILE.exists() && !SETTINGS_FILE.createNewFile())
      throw new IOException("Could not create %s file!".formatted(SETTINGS_FILE));
    return new Wini(SETTINGS_FILE);
  }

  /**
   * Load resource bundles for all available languages and populate {@link #LANGUAGES} field.
   *
   * @throws IOException If any IO error occurs.
   */
  private static void loadLanguages() throws IOException {
    LANGUAGES.clear();
    for (final var langSetting : LanguageSetting.LANGUAGES) {
      final String langCode = langSetting.id();
      final Locale locale = new Locale(langCode);
      final ResourceBundle bundle = getResourceBundle(locale);
      if (bundle != null) {
        final String langName = bundle.getString("language_name");
        LANGUAGES.put(langSetting, new Language(langCode, langName, locale, bundle));
      }
    }
    if (LANGUAGES.isEmpty()) throw new IOException("No languages found");
    final LanguageSetting defaultSetting = LanguageSetting.fromId(Locale.getDefault().getLanguage())
        .orElseThrow(() -> new IOException("No language found"));
    LANGUAGES.put(LanguageSetting.SYSTEM, LANGUAGES.get(defaultSetting));
  }

  /**
   * Return the resource bundle for the given locale.
   *
   * @param locale A locale.
   * @return The locale’s resources.
   */
  private static ResourceBundle getResourceBundle(@NotNull Locale locale) {
    return ResourceBundle.getBundle(
        App.RESOURCES_ROOT.substring(1).replace('/', '.') + "translations.ui",
        locale
    );
  }

  /**
   * Get the {@link Language} for the given {@link LanguageSetting}.
   *
   * @param languageSetting The setting to get the language of.
   * @return The corresponding {@link Language}.
   */
  public static Language getLanguage(@NotNull LanguageSetting languageSetting) {
    return LANGUAGES.get(languageSetting);
  }

  private final LanguageSetting languageSetting;
  private final Language language;
  private final ThemeSetting themeSetting;
  private final Theme theme;

  /**
   * Create a configuration object.
   *
   * @param languageSetting Language setting to use.
   * @param themeSetting    Theme setting to use.
   */
  public Config(
      @NotNull LanguageSetting languageSetting,
      @NotNull ThemeSetting themeSetting
  ) {
    this.languageSetting = Objects.requireNonNull(languageSetting);
    this.language = LANGUAGES.get(languageSetting);
    this.themeSetting = Objects.requireNonNull(themeSetting);
    this.theme = Theme.getTheme(themeSetting);
  }

  /**
   * The language setting to use.
   */
  public LanguageSetting languageSetting() {
    return this.languageSetting;
  }

  /**
   * The language to use.
   */
  public Language language() {
    return this.language;
  }

  /**
   * The theme setting to use.
   */
  public ThemeSetting themeSetting() {
    return this.themeSetting;
  }

  /**
   * The theme to use.
   */
  public Theme theme() {
    return this.theme;
  }

  /**
   * Return a copy of this object and replace its language setting by the given one.
   *
   * @param languageSetting The language setting to use.
   * @return A new configuration object.
   */
  public Config withLanguage(@NotNull LanguageSetting languageSetting) {
    return new Config(
        languageSetting,
        this.themeSetting
    );
  }

  /**
   * Return a copy of this object and replace its theme setting by the given one.
   *
   * @param themeSetting The theme setting to use.
   * @return A new configuration object.
   */
  public Config withTheme(@NotNull ThemeSetting themeSetting) {
    return new Config(
        this.languageSetting,
        themeSetting
    );
  }

  /**
   * Clone this object.
   *
   * @return A new deep copy of this object.
   */
  @Override
  public Config clone() {
    try {
      return (Config) super.clone();
    } catch (final CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Save this configuration object to the disk.
   */
  public void save() throws IOException {
    App.LOGGER.info("Saving config…");
    final Wini ini = getOrCreateIniFile();
    ini.put(APP_SECTION, LANGUAGE_OPTION, this.languageSetting.id());
    ini.put(APP_SECTION, THEME_OPTION, this.themeSetting.id());
    ini.store();
    App.LOGGER.info("Done.");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || this.getClass() != o.getClass())
      return false;
    final Config that = (Config) o;
    return Objects.equals(this.language, that.language)
        && Objects.equals(this.theme, that.theme);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        this.language,
        this.theme
    );
  }
}
