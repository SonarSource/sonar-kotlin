import java.lang.RuntimeException
import org.junit.Test
import org.junit.Ignore

public class SubmoduleTest {
  @Test
  public fun success() {
    Thread.sleep(1000)
  }

  @Ignore
  @Test
  public fun ignored() {
  }
}