package surefirebug.thing;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

public final class Main implements Callable<String> {

  private static final Supplier<String> DEFAULT_PROVIDER = () -> "Hello, World";


  public static void main(String... args) throws Exception {
    Main main = new Main();
    String text = main.call();

    System.out.println(text);
  }

  private final Supplier<String> supplier;

  public Main(Supplier<String> supplier) {
    this.supplier = supplier;
  }

  public Main() {
    this(DEFAULT_PROVIDER);
  }

  @Override
  public String call() {
    var value = supplier.get();

    if (value == null) {
      var annotations = supplier.getClass().getAnnotations();
      for (var annotation : annotations) {
        if (annotation.annotationType().getSimpleName().equals("Nullable")) {
          return "<null value>";
        }
      }
      throw new IllegalStateException("null without @Nullable annotation");
    }

    return value;
  }
}
