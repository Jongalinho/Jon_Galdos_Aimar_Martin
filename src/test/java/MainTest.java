import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import static org.junit.jupiter.api.Assertions.*;

public class MainTest {
    private ByteArrayOutputStream os;
    private PrintStream ps;

    @BeforeEach
    public void setup() {
        os = new ByteArrayOutputStream();
        ps = new PrintStream(os);
        System.setOut(ps);
    }

    @Test
    public void testMenuAñadirYSalir() {
        String input = "3\ncred1\nmi_usuario\nmi_pass\n1\ne\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        
        Main.main(new String[]{});
        
        String output = os.toString();
        assertTrue(output.contains("Login added."));
        assertTrue(output.contains("- cred1"));
        assertTrue(output.contains("Exiting..."));
    }

    @Test
    public void testMenuOpcionInvalidaYBorrar() {
        String input = "9\n3\ncred2\n\n\n4\ncred2\ne\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        
        Main.main(new String[]{});
        
        String output = os.toString();
        assertTrue(output.contains("Invalid option."));
        assertTrue(output.contains("Login deleted."));
    }
}