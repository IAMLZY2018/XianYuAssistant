import com.sun.tools.javac.Main;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author IAMLZY
 * @date 2026/4/11 14:07
 * @description
 */

@SpringBootTest(classes = Main.class)
public class Test {
    @org.junit.jupiter.api.Test
    public void test() {
        System.out.println("Hello World");
    }
}
