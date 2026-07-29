import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;

/** The transaction's structure is inspired by Account-based protocols */

public class Transaction {
    
    private double funds;
    private String src; // Wallet Address
    private String dst; // Wallet Address
    private byte[] sign;
    private final PublicKey senderPubKey;
    private final long timestamp; // To avoid reply attacks

    public Transaction(String srcAdd, String dstAdd, double quantity, PublicKey pubKey) {
        this.src = srcAdd; 
        this.dst = dstAdd;
        this.funds = quantity;
        this.senderPubKey = pubKey;
        this.timestamp = System.currentTimeMillis();
    }

    public String getDst() {
        return this.dst; 
    }

    public String getSrc() {
        return this.src;
    }

    public double getFunds() {
        return this.funds;
    }

    public byte[] getSignature() {
        return this.sign; 
    }

    public void setSignature(byte[] sign) {
        this.sign = sign;
    }

    public byte[] getTransactionDataBytes() {
        // Converts the addresses in byte arrays
        byte[] srcBytes = this.src.getBytes(StandardCharsets.UTF_8);
        byte[] dstBytes = this.dst.getBytes(StandardCharsets.UTF_8);
        // Calculates the dimension of the buffer
        int totalLength = srcBytes.length + dstBytes.length + Double.BYTES + Long.BYTES;
        // Creates the buffer
        ByteBuffer buffer = ByteBuffer.allocate(totalLength);
        buffer.put(srcBytes);
        buffer.put(dstBytes);
        buffer.putDouble(this.funds);
        buffer.putLong(this.timestamp);
        // Returns the byte array 
        return buffer.array();
    }

    public boolean verifySignature() {
        // Checks if sign has been set 
        if (this.sign == null) {
            System.out.println("Verification has failed: no signature is present");
            return false;
        }
        try {
            // Gets an instance of the ECDSA algorithm
            Signature ecdsa = Signature.getInstance("SHA256withECDSA");
            // Specifies the public key used for verifying the messages             
            ecdsa.initVerify(this.senderPubKey); // Throws InvalidKeyException
            // Loads the signed data to verify
            byte[] data = getTransactionDataBytes(); 
            ecdsa.update(data);
            // Verify the signature
            return ecdsa.verify(this.sign);
        } catch (NoSuchAlgorithmException nsae) {
            System.out.println("The specified algorithm was not found");
        } catch (InvalidKeyException ike) {
            System.out.println("The public key used to initialize the ECDSA is invalid");
        } catch (SignatureException se) {
            System.out.println("The signature couldn't be verified");
        }
        return false; 
    }

    // To guarantee authentication of the source
    public boolean verifyAddressBinding() {
        String derivedAddress = Wallet.deriveAddress(this.senderPubKey);
        return derivedAddress.equals(this.src);
    }

    public boolean isValid() {
        return verifyAddressBinding() && verifySignature();
    }

    public byte[] getHash() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(this.getTransactionDataBytes());
            if (this.sign != null) {
                digest.update(this.sign); // Includes the signature 
            } 
            return digest.digest();
        } catch (NoSuchAlgorithmException nsae) {
            throw new RuntimeException("Algoritmo SHA-256 non trovato", nsae);
        }
    }
}