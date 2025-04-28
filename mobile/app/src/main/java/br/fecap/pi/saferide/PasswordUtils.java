package br.fecap.pi.saferide;

import android.util.Base64;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class PasswordUtils {

    // Gerando o hash da senha usando PBKDF2
    public static String hashPassword(String password, String salt){
        try
        {
            int iterations = 10000;
            char[] chars = password.toCharArray();
            byte[] saltBytes = Base64.decode(salt, Base64.NO_WRAP);

            PBEKeySpec spec = new PBEKeySpec(chars, saltBytes, iterations, 256);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");

            byte[] hash = skf.generateSecret(spec).getEncoded();
            return Base64.encodeToString(hash, Base64.NO_WRAP);
        }
        catch (NoSuchAlgorithmException | InvalidKeySpecException e){
            throw new RuntimeException(e);
        }
    }

    // Gera um salt aleatório
    public static String generateSalt(){
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);

        return Base64.encodeToString(salt, Base64.NO_WRAP);
    }
}
