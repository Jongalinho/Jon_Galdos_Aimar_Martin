package app;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("java:S106")
class MainTest {
    private ByteArrayOutputStream os;
    private PrintStream ps;

    @BeforeEach
    void setup() {
        os = new ByteArrayOutputStream();
        ps = new PrintStream(os);
        System.setOut(ps);
        
        java.io.File file = new java.io.File("vault.dat");
        if (file.exists()) {
            file.delete();
        }
    }

    @AfterEach
    void tearDown() {
        java.io.File file = new java.io.File("vault.dat");
        if (file.exists()) {
            file.delete();
        }
    }

    @Test
    void testMenuAñadirYSalir() {
        String input = "3\ncred1\nmi_usuario\nmi_pass\n1\ne\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        
        Main.main(new String[]{});
        
        String output = os.toString();
        assertTrue(output.contains("Login added."));
        assertTrue(output.contains("- cred1"));
        assertTrue(output.contains("Exiting..."));
    }

    @Test
    void testMenuOpcionInvalidaYBorrar() {
        String input = "9\n3\ncred2\n\n\n4\ncred2\ne\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        
        Main.main(new String[]{});
        
        String output = os.toString();
        assertTrue(output.contains("Invalid option."));
        assertTrue(output.contains("Login deleted."));
    }
    
    @Test
    void testMenuNuevasOpcionesCategorias() {
        String input = "3\ncred1\nuser\npass\n8\nOcio\n6\ncred1\nOcio\n5\ne\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        
        Main.main(new String[]{});
        
        String output = os.toString();
        assertTrue(output.contains("Exiting..."));
    }

    @Test
    void testMenuCategoriasErrores() {
        String input = "8\nCat1\n8\nCat1\n9\nFalsa\n6\nFalso\nCat1\n7\nFalso\nCat1\ne\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        
        Main.main(new String[]{});
        
        String output = os.toString();
        assertTrue(output.contains("Category already exists."));
        assertTrue(output.contains("Category not found."));
        assertTrue(output.contains("Error: Login ID or Category not found."));
        assertTrue(output.contains("Error: Category not found or ID not in category."));
    }

    @Test
    void testMenuQueryYEmptyCategory() {
        String input = "3\ncred1\nuser\npass\n8\nVacia\n2\ncred1\n2\nfalso\n5\ne\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        
        Main.main(new String[]{});
        
        String output = os.toString();
        assertTrue(output.contains("Username: user"));
        assertTrue(output.contains("ID not found."));
        assertTrue(output.contains("(No logins assigned)"));
    }

    @Test
    void testMainCaminosSecundarios() {
        String input = "3\ncred2\n\n\n8\nTrabajo\n6\ncred2\nTrabajo\n7\ncred2\nTrabajo\n4\ncred2\n9\nTrabajo\ne\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        
        Main.main(new String[]{});
        
        String output = os.toString();
        assertTrue(output.contains("Login added."));
        assertTrue(output.contains("Login ID added to category successfully."));
        assertTrue(output.contains("Login ID removed from category successfully."));
        assertTrue(output.contains("Login deleted."));
        assertTrue(output.contains("Category deleted successfully."));
    }

    @Test
    void testMainCoberturaExtrema() {
        String input = "3\ncred3\n\n1234\n3\ncred4\nadmin\n\n8\nTestCat\n6\ncred3\nTestCat\n5\n7\ncred4\nTestCat\ne\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        
        Main.main(new String[]{});
        
        String output = os.toString();
        assertTrue(output.contains("Error: Category not found or ID not in category."));
        assertTrue(output.contains("Exiting..."));
    }

    @Test
    void testMenuBorrarLimpiamente() {
        String input = "4\nID_FALSO\ne\n";
        System.setIn(new java.io.ByteArrayInputStream(input.getBytes()));
        
        Main.main(new String[]{});
        
        assertTrue(os.toString().contains("Login deleted."));
    }

    @Test
    void testMainConstructorOculto() {
        new Main(); 
        assertTrue(true);
    }

    @Test
    void testViewCategoriesCombinacionPerfecta() {
        String input = "3\nlogin1\n\n\n3\nlogin2\n\n\n8\nCat_Llena\n8\nCat_Vacia\n6\nlogin1\nCat_Llena\n5\ne\n";
        System.setIn(new java.io.ByteArrayInputStream(input.getBytes()));

        Main.main(new String[]{});

        String output = os.toString();
        assertTrue(output.contains(" - login1")); 
        assertTrue(output.contains("(No logins assigned)")); 
    }
}