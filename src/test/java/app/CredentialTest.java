package app;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("java:S106")
class CredentialTest {

    @Test
    void testCredential() {
        Credential cred = new Credential("aimar", "1234");
        assertEquals("aimar", cred.getUsername());
        assertEquals("1234", cred.getPassword());
    }
    @Test
    void testCategorias() {
        Credential cred = new Credential("aimar", "1234");
        assertTrue(cred.getCategories().isEmpty());

        cred.addCategory("Universidad");
        assertTrue(cred.getCategories().contains("Universidad"));

        cred.removeCategory("Universidad");
        assertFalse(cred.getCategories().contains("Universidad"));
    }

    @Test
    void testCategoriesNullRecuperacion() throws Exception {
        Credential cred = new Credential("aimar", "1234");
        
        java.lang.reflect.Field field = Credential.class.getDeclaredField("categories");
        field.setAccessible(true);
        field.set(cred, null);
        
        assertNotNull(cred.getCategories());
        assertTrue(cred.getCategories().isEmpty());
    }

    @Test
    @SuppressWarnings("java:S2925")
    void testLastEditDate() throws InterruptedException {
        Credential cred = new Credential("syncUser", "pass123");
        long initialDate = cred.getLastEditDate();
        assertTrue(initialDate > 0);

        Thread.sleep(10); 

        cred.addCategory("SyncCat");
        long afterAddDate = cred.getLastEditDate();
        assertTrue(afterAddDate > initialDate);

        Thread.sleep(10);

        cred.removeCategory("SyncCat");
        long afterRemoveDate = cred.getLastEditDate();
        assertTrue(afterRemoveDate > afterAddDate);
    }
}