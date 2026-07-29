import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;


public class Block {

    /* ATTRIBUTES */
    // Header
    private int version;
    private byte[] merkleRoot;
    private Instant timestamp;
    private byte[] hashPointer;
    private long nonce;
    private int difficulty; 

    // Payload
    private List<Transaction> transactions;


    /* CONSTRUCTOR */
    public Block(List<Transaction> transPool, byte[] pointer, Instant timestamp) {
        this.version = 1;
        this.timestamp = timestamp; 
        this.hashPointer = pointer;
        this.nonce = 0;
        if (transPool == null) {    
            this.transactions = new ArrayList<>();
        } else {
            this.transactions = new ArrayList<>(transPool);
        }
        this.merkleRoot = calculateMerkleRoot(this.transactions);
    }

    /* METHODS */
    public List<Transaction> getTransactions() {
        return this.transactions;
    }

    public byte[] getHashPointer() {
        return this.hashPointer;
    }

    public int getDifficulty() {
        return this.difficulty;
    }

    private byte[] calculateMerkleRoot(List<Transaction> transactions) {
        if (transactions == null || transactions.isEmpty()) {
             return new byte[0];
        }

        // Extracts the hashes of each transaction
        List<byte[]> currentLevel = new ArrayList<>();
        for (Transaction t : transactions) {
            currentLevel.add(t.getHash());
        }
        return processMerkleLevel(currentLevel);
    }

    private byte[] processMerkleLevel(List<byte[]> hashes) {
        // Base case: a single hash is left (root)
        if (hashes.size() == 1) {
            return hashes.get(0);
        }
        // ...
        List<byte[]> currentLevel = new ArrayList<>(hashes);
        // If list has odd size, duplicates its last element
        if (currentLevel.size() % 2 != 0) {
            currentLevel.add(currentLevel.get(currentLevel.size() - 1));
        }
        // Calculates next level of the Merkle Tree
        List<byte[]> nextLevel = new ArrayList<>();
        try {
            // Instantiate the hash algorithm
            MessageDigest hashingFunc = MessageDigest.getInstance("SHA-256");
            for (int i = 0; i < currentLevel.size(); i += 2) {
                byte[] h1, h2, h12;
                h1 = currentLevel.get(i);
                h2 = currentLevel.get(i+1);
                // Concatenates the hashes
                h12 = new byte[h1.length + h2.length];
                System.arraycopy(h1, 0, h12, 0, h1.length);
                System.arraycopy(h2, 0, h12, h1.length, h2.length);
                // Hash of the father node
                nextLevel.add(hashingFunc.digest(h12));
            }
        } catch (NoSuchAlgorithmException nsae) {
            System.out.println("The specified hashing algorithm was not found");
        }
        return processMerkleLevel(nextLevel);
    }
    public byte[] computeBlockHash() {
        int totalLength = Integer.BYTES * 2+ Long.BYTES * 2 + hashPointer.length + merkleRoot.length ;
        ByteBuffer buf = ByteBuffer.allocate(totalLength);
        buf.putInt(this.version);
        buf.put(merkleRoot);
        buf.putLong(this.timestamp.toEpochMilli());
        buf.put(hashPointer);
        buf.putLong(nonce);
        buf.putInt(difficulty);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(buf.array()); 
        } catch (NoSuchAlgorithmException sae) {
            System.out.println("The specified hashing algorithm was not found");
            throw new RuntimeException(sae);
        }
    }

    public void mine(int difficulty) {
        // In this way, it tracks the mining rule/difficulty
        this.difficulty = difficulty;
        this.nonce = 0;
        byte[] hash = this.computeBlockHash();
        while(!verifyProofOfWork(hash, difficulty)) {
            this.nonce += 1;
            hash = this.computeBlockHash();
        }
    }

    public boolean verifyProofOfWork(byte[] hash, int difficulty) {
        String hex = HexFormat.of().formatHex(hash);
        return hex.startsWith("0".repeat(difficulty));
    }


}