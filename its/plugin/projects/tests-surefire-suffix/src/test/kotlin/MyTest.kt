import java.lang.RuntimeException
import org.junit.Test
import org.junit.Ignore

public class MyTest {
  @Test
  public fun success() {
    Thread.sleep(1000)
  }

  @Ignore
  @Test
  public fun ignored() {
  }
}