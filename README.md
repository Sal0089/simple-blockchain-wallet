## NOTE SULL'IMPLEMENTAZIONE
1. Per quanto riguarda la classe Block: ci si è ispirati alla struttura di un blocco della blockchain di Bitcoin e si è scelto pertanto di incorporare nel singolo blocco anche un timestamp, che viene generato in modo deterministico per il blocco di genesi (così da poter hardcodare il suo hash) (1710000000000L è il numero di epoche da 1 gennaio 1970)

2. Nella blockchain non si è imposto un limite di transazioni per blocco, ogni volta che viene minato un blocco si inseriscono al suo interno tutte le transazioni presenti nel mempool fino a quell'istante

3. Scelta di implementazioni di meccanismo di PoW toy. Si è scelto di settare una difficoltà espressa come numero di zeri necessari per risolvere il puzzle, senza quindi determinare un valore di target per ogni blocco. Per semplicità, si è scelto di omettere il meccanismo di retroazione che rende dinamica la difficoltà.
Si è scelto di fare in modo di tenere traccia per ogni blocco della regola sulla base della quale è avvenuto il suo mining, così da mantenere il tutto robusto a fronte di cambiamenti di difficoltà nel corso del ciclo di vita della blockchain. (Solo a fini dimostrativi, anche perché non è stata implementata retroazione)

4. Controllo di integrità del genesis block: si è scelto di verificare, oltre al 
   campo hashPointer (fissato a 32 byte a zero, per convenzione), anche l'hash 
   completo dell'header del genesis block, confrontandolo con un valore noto e 
   "congelato" nel codice (EXPECTED_GENESIS_HASH_HEX). Questo rende il genesis 
   un trust anchor verificabile: qualsiasi alterazione del suo contenuto (es. 
   timestamp, transazioni incluse) produce un hash diverso da quello atteso, 
   rendendo la manomissione rilevabile.

   Il valore hardcoded è stato calcolato una tantum a partire da un genesis 
   block con timestamp fisso (1710000000000L) e prevHash pari a un vettore di 
   32 byte a zero — scelto deterministico proprio per permettere questo tipo 
   di verifica statica.

   Nota: questo meccanismo è tamper-evident (rileva la manomissione), non 
   tamper-proof (non la impedisce) — un attaccante con accesso diretto al 
   codice sorgente potrebbe comunque modificare anche la costante hardcoded 
   stessa. Il controllo protegge principalmente da alterazioni dello stato 
   a runtime (es. caricamento di dati corrotti o manomessi), non da un 
   attaccante che controlla il codice compilato.

5. Il metodo printBalance riceve la blockchain come parametro anziché mantenerne un riferimento interno, rispecchiando il comportamento dei wallet reali (es. MetaMask), che non sono legati in modo permanente a una singola rete ma interrogano lo stato on-demand alla rete correntemente selezionata.

1000. DESCRIZIONE-SIMULAZIONE: Si sceglie di creare una blockchain e 3 wallet. L'obiettivo è di simulare un attacco di tampering