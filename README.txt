CineMax - Laboratorio Interdisciplinare B (a.a. 2025/2026)
============================================================

Stato del progetto: PROGETTAZIONE DEL DATABASE COMPLETATA.
Progettazione UML e sviluppo di serverCM/clientCM: da fare.

Autori: vedi autori.txt

------------------------------------------------------------------
STRUTTURA DEL REPOSITORY
------------------------------------------------------------------
- autori.txt                 elenco autori del progetto
- doc/                        documentazione di progetto (manuale utente,
                               manuale tecnico, diagrammi ER/UML, javadoc)
- src/main/java/cinemax/      codice sorgente Java (package cinemax) - da scrivere
- src/main/resources/db/      script SQL di creazione del database PostgreSQL
                               (schema_cinemax.sql) e dati di esempio
- bin/                        eseguibili .jar di serverCM e clientCM (da generare)
- lib/                        eventuali librerie esterne non gestite da Maven
- pom.xml                     build Maven del progetto

------------------------------------------------------------------
DATABASE (gia' pronto)
------------------------------------------------------------------
Requisiti: PostgreSQL (testato su v16), estensione pgcrypto (abilitata
automaticamente dallo script).

    createdb "dbCM"
    psql -d dbCM -f src/main/resources/db/schema_cinemax.sql

Dettagli di progettazione (analisi requisiti, schema E-R concettuale e
ristrutturato, traduzione relazionale, motivazioni) in
doc/01_progettazione_database.md.

------------------------------------------------------------------
COMPILAZIONE (Maven) - da completare insieme al codice sorgente
------------------------------------------------------------------
Requisiti: JDK 17+, Maven 3.9+.

    mvn clean compile        # compila il codice sorgente
    mvn javadoc:javadoc      # genera la javadoc in target/site/apidocs
    mvn package               # (da configurare) produrra' i due eseguibili
                               # bin/serverCM.jar e bin/clientCM.jar

Questa sezione verra' aggiornata man mano che il codice di serverCM e
clientCM viene implementato.

------------------------------------------------------------------
ESECUZIONE (da definire)
------------------------------------------------------------------
Al lancio, serverCM richiedera' le credenziali di accesso a dbCM e
l'host del DB, restando poi in attesa di connessioni da clientCM.
Istruzioni dettagliate seguiranno con l'implementazione.
