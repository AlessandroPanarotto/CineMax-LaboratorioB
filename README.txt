====================================================================
CineMax - Laboratorio Interdisciplinare B (a.a. 2025/2026)
Universita' degli Studi dell'Insubria
====================================================================

Questo file spiega come installare, compilare ed eseguire il progetto
CineMax (modulo serverCM + modulo clientCM).


--------------------------------------------------------------------
1. REQUISITI
--------------------------------------------------------------------

- Java JDK 17 o superiore
- Apache Maven 3.8 o superiore (https://maven.apache.org/)
- PostgreSQL 14 o superiore, con estensione "pgcrypto" installabile
  (di solito gia' presente nel pacchetto postgresql-contrib)

Tutte le altre librerie necessarie (in particolare il driver JDBC di
PostgreSQL) sono dichiarate nel file pom.xml e vengono scaricate in
automatico da Maven al momento della compilazione: non serve scaricare
nulla a mano.

L'unica libreria "non standard" usata dal progetto e' quindi il driver
JDBC ufficiale di PostgreSQL (org.postgresql:postgresql), dichiarato
come dipendenza Maven in pom.xml. Lato database viene inoltre usata
l'estensione pgcrypto di PostgreSQL (per le funzioni crypt()/gen_salt()
usate per cifrare le password): va abilitata una sola volta nel
database con il comando CREATE EXTENSION, che e' gia' incluso in testa
allo script di creazione dello schema (vedi punto 2).


--------------------------------------------------------------------
2. CREAZIONE DEL DATABASE (dbCM)
--------------------------------------------------------------------

Prima di avviare il server e' necessario creare il database e le sue
tabelle. Con PostgreSQL gia' installato e in esecuzione:

    1) Creare un database vuoto chiamato dbCM, ad esempio:
           createdb -U <utente_postgres> dbCM

    2) Eseguire lo script che crea tabelle, vincoli e trigger:
           psql -h <host> -U <utente_postgres> -d dbCM \
                -f src/main/resources/db/schema_cinemax.sql

       Questo script crea anche automaticamente 2 utenti proiezionisti
       e 5 utenti bigliettai gia' pronti per il login (le credenziali
       di prova sono elencate in fondo allo script stesso).

    3) Per caricare i dati di partenza forniti dal docente (file
       src/main/resources/db/proiezioni.csv: 725 film e 8878
       proiezioni), lanciare psql dalla cartella principale del
       progetto (il percorso del CSV nello script e' relativo a
       quella cartella) ed eseguire:
           psql -h <host> -U <utente_postgres> -d dbCM \
                -f src/main/resources/db/import_proiezioni.sql

    4) (Facoltativo, solo per test) Per avere anche qualche film,
       proiezione e prenotazione di esempio aggiuntivi:
           psql -h <host> -U <utente_postgres> -d dbCM \
                -f src/main/resources/db/dati_esempio.sql

Non e' richiesta alcuna configurazione aggiuntiva: le credenziali e
l'host del database vengono chiesti a runtime quando si avvia serverCM
(vedi punto 4), non sono scritti in nessun file di configurazione.


--------------------------------------------------------------------
3. COMPILAZIONE (con Maven)
--------------------------------------------------------------------

Dalla cartella principale del progetto (dove si trova questo file e
pom.xml), lanciare:

    mvn clean package

Questo comando:
  - compila tutte le classi Java del progetto (src/main/java);
  - esegue il comando "package" del ciclo di vita Maven, che grazie al
    plugin maven-assembly-plugin configurato in pom.xml genera nella
    cartella bin/ (creata automaticamente) i due archivi eseguibili
    richiesti dalla consegna:
        bin/serverCM.jar   -> contiene anche il driver JDBC di
                               PostgreSQL, perche' e' l'unico modulo
                               che parla direttamente col database
        bin/clientCM.jar   -> non contiene il driver JDBC (il client
                               comunica solo via RMI col server, non
                               gli serve)

Per generare anche la documentazione Javadoc (salvata in
target/site/apidocs) si puo' lanciare separatamente:

    mvn javadoc:javadoc

Una copia della Javadoc generata viene inclusa anche nella cartella
doc/javadoc di questo repository.


--------------------------------------------------------------------
4. AVVIO DEL SERVER (serverCM)
--------------------------------------------------------------------

Dalla cartella principale del progetto (o da bin/, se si preferisce
copiare li' il jar):

    java -jar bin/serverCM.jar

Il programma chiede in modo interattivo, da riga di comando:
  - host del database dbCM (default: localhost)
  - porta di PostgreSQL (default: 5432)
  - nome del database (default: dbCM)
  - utente PostgreSQL
  - password PostgreSQL (non viene mostrata a schermo, se il terminale
    lo consente)
  - porta su cui pubblicare i servizi RMI (default: 1099)

Una volta connesso al database, il server resta in ascolto e puo'
gestire piu' client contemporaneamente, finche' non viene interrotto
(es. con Ctrl+C).


--------------------------------------------------------------------
5. AVVIO DEL CLIENT (clientCM)
--------------------------------------------------------------------

Su ogni postazione che deve collegarsi al server (anche computer
diversi dalla stessa rete), lanciare:

    java -jar bin/clientCM.jar

All'avvio viene chiesto (tramite una finestra di dialogo) l'indirizzo
e la porta RMI del server a cui collegarsi. Si possono aprire piu'
istanze del client in parallelo (anche sulla stessa macchina, utile
per fare delle prove), ognuna si comporta come un utente indipendente.


--------------------------------------------------------------------
6. STRUTTURA DEL REPOSITORY
--------------------------------------------------------------------

    autori.txt   - dati anagrafici degli autori del progetto
    README.txt   - questo file
    pom.xml      - file di build Maven
    src/         - codice sorgente Java (package cinemax) e script SQL
    bin/         - jar eseguibili generati con "mvn clean package"
    doc/         - documentazione di progetto: manuale utente e
                   manuale tecnico (PDF), diagrammi ER e UML, Javadoc
    lib/         - eventuali librerie esterne non gestite da Maven
                   (attualmente non necessaria: tutte le dipendenze
                   sono gestite automaticamente da Maven tramite
                   pom.xml)
