import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.ECGenParameterSpec;
import java.util.HexFormat;
import java.security.SecureRandom;


public class Wallet {

    private PrivateKey privKey;
    private PublicKey pubKey;
    private String address;
    private String walletName; // Assigned by the end user

    // Constructor 
    public Wallet() {
        // Generates the key pair 
        KeyPair keyPair = this.genKeys();
        if (keyPair != null) {
            this.privKey = keyPair.getPrivate();
            this.pubKey = keyPair.getPublic();
        }
        // Creates the address of the wallet
        generateAddress();
    }

    // Getters and setters
    public PublicKey getPublicKey() {
        return this.pubKey; 
    }
    public String getAddress() {
        return this.address;
    }
    public String getName() {
        return this.walletName;
    }
    public void setName(String customName) {
        this.walletName = customName;
    }

    /* METHODS */
    // Function used when a wallet is "loaded" from memory
    public void loadWallet(PrivateKey savedPrivKey, PublicKey savedPubKey, String savedName) {
        this.privKey = savedPrivKey;
        this.pubKey = savedPubKey;
        this.walletName = savedName;
        generateAddress(); // Recalculate address for consistency 
    }

    // This is the function used to create the keyPair that must be stored in the wallet
    private KeyPair genKeys() {
        KeyPairGenerator keyGen = null; 
        ECGenParameterSpec ecSpec = null; 
        KeyPair keyPair = null; 
        try {
            // Generator for Elliptic Curves Cryptography
            keyGen = KeyPairGenerator.getInstance("EC");
            // Specifics the curves used in blockchain (no dependence from providers)
            ecSpec = new ECGenParameterSpec("secp256r1");
            // Init the keyGen with the curve and cryptographically secure PRNG  
            keyGen.initialize(ecSpec, new SecureRandom()); 
            // Generates the keys
            keyPair = keyGen.generateKeyPair();
        } catch (InvalidAlgorithmParameterException iaep) {
            System.out.println("Invalid parameters were passed!");
        } catch (NoSuchAlgorithmException e) {
            System.out.println("The specified algorithm does not exist!");
        }
        return keyPair;
    }

    // Used to generate an address for the wallet (hash of the pubKey)
    public static String deriveAddress(PublicKey pubKey) {
        try {
            // Get an instance of the SHA-256 hashing algorithm
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            // Execute hashing
            byte[] hashBytes = digest.digest(pubKey.getEncoded()); 
            // Converts the array byte into hex 
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available");
        }
    }

    private void generateAddress() {
        if (this.pubKey == null) return;
        this.address = deriveAddress(this.pubKey);
    }

    // Function used by the wallet to sign a transaction -- TODO
    private byte[] signTransaction(Transaction transaction) {
        // Data to sign
        byte[] data = transaction.getTransactionDataBytes();
        try {
            // Gets an instance of the ECDSA algorithm
            Signature ecdsa = Signature.getInstance("SHA256withECDSA");
            // Specifies the private key used for signing the messages 
            ecdsa.initSign(this.privKey); // Throws InvalidKeyException
            // Specifies the data that needs to be signed
            ecdsa.update(data);
            // Generates the sign
            byte[] sign = ecdsa.sign();
            return sign;
        } catch (NoSuchAlgorithmException nsae) {
            throw new RuntimeException("The specified algorithm was not found");
        } catch (InvalidKeyException ike) {
            throw new RuntimeException("An error regarding the private key occurred");
        } catch (SignatureException se) {
            throw new RuntimeException("An error regarding the sign occurred");
        }
    }

    // Function used to transfer money into another account
    public Transaction transferMoney(String destination, double amount) {
        Transaction transaction = new Transaction(getAddress(), destination, amount, getPublicKey());
        byte[] sign = signTransaction(transaction);
        transaction.setSignature(sign); // Adds the sign to the transaction
        return transaction;
    }

    // Function to visualize the current balance for the wallet
    public void printBalance(Blockchain blockchain) { 
        System.out.println("The current balance for the wallet " + getName() + " is: " + blockchain.getBalance(this.address));
    }
}
