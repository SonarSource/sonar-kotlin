package checks;

/**
 * Stand-in for a generic Java library class (e.g. Project Reactor's {@code Mono})
 * that is commonly parameterized with {@code Void} and cannot use Kotlin's {@code Unit}.
 */
public interface JavaMono<T> {
  T get();
}
