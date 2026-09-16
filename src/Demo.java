
public class Demo {
    public static void main(String[] args) {
        // INIT: Blockchain init
        Blockchain blockchain = new Blockchain();

        // Wallet definitions
        Wallet systemWallet = new Wallet();
        Wallet aliceWallet = new Wallet();
        Wallet bobWallet = new Wallet();    
        Wallet carolWallet = new Wallet();
        systemWallet.setName("SYSTEM");
        aliceWallet.setName("Alice");
        bobWallet.setName("Bob");
        carolWallet.setName("Carol");

        // GENESIS TRANSACTION: Creates a genesis transaction towards Alice 
        Transaction genesisFunding = new Transaction(systemWallet.getAddress(), aliceWallet.getAddress(), 1000.0, systemWallet.getPublicKey());
        // systemWallet signs the transaction
        genesisFunding.setSignature(systemWallet.signTransaction(genesisFunding));
        // Using injectGenesisFunds() bypasses submitTransaction's balance check
        boolean injected = blockchain.injectGenesisFunds(genesisFunding);
        System.out.println("[SYSTEM] Injected funds (" + injected + ")"); 
        blockchain.mineBlock(); 

        // ALLOWED TRANSACTIONS: 
        // Transaction between Alice and Bob
        Transaction tx1 = aliceWallet.transferMoney(bobWallet.getAddress(), 500.0);
        boolean accepted1 = blockchain.submitTransaction(tx1);
        System.out.println("[TX] Alice -> Bob (500.0): " + (accepted1 ? "accepted into mempool" : "REJECTED"));
        blockchain.mineBlock();
    
        // Transaction between Bob and Carol
        Transaction tx2 = bobWallet.transferMoney(carolWallet.getAddress(), 75.0);
        boolean accepted2 = blockchain.submitTransaction(tx2);
        System.out.println("[TX] Bob -> Carol (75.0): " + (accepted2 ? "accepted into mempool" : "REJECTED"));
        blockchain.mineBlock();
        
        // RESULTING BLOCKCHAIN STATUS:
        // Shows blockchain status
        blockchain.printChainStatus();

        // Prints current Balances
        System.out.println("==== Current balances ==== ");
        aliceWallet.printBalance(blockchain);
        bobWallet.printBalance(blockchain); 
        carolWallet.printBalance(blockchain); 
        System.out.println("========================== ");

        // TEST 01: Fraudulent transaction
        System.out.println("\nTEST_01:\nFRAUDULENT TRANSACTION ATTEMPT: ");
        // Malicious user
        Wallet malloryWallet = new Wallet();
        malloryWallet.setName("Mallory");

        // Mallory creates a transaction claiming Alice as sender, but signs it with her own key pair
        Transaction fraudTx = new Transaction(aliceWallet.getAddress(), malloryWallet.getAddress(), 300.0, malloryWallet.getPublicKey());
        fraudTx.setSignature(malloryWallet.signTransaction(fraudTx));

        boolean fraudAccepted = blockchain.submitTransaction(fraudTx);
        System.out.println("[SECURITY] Detected an attempt by Mallory to spend from Alice's address: " + (fraudAccepted ? "ACCEPTED — VULNERABILITY!" : "REJECTED — binding FAILED: the senderAddress does not match the provided pubKey"));

        // TEST 02: tampering attack
        System.out.println("\nTEST_02: SIMULATION OF A TAMPERING ATTACK:");
        System.out.println("[BLOCKCHAIN-STATUS] Integrity before tampering: " + blockchain.checkLedgerIntegrity());
        
        Block targetBlock = blockchain.getLedger().get(2); // Takes a block that contains a transaction between users
        Transaction victimTx = targetBlock.getTransactions().get(0);
        System.out.println("[BALANCE] Alice's balance before tampering: " + blockchain.getBalance(aliceWallet.getAddress()));
        victimTx.tamperFunds(99999.0); // Uses a method exposed only for demonstration purposes to simulate a tampering attack
        System.out.println("[BALANCE] Alice's balance after tampering: " + blockchain.getBalance(aliceWallet.getAddress()));
        System.out.println("[BLOCKCHAIN-STATUS] Integrity after tampering: " + blockchain.checkLedgerIntegrity());
    }   

    
}
