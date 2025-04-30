package net.darmo_creations.unicode_converter;

import javafx.application.*;
import javafx.stage.*;
import net.darmo_creations.unicode_converter.config.*;
import net.darmo_creations.unicode_converter.utils.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;

public class App extends Application {
  public static final String NAME = "Unicode Converter";
  public static final String VERSION = "2.0";

  public static final Logger LOGGER = new Logger(NAME);
  /**
   * Jar path to the resources’ root directory.
   */
  public static final String RESOURCES_ROOT = "/net/darmo_creations/unicode_converter/";
  /**
   * Jar path to the images directory.
   */
  public static final String IMAGES_PATH = RESOURCES_ROOT + "images/";
  /**
   * Path of the current working directory.
   */
  public static final Path CURRENT_DIR = Paths.get("").toAbsolutePath();

  /**
   * Application’s resource bundlo for the currently selected language.
   */
  private static ResourceBundle resourceBundle;

  /**
   * Return the resource bundle of the currently selected language.
   */
  public static ResourceBundle getResourceBundle() {
    if (resourceBundle == null)
      resourceBundle = config.language().resources();
    return resourceBundle;
  }

  private static HostServices hostServices;

  /**
   * Open a URL in the user’s default web browser.
   *
   * @param url URL to open.
   */
  public static void openURL(@NotNull String url) {
    hostServices.showDocument(url);
  }

  /**
   * App’s global configuration object.
   */
  private static Config config;

  @Override
  public void start(Stage stage) {
    LOGGER.info("Running %s (v%s)".formatted(NAME, VERSION));
    hostServices = this.getHostServices();
    final AppController controller = new AppController(stage, config);
    controller.show();
  }

  public static void main(String[] args) {
    try {
      config = Config.loadConfig();
    } catch (final IOException | ConfigException e) {
      generateCrashReport(e);
      System.exit(1);
      return; // To shut up compiler errors
    }
    try {
      launch();
    } catch (final Exception e) {
      generateCrashReport(e.getCause()); // JavaFX wraps exceptions into a RuntimeException
      System.exit(2);
    }
  }

  /**
   * Generate a crash report from the given throwable object.
   *
   * @param throwable The throwable object that caused the unrecoverable crash.
   */
  public static void generateCrashReport(@NotNull Throwable throwable) {
    final LocalDateTime date = LocalDateTime.now();
    final StringWriter out = new StringWriter();
    try (final var s = new PrintWriter(out)) {
      throwable.printStackTrace(s);
    }
    final String template = """
        --- %s (v%s) Crash Report ---
        
        Time: %s
        Description: %s
        
        -- Detailled Stack Trace --
        %s
        
        -- Technical Information --
        System properties:
        %s
        """;
    final String message = template.formatted(
        NAME,
        VERSION,
        DateTimeUtils.format(date),
        throwable.getMessage(),
        out,
        getSystemProperties()
    );
    LOGGER.fatal(message);
    final Path logsDir = App.CURRENT_DIR.resolve("logs");
    if (!Files.exists(logsDir))
      try {
        Files.createDirectory(logsDir);
      } catch (final IOException e) {
        throw new RuntimeException(e);
      }
    final String fileName = "crash_report_%s.log".formatted(DateTimeUtils.formatFileName(date));
    try (final var fw = new FileWriter(logsDir.resolve(fileName).toFile())) {
      fw.write(message);
    } catch (final IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  /**
   * Return a list of some system properties.
   */
  public static String getSystemProperties() {
    final StringJoiner systemProperties = new StringJoiner("\n");
    final String userHome = System.getProperty("user.home");
    final String userName = System.getProperty("user.name");
    System.getProperties().entrySet().stream()
        .filter(entry -> {
          final String key = entry.getKey().toString();
          return !key.equals("user.home") && !key.equals("user.name");
        })
        .map(entry -> {
          final Object key = entry.getKey();
          String value = entry.getValue().toString();
          if (value.contains(userHome))
            value = value.replace(userHome, "~");
          if (value.contains(userName))
            value = value.replace(userName, "*USERNAME*");
          if (value.contains("\n"))
            value = value.replace("\n", "\\n");
          if (value.contains("\r"))
            value = value.replace("\n", "\\r");
          return new Pair<>(key, value);
        })
        .sorted(Comparator.comparing(entry -> entry.left().toString()))
        .forEach(property -> systemProperties.add("%s: %s".formatted(property.left(), property.right())));
    return systemProperties.toString();
  }
}
