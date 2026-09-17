import java.util.List;
import java.util.Collections;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;

public class Blockchain {
    
    private List<Block> ledger;
    private List<Transaction> mempool;
    private static final byte[] GENESIS_PREV_HASH = new byte[32]; // This is a 32 byte array filled with zeros used as prevHash for genesis (Bitcoin-like)
    private static final String EXPECTED_GENESIS_HASH_HEX = "2f55162e9ac7e9e584677aeebbfc9f63aef043ad51a151cc633a3a1108f21488";
    private static final int DIFFICULTY = 4; // This is the number of nonce's zeros required to solve the puzzle 

    public Blockchain() {
        this.mempool = new ArrayList<>();
        this.ledger = new ArrayList<>();
        // Creates a genesis block, (fixed timestamp)
        Block genesis = new Block(null, new byte[32], Instant.ofEpochMilli(1710000000000L));
        this.ledger.add(genesis);
    }

    // Calculates the amount of funds already committed to pending transactions in the mempool
    private double getPendingOutflow(String address) {
        double total = 0.0;
        for (Transaction tx: this.mempool) {
            if (tx.getSrc().equals(address)) total += tx.getFunds();
        }
        return total; 
    }

    // Receives a tx from the wallet, validates it and puts it in the mempool
    public boolean submitTransaction(Transaction t) {
        if (!t.isValid()) return false;
        if (getBalance(t.getSrc()) - getPendingOutflow(t.getSrc()) <  t.getFunds()) return false;
        mempool.add(t);
        return true;
    }

    // Exposes a read-only view of the ledger
    public List<Block> getLedger() {
        return Collections.unmodifiableList(this.ledger);
    }

    public Block getLastBlock() {
        return this.ledger.get(this.ledger.size()-1);
    }

    public double getBalance(String address) {
        double balance = 0.0;
        for (Block b : ledger) {
            for (Transaction tx : b.getTransactions()) {
                if (tx.getDst().equals(address)) balance += tx.getFunds();
                if (tx.getSrc().equals(address)) balance -= tx.getFunds();
            }
        }
        return balance;
    }

    // SOLO PER IL BOOTSTRAP INIZIALE DEI FONDI — bypassa il controllo di saldo 
    // (che non avrebbe senso per l'iniezione di fondi nel sistema), 
    // ma mantiene comunque la validazione di firma/binding
    public boolean injectGenesisFunds(Transaction t) {
        if (!t.isValid()) return false; // firma e binding restano obbligatori
        mempool.add(t);
        return true;
    }

    public Block mineBlock() {
        if (mempool.isEmpty()) {
            return null;
        }
        byte[] prevHash = this.getLastBlock().computeBlockHash();
        Block newBlock = new Block(mempool, prevHash, Instant.now());
        newBlock.mine(DIFFICULTY);
        this.ledger.add(newBlock);
        mempool.clear();
        System.out.println("[BLOCKCHAIN]: A new Block has been mined");
        return newBlock;
    }

    public boolean isGenesisValid() {
        Block genesis = ledger.get(0);
        boolean pointerIntegrity = Arrays.equals(genesis.getHashPointer(), GENESIS_PREV_HASH);
        boolean payloadIntegrity = HexFormat.of().formatHex(genesis.computeBlockHash()).equals(EXPECTED_GENESIS_HASH_HEX);
        return pointerIntegrity && payloadIntegrity;
    }

    public boolean checkLedgerIntegrity() {
        if (ledger.isEmpty() || !isGenesisValid()) {
            return false;
        }
        // Genesis block excluded
        for (int i = 1; i < this.ledger.size(); i++) {
            Block current = this.ledger.get(i);
            Block previous = this.ledger.get(i-1);
            if (!Arrays.equals(current.getHashPointer(), previous.computeBlockHash())) return false;
            // Asks the block the rule used when it was mined (useful if the rule changes during the blockchain lifecycle)
            if (!current.verifyProofOfWork(current.computeBlockHash(), current.getDifficulty())) return false;
        }
        return true;
    }

    public void printChainStatus() {
        System.out.println("\n=== Blockchain status ===");
        System.out.println("Chain length: " + ledger.size() + " blocks");
        System.out.println("Pending transactions in mempool: " + mempool.size());
        System.out.println("Ledger integrity: " + checkLedgerIntegrity());
        System.out.println("--------------------------");
        for (int i = 0; i < ledger.size(); i++) {
            Block b = ledger.get(i);
            String hashPreview = HexFormat.of().formatHex(b.computeBlockHash()).substring(0, 8);
            System.out.println("Block " + i + " | hash: " + hashPreview + "... | transactions: " + b.getTransactions().size());
        }
        System.out.println("==========================\n");
    }

}
