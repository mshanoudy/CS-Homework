import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.*;
import java.util.UUID;

/**
 * This class makes use of the AES and RSA cryptosystems to encrypt and decrypt a message passed at the command line.
 * If no message is passed, the program will run the extra credit routine which generates 100 random strings and
 * compares the amount of time necessary to encrypt them by each cryptosystem
 */
public class CryptoTest
{
    /**
     * Main class which reads in command line argument
     *
     * @param args The message to encrypt
     * @throws Exception
     */
    public static void main(String[] args) throws Exception
    {
        // Set the provider as bouncy castle
        Security.addProvider(new BouncyCastleProvider());

        if (args.length > 0)
        {// If command line argument was passed
            String message = args[0];

            // Run the AESTest
            System.out.println("Running AES Test...");
            System.out.print("Output = ");
            System.out.println(runAESTest(message));

            // Run the RSATest
            System.out.println("Running RSA Test...");
            System.out.print("Output = ");
            System.out.println(runRSATest(message));
        }
        else
            runExtraCredit();
    }

    /**
     * Runs the AES test.
     * Generates a 128-bit AES key
     * Encrypts the string passed to the method using AES
     * Decrypts the resulting AES ciphertext and returns the plaintext
     *
     * @param message The message to encrypt
     * @return The decrypted plaintext
     * @throws Exception
     */
    private static String runAESTest(String message) throws Exception
    {
        // Create the key generator
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES", "BC");
        keyGenerator.init(128); // Initialize key size to 128-bits

        // Generate SecretKey
        SecretKey secretKey = keyGenerator.generateKey();

        // Create Cipher, set for encryption
        Cipher cipher = Cipher.getInstance("AES", "BC");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        // Encrypt the message
        byte[] encryptedData = cipher.doFinal(message.getBytes());

        // Set Cipher for decryption
        cipher.init(Cipher.DECRYPT_MODE, secretKey);

        // Decrypt the message
        byte[] decryptedData = cipher.doFinal(encryptedData);

        // Print decrypted plaintext
        return new String(decryptedData);
    }

    /**
     * Runs the RSA test.
     * Generates an RSA public/private key pair
     * Encrypts the string passed to the method using RSA
     * Decrypts the resulting RSA ciphertext and returns the paintext
     * Also Generates and verifies an RSA signature over the message
     *
     * @param message The message to encrypt
     * @return The decrypted plaintext, null if signature was not verified
     * @throws Exception
     */
    private static String runRSATest(String message) throws Exception
    {
        // Create the key pair generator
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", "BC");
        keyPairGenerator.initialize(1024); // Initialize key size to 1024-bits

        // Generate public/private key pair
        KeyPair    keyPair    = keyPairGenerator.generateKeyPair();
        PublicKey  publicKey  = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();

        // Create signature
        Signature signature = Signature.getInstance("NONEwithRSA", "BC");
        signature.initSign(privateKey); // Initialize with private key

        // Create Cipher, set for encryption
        Cipher cipher = Cipher.getInstance("RSA", "BC");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);

        // Update signature and generate signature bytes
        signature.update(message.getBytes());
        byte[] signatureData = signature.sign();

        // Encrypt the message
        byte[] encryptedData = cipher.doFinal(message.getBytes());

        // Set Cipher for decryption
        cipher.init(Cipher.DECRYPT_MODE, privateKey);

        // Decrypt the message
        byte[] decryptedData = cipher.doFinal(encryptedData);

        // Initialize signature for verification and update
        signature.initVerify(publicKey);
        signature.update(decryptedData);

        // Print decrypted plaintext
        if (signature.verify(signatureData))
            return new String(decryptedData);
        else
            return null;
    }

    /**
     * Runs the extra credit routine.
     * Generates an array of 100 different random strings
     * Times how long it takes to encrypt all 100 strings using AES
     * Times how long it takes to encrypt all 100 strings using RSA
     * Output how many times faster AES encryption is than RSA encryption
     *
     * @throws Exception
     */
    private static void runExtraCredit() throws Exception
    {
        // Generate 100 random strings
        String[] strings = new String[100];
        for (int x = 0; x < 100; x++)
            strings[x] = UUID.randomUUID().toString();

        // Declare timing variables
        long startTime, endTime, aesTime, rsaTime;
        String temp;

        // Start AES test
        startTime = System.nanoTime();
        for (String message : strings)
            aesExtra(message);
        endTime  = System.nanoTime();
        aesTime  = endTime - startTime;

        // Start RSA test
        startTime = System.nanoTime();
        for (String message : strings)
            rsaExtra(message);
        endTime  = System.nanoTime();
        rsaTime  = endTime - startTime;

        // Print result
        System.out.println("AES is " + (rsaTime / aesTime) + " times faster than RSA");
    }

    /**
     * Encrypts a message using AES
     *
     * @param message The message to encrypt
     * @throws Exception
     */
    private static void aesExtra(String message) throws Exception
    {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES", "BC");
        keyGenerator.init(128);
        Cipher cipher = Cipher.getInstance("AES", "BC");
        cipher.init(Cipher.ENCRYPT_MODE, keyGenerator.generateKey());
        cipher.doFinal(message.getBytes());
    }

    /**
     * Encrypts a message using RSA
     *
     * @param message The message to encrypt
     * @throws Exception
     */
    private static void rsaExtra(String message) throws Exception
    {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", "BC");
        keyPairGenerator.initialize(1024);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        Cipher  cipher  = Cipher.getInstance("RSA", "BC");
        cipher.init(Cipher.ENCRYPT_MODE, keyPair.getPublic());
        cipher.doFinal(message.getBytes());
    }
}
