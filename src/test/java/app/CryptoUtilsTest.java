package app;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import static org.junit.jupiter.api.Assertions.*;

class CryptoUtilsTest {

    @Test
    void testEncryptAndDecryptSuccess() throws Exception {
        String original = "secreto_super_seguro_123";
        String password = "miPasswordMaestra";

        String encrypted = CryptoUtils.encrypt(original.getBytes(StandardCharsets.UTF_8), password);
        assertNotEquals(original, encrypted);

        byte[] decrypted = CryptoUtils.decrypt(encrypted, password);
        assertEquals(original, new String(decrypted, StandardCharsets.UTF_8));
    }

    @Test
    void testDecryptWithWrongPasswordFails() throws Exception {
        String encrypted = CryptoUtils.encrypt("datos".getBytes(StandardCharsets.UTF_8), "passCorrecta");
        assertThrows(GeneralSecurityException.class, () -> CryptoUtils.decrypt(encrypted, "passIncorrecta"));
    }

    @Test
    void testDecryptShortPayloadFails() {
        String tooShort = Base64.getEncoder().encodeToString(new byte[]{1, 2, 3});
        assertThrows(GeneralSecurityException.class, () -> CryptoUtils.decrypt(tooShort, "pass"));
    }
}
