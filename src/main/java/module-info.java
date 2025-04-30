module net.darmo_creations.unicode_converter {
  requires javafx.graphics;
  requires javafx.controls;
  requires com.jthemedetector;
  requires ini4j;
  requires org.jetbrains.annotations;
  requires java.desktop;

  // Make App accessible to JavaFX, other classes accessible from App’s public interface
  // are not exported because it’s not necessary
  exports net.darmo_creations.unicode_converter to javafx.graphics;
}