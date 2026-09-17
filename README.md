# Simulazione Didattica di Blockchain ed Elaborazione di Attacchi di Tampering

## 0. Compilazione ed esecuzione
I comandi seguenti vanno eseguiti dalla **root** del progetto (la cartella principale).

### Compilazione
Compila i sorgenti presenti in `src/` salvando i file `.class` generati nella cartella `bin/`:
```bash
javac -d bin src/*.java
```

### Esecuzione
```bash
java -cp bin Demo
```

## 1. Introduzione e obiettivi
- **Scopo del progetto**: Il progetto ha un intento didattico e si propone di dimostrare in modo chiaro e isolato le proprietà crittografiche, di autenticazione e di immutabilità strutturale di una blockchain. L'obiettivo primario è la simulazione e rilevazione di attacchi di manomissione (tampering) dello stato e la validazione delle transazioni.
- **Vincolo tecnico**: Il progetto utilizza esclusivamente le API standard di Java Cryptography Architecture (JCA) (pacchetti `java.security.*`) per la generazione delle chiavi (`KeyPairGenerator`, curva EC `secp256r1`), il calcolo degli hash (`MessageDigest`, SHA-256) e la firma/verifica digitale (`Signature`, `SHA256withECDSA`), senza dipendenze crittografiche esterne.
- **Panoramica architetturale**: Il sistema è basato su un modello *Account-based*. È composto da tre macro-componenti interconnesse:
  1. **Wallet & Transaction**: Gestiscono la generazione delle coppie di chiavi asimmetriche (ECDSA su curva `secp256r1`), la derivazione dell'indirizzo tramite hash SHA-256 della chiave pubblica, la creazione delle transazioni e la loro firma digitale.
  2. **Block & Merkle Tree**: Organizzano le transazioni in una struttura a blocchi composta da Header e Payload. L'integrità del payload è garantita dal calcolo di un Merkle Tree di hash SHA-256, il cui valore *Merkle Root* viene incorporato direttamente nell'header.
  3. **Blockchain & Proof of Work (PoW) Toy**: Gestisce la validazione e l'accettazione delle transazioni nel `mempool` (verificando saldi e firme), il mantenimento del registro (`ledger`), l'aggancio cronologico dei blocchi tramite *hash pointer* e il meccanismo di mining basato su un puzzle di Proof of Work a difficoltà fissa.

---

## 2. Scelte di semplificazione
Il progetto privilegia la dimostrazione chiara delle proprietà crittografiche rispetto alla replica di un sistema di produzione. Alcuni meccanismi tipici di implementazioni reali sono stati volutamente omessi o semplificati:

- **Difficulty fissa**: La difficoltà di mining è una costante globale (`DIFFICULTY = 4`), che impone un prefisso di 4 zeri esadecimali all'hash del blocco. Non è presente un algoritmo dinamico di ri-calibrazione basato sul tempo medio di generazione. Il valore viene comunque salvato come campo del blocco stesso al momento del mining (anziché essere letto da una costante esterna al momento della validazione), scelta che renderebbe la struttura pronta a supportare una difficulty variabile in un'eventuale estensione futura.
- **Assenza di rete P2P**: Il sistema modella un nodo singolo centralizzato. Non esistono propagazione di blocchi, protocolli di gossip, gestione o risoluzione dei fork, né competizione tra miner. Il mining è un'operazione esplicita scatenata sequenzialmente dall'orchestratore.
- **Assenza di limite alla dimensione del blocco**: Non esiste un vincolo sulla dimensione massima in byte o sul numero di transazioni per blocco. Durante l'operazione di mining, tutte le transazioni correntemente presenti nel `mempool` vengono prelevate e svuotate all'interno del nuovo blocco.
- **Genesis block deterministico e non minato**: Il blocco Genesis (indice 0 nel `ledger`) è generato con valori hardcoded (timestamp fisso `1710000000000L`, nessun payload/transazione, `hashPointer` a 32 byte azzerati) per permettere il "congelamento" statico del suo hash nell'applicazione come *Trust Anchor*, ed è escluso dal meccanismo di Proof of Work.
- **Assenza di Coinbase Transaction e dati di Witness**: Non è stato implementato un meccanismo di ricompensa/emissione di nuove monete per il miner, né la separazione tra dati di firma e transazione (stile SegWit).
- **Modello Account-based anziché UTXO**: Il sistema risale al saldo dei wallet calcolando la differenza tra i flussi in ingresso e in uscita sul registro storico, invece di tracciare transazioni non spese (UTXO).

---

## 3. Componenti del sistema

### 3.1 Transaction (`Transaction.java`)
Rappresenta il trasferimento di fondi da un indirizzo sorgente (`src`) a un indirizzo destinazione (`dst`).
- **Struttura dei dati**:
  - `src`, `dst` (String): Indirizzi esadecimali dei wallet coinvolti.
  - `funds` (double): Importo trasferito.
  - `senderPubKey` (PublicKey): Chiave pubblica ECDSA del mittente.
  - `timestamp` (long): Timestamp di creazione della transazione (utilizzato per prevenire attacchi di *replay*).
  - `sign` (byte[]): Firma digitale ECDSA applicata al payload serializzato.
- **Metodi principali**:
  - `getTransactionDataBytes()`: Serializza in un `ByteBuffer` i dati fondamentali della transazione (mittente, destinatario, importo, timestamp).
  - `verifySignature()`: Ricostruisce il buffer dei dati e usa `senderPubKey` tramite l'algoritmo `SHA256withECDSA` per verificare la validità della firma `sign`.
  - `verifyAddressBinding()`: Ricalcola l'indirizzo teorico associato a `senderPubKey` e verifica che sia perfettamente identico alla stringa `src`.
  - `isValid()`: Esegue congiuntamente `verifyAddressBinding()` e `verifySignature()`.
  - `getHash()`: Genera l'hash SHA-256 univoco della transazione includendo sia il payload dei dati sia la firma digitale (utilizzato per il calcolo delle foglie del Merkle Tree).
  - `tamperFunds(double newAmount)`: Metodo ad uso esclusivo di test per simulare l'alterazione fraudolenta dei fondi e testare la rilevazione della manomissione.

### 3.2 Wallet (`Wallet.java`)
Rappresenta l'entità utente in grado di gestire chiavi crittografiche ed emettere transazioni.
- **Struttura dei dati**:
  - `privKey` e `pubKey`: Coppia di chiavi generate con l'algoritmo ECDSA sulla curva ellittica `secp256r1`.
  - `address`: Indirizzo univoco generato applicando l'hash SHA-256 alla chiave pubblica codificata.
  - `walletName`: Nome utente assegnato a scopi identificativi nel log di esecuzione.
- **Metodi principali**:
  - `deriveAddress(PublicKey pubKey)` (static): Esegue l'hash SHA-256 sulla codifica byte della chiave pubblica fornita e ne restituisce la rappresentazione esadecimale.
  - `signTransaction(Transaction t)`: Genera la firma digitale del buffer di `t` utilizzando la chiave privata dell'utente (`privKey`).
  - `transferMoney(String destination, double amount)`: Crea e firma una nuova istanza di `Transaction`.
  - `printBalance(Blockchain b)`: Riceve come argomento un riferimento alla blockchain da interrogare e stampa a schermo il saldo corrente dell'indirizzo, seguendo un modello di interrogazione *on-demand* (analogo a wallet reali come MetaMask, che non sono legati in modo permanente a una singola rete).

### 3.3 Block (`Block.java`)
Rappresenta la struttura dati del blocco composta da Header e Payload.
- **Struttura dei dati**:
  - **Header**:
    - `version` (int): Versione del formato (fissata a `1`).
    - `timestamp` (Instant): Momento di creazione del blocco.
    - `hashPointer` (byte[]): Hash SHA-256 dell'header del blocco precedente.
    - `nonce` (long): Contatore modificato sequenzialmente durante il mining.
    - `difficulty` (int): Valore di difficoltà con cui il blocco è stato effettivamente minato (vedi Sezione 2 per la scelta di difficulty fissa a livello di sistema).
  - **Payload**:
    - `transactions` (List<Transaction>): Elenco delle transazioni incluse nel blocco.
- **Metodi principali**:
  - `calculateMerkleRoot()` & `processMerkleLevel()`: Costruiscono ricorsivamente il Merkle Tree prendendo gli hash delle transazioni (`t.getHash()`), duplicando l'ultimo hash in caso di livello dispari, e concatenando/hashando i nodi a coppie fino ad ottenere un singolo hash radice di 32 byte.
  - `computeBlockHash()`: Serializza l'header e ne restituisce l'hash SHA-256. Il *Merkle Root* **non è cachato** in un campo, ma ricalcolato ad ogni chiamata a partire dal contenuto attuale di `transactions`: questa scelta è essenziale per la rilevazione del tampering, poiché un Merkle Root salvato una sola volta alla creazione del blocco non rifletterebbe eventuali alterazioni successive ai dati delle transazioni, rendendo `checkLedgerIntegrity()` inefficace.
  - `mine(int difficulty)`: Esegue il puzzle PoW incrementando il `nonce` fino a quando `computeBlockHash()` non produce un valore la cui rappresentazione esadecimale inizia con il numero richiesto di zeri.
  - `verifyProofOfWork(byte[] hash, int difficulty)`: Controlla se l'hash fornito rispetta il vincolo della difficoltà specificata.

### 3.4 Blockchain (`Blockchain.java`)
Rappresenta il registro e l'orchestratore dello stato del sistema.
- **Struttura dei dati**:
  - `ledger` (List<Block>): Registro cronologico dei blocchi validati (indice 0 = Genesis).
  - `mempool` (List<Transaction>): Area di stazionamento per le transazioni in attesa di essere minate.
  - `GENESIS_PREV_HASH`: Vettore di 32 byte azzerati usato come puntatore precedente per il blocco Genesis.
  - `EXPECTED_GENESIS_HASH_HEX`: Costante contenente l'hash noto dell'header del blocco Genesis ("`2f55162e9ac7e9e584677aeebbfc9f63aef043ad51a151cc633a3a1108f21488`").
  - `DIFFICULTY`: Costante di difficoltà PoW (valore fisso: `4`).
- **Metodi principali**:
  - `submitTransaction(Transaction t)`: Esegue le verifiche di validità crittografica su `t` (`isValid()`) e controlla che il saldo disponibile del mittente sia sufficiente, dove per saldo disponibile si intende il saldo confermato nel `ledger` **al netto delle uscite già presenti nel `mempool` per lo stesso indirizzo** (metodo `getPendingOutflow()`). Questo previene la doppia spesa all'interno dello stesso mempool: due transazioni consecutive dallo stesso mittente che, sommate, eccederebbero il saldo disponibile non possono essere entrambe accettate.
  - `injectGenesisFunds(Transaction t)`: Metodo speciale per l'inizializzazione del sistema: valida firma e binding crittografico ma bypassa il controllo sul saldo disponibile (che risulterebbe zero al primo avvio).
  - `mineBlock()`: Preleva tutte le transazioni dal `mempool`, crea un nuovo blocco collegandolo a `getLastBlock()`, esegue il mining tramite PoW, lo aggiunge al `ledger` e svuota il `mempool`.
  - `getBalance(String address)`: Calcola il saldo corrente scansionando l'intero registro e calcolando la somma algebrica di accreditamenti (`dst`) e addebiti (`src`).
  - `isGenesisValid()`: Verifica l'integrità del blocco Genesis confrontando sia il puntatore precedente azzerato sia l'hash risultante dell'header con la costante congelata (`EXPECTED_GENESIS_HASH_HEX`).
  - `checkLedgerIntegrity()`: Valida l'intera catena verificando la correttezza del blocco Genesis, la continuità dei collegamenti tra blocchi (`current.hashPointer == hash(previous)`) e il rispetto della regola PoW salvata su ciascun blocco.

---

## 4. Proprietà di sicurezza dimostrate
- **Binding Indirizzo / Chiave Pubblica**: Dimostrato tramite `verifyAddressBinding()`. Un attaccante non può inviare denaro dichiarando un indirizzo sorgente non suo, anche se firma il messaggio con la propria chiave privata, in quanto l'hash della chiave fornita non corrisponderà all'indirizzo `src` specificato.
- **Autenticità della Firma**: Dimostrata tramite `verifySignature()`. Impossibile modificare l'importo, i partecipanti o il timestamp senza invalidare la firma digitale calcolata con ECDSA sulla chiave privata del legittimo mittente.
- **Prevenzione della doppia spesa nel mempool**: Dimostrata tramite `getPendingOutflow()`, integrato nel controllo di `submitTransaction()`. Transazioni pendenti (non ancora minate) dallo stesso mittente vengono conteggiate come uscite già impegnate, impedendo che il saldo confermato venga "promesso" più volte prima della conferma.
- **Integrità del Blocco e del Payload**: Dimostrata dall'inserimento del *Merkle Root* nell'header e dall'hashing dell'header stesso. Qualsiasi modifica anche a una singola transazione (o al suo importo) ne varia l'hash, propagandosi fino alla radice dell'albero e cambiando l'hash finale del blocco.
- **Hash Chaining e Immutabilità del Registro**: Dimostrata tramite `checkLedgerIntegrity()`. Se un blocco passato viene alterato, il suo hash cambia. Di conseguenza, il campo `hashPointer` del blocco successivo diventa non valido, rompendo l'intera catena a cascata.
- **Proof of Work (PoW) come Vincolo Computazionale**: Dimostrata da `verifyProofOfWork()`. Impedisce la ri-scrittura arbitraria della storia senza che l'attaccante ri-esegua il lavoro di mining (ricerca del `nonce`) per il blocco manomesso e per tutti i blocchi successivi.
- **Trust Anchor del Genesis Block**: Dimostrata tramite `isGenesisValid()`. Congelare l'hash atteso del blocco 0 nel codice sorgente impedisce attacchi in cui un malintenzionato tenta di sostituire la radice dell'intera blockchain.

---

## 5. Simulazione dimostrativa (Demo)
- **Setup e Inizializzazione**:
  - Inizializzazione della Blockchain e creazione dei Wallet: `SYSTEM`, `Alice`, `Bob`, `Carol`.
  - Iniezione dei fondi iniziali via `injectGenesisFunds()` da `SYSTEM` ad Alice (1000.0 unità) e mining del blocco 1.
  - Esecuzione e mining di transazioni legittime: Alice trasferisce 500.0 a Bob (blocco 2); Bob trasferisce 75.0 a Carol (blocco 3).
  - Stampa dello stato iniziale della catena (`printChainStatus()`) e dei saldi correnti: Alice 500.0, Bob 425.0, Carol 75.0.
- **TEST 01: Tentativo di Transazione Fraudolenta (Falsificazione Mittente)**:
  - Creazione del wallet malevolo `Mallory`.
  - Mallory tenta di spendere 300.0 dall'indirizzo di Alice verso se stessa, firmando la transazione con la propria chiave privata.
  - **Esito atteso e osservato**: La blockchain rifiuta l'inserimento nel `mempool` poiché `verifyAddressBinding()` fallisce (l'indirizzo sorgente appartiene ad Alice, ma la chiave pubblica fornita è di Mallory), output: `REJECTED - binding FAILED: the senderAddress does not match the provided pubKey`.
- **TEST 02: Simulazione di Attacco di Manomissione dello Stato (Tampering)**:
  - Controllo preventivo dell'integrità del registro: `checkLedgerIntegrity()` restituisce `true`.
  - Accesso diretto al blocco 2 del `ledger` (quello contenente la transazione Alice → Bob) e alterazione del relativo importo da 500.0 a 99999.0 tramite `victimTx.tamperFunds(99999.0)`, un metodo esposto esclusivamente a fini dimostrativi, per bypassare l'assenza di setter pubblici sui dati di una transazione confermata.
  - **Esito osservato**: il saldo di Alice, ricalcolato scansionando il registro alterato, passa da `500.0` a `-98999.0`, rendendo tangibile l'effetto distorsivo della manomissione sulla contabilità. Contestualmente, `checkLedgerIntegrity()` passa da `true` a `false`, poiché il nuovo Merkle Root (ricalcolato al volo, si veda Sezione 3.3) produce un `computeBlockHash()` del blocco 2 diverso da quello atteso dal `hashPointer` del blocco 3, rompendo la catena.

---

## 6. Limiti noti e possibili estensioni
- **Assenza di Persistenza e Distribuzione P2P**: Il registro risiede completamente in memoria volatile (RAM). Una naturale estensione include l'implementazione della serializzazione su disco e l'introduzione di socket di rete P2P per consentire la sincronizzazione tra nodi differenti.
- **Tamper-Evident vs Tamper-Proof nel Trust Anchor**: Come indicato in Sezione 2, la costante `EXPECTED_GENESIS_HASH_HEX` rende il Genesis Block *tamper-evident* a runtime (rileva l'alterazione dello stato in memoria), ma non *tamper-proof* contro un attaccante con accesso diretto al codice sorgente, che potrebbe modificare anche la costante stessa.
- **Assenza di ripristino automatico**: Il sistema attualmente *rileva* l'avvenuta manomissione invalidando la catena (`checkLedgerIntegrity()` → `false`), ma non implementa algoritmi di rollback automatico allo stato valido precedente né meccanismi di recovery.
- **Mining sequenziale e non concorrente**: Il mining è un'operazione bloccante ed esplicita, innescata dall'orchestratore (si veda Sezione 2). Un'estensione realistica prevederebbe un thread di mining indipendente, con le relative problematiche di sincronizzazione sull'accesso concorrente al `mempool`.
