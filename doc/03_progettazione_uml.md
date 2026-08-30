# Progettazione UML dell'applicazione CineMax

Laboratorio Interdisciplinare B — a.a. 2025/2026

Questo documento descrive la progettazione software dell'applicazione CineMax:
architettura client/server, package, design pattern adottati e i diagrammi UML
richiesti (casi d'uso, classi, sequenza). E' il seguito naturale di
[`01_progettazione_database.md`](01_progettazione_database.md) (progettazione
della base di dati) e di [`02_query_servizi.md`](02_query_servizi.md) (query SQL
a supporto dei servizi applicativi).

## 1. Architettura generale

La specifica richiede un'applicazione **client/server**, con due archivi jar
distinti (`serverCM` e `clientCM`) e "servizi erogati da interfacce di
programmazione", utilizzabile da più client contemporaneamente. Si è scelto di
realizzare la comunicazione client/server con **Java RMI (Remote Method
Invocation)**:

- il server pubblica un insieme di oggetti remoti (le "interfacce di
  programmazione" richieste dalla traccia) su un RMI registry;
- ogni client, dopo il lookup, ottiene uno **stub** locale che implementa la
  stessa interfaccia e inoltra le chiamate al server in modo trasparente;
- più client possono connettersi in concorrenza allo stesso server, che
  gestisce l'accesso condiviso al database (si veda il requisito di
  concorrenza discusso in `01_progettazione_database.md`).

Il codice condiviso fra client e server (le interfacce remote e le classi di
dominio serializzabili) è isolato in un package comune, compilato in entrambi
i jar.

```
cinemax
 ├── common      (interfacce Remote + classi di dominio, condiviso)
 ├── server      (implementazione dei servizi, DAO, accesso al DB, main server)
 └── client      (interfaccia utente, controller, main client)
```

Questa separazione rispecchia i due archivi richiesti dalla consegna:
`serverCM` include `cinemax.common` + `cinemax.server`; `clientCM` include
`cinemax.common` + `cinemax.client`.

## 2. Design pattern adottati

| Pattern | Dove | Motivazione |
|---|---|---|
| **DAO** (Data Access Object) | `cinemax.server.dao` (`FilmDAO`, `UtenteDAO`, `ProiezioneDAO`, `PrenotazioneDAO` + implementazioni `*Postgres`) | Isola le query SQL (già progettate in `02_query_servizi.md`) dalla logica di servizio: i `Service` non conoscono JDBC né SQL, e l'accesso ai dati potrebbe essere sostituito senza modificare la logica applicativa. |
| **Singleton** | `ConnectionManager` | Punto unico di accesso al pool di connessioni verso PostgreSQL lato server, condiviso da tutti i DAO: evita connessioni ridondanti e centralizza la gestione della concorrenza multi-client richiesta dalla traccia. |
| **Factory Method** | `UtenteFactory` | La tabella `utenti` è unica con colonna discriminante `ruolo` (scelta di ristrutturazione, §3.1 di `01_progettazione_database.md`); la factory legge il ruolo restituito dalla query e istanzia la sottoclasse Java corretta (`Cliente`, `Proiezionista`, `Bigliettaio`), facendo da ponte fra tabella unica e gerarchia OOP. |
| **Facade** | Le interfacce remote `IAutenticazioneService`, `IProiezioneService`, `IPrenotazioneService` | Espongono al client solo le operazioni ad alto livello richieste dai casi d'uso (login, ricerca proiezioni, prenotazione, ...), nascondendo DAO e logica interna del server dietro un'interfaccia semplice. |
| **Proxy** | Stub generato da Java RMI | Intrinseco al meccanismo RMI: il client invoca metodi su un oggetto locale (lo stub) che si comporta da proxy e inoltra la chiamata all'oggetto remoto reale sul server. |
| **MVC** | `cinemax.client.view` / `cinemax.client.controller` + classi di dominio come model | Separa l'interfaccia grafica (view) dalla logica di interazione (controller), che a sua volta invoca i servizi remoti; il model è costituito dalle classi di dominio condivise (`Film`, `Proiezione`, `Prenotazione`, ...). |

## 3. Diagramma dei casi d'uso

![Diagramma dei casi d'uso](uml/casi_uso.png)

Gli attori sono derivati dai ruoli previsti dalla traccia: un utente non
registrato (guest) può cercare/visualizzare proiezioni e registrarsi; un
utente registrato si specializza in **Cliente** (prenotazioni), **Proiezionista**
(gestione proiezioni) e **Bigliettaio** (consultazione/ricerca prenotazioni in
biglietteria). I vincoli applicativi non esprimibili graficamente (es. una
proiezione modificabile solo se non ha prenotazioni associate) sono annotati
come note sui singoli casi d'uso, e corrispondono ai trigger già implementati
lato database (`check_proiezione_immutabile` in `schema_cinemax.sql`).

## 4. Diagramma delle classi di dominio

![Diagramma delle classi di dominio](uml/classi_dominio.png)

Rappresenta il modello concettuale delle entità applicative (`Utente` e le sue
specializzazioni, `Film`, `Proiezione`, `Prenotazione`) così come viste dal
client, indipendentemente dai dettagli di persistenza. Corrisponde alle
entità/attributi dello schema ER ristrutturato (§3 di
`01_progettazione_database.md`); in particolare `Prenotazione.getCostoTotale()`
è un dato derivato non persistito (§3.3) e la gerarchia `Utente` rispecchia la
scelta di accorpamento ISA con attributo `ruolo` (§3.1).

## 5. Diagramma delle classi architetturale

![Diagramma delle classi architetturale](uml/classi_architettura.png)

Mostra come i pattern del §2 si traducono in classi concrete: le interfacce
remote in `cinemax.common`, le loro implementazioni server-side (`*ServiceImpl`,
facciate RMI), i DAO e il `ConnectionManager`, fino ai controller/view lato
client che invocano gli stub RMI.

## 6. Diagrammi di sequenza

Tre scenari rappresentativi, scelti per mostrare il percorso completo
client → stub RMI → service (server) → DAO → database, comprese le
interazioni con i vincoli implementati a livello di database (§6 di
`01_progettazione_database.md`).

### 6.1 Login

![Diagramma di sequenza — login](uml/sequenza_login.png)

Il client invoca `login` sullo stub RMI di `IAutenticazioneService`; il
server recupera l'utente tramite `UtenteDAO` e verifica la password con
`crypt()` (pgcrypto, vedi `schema_cinemax.sql`). In caso di successo,
`UtenteFactory` istanzia la sottoclasse corretta in base al `ruolo`
(pattern Factory Method, §2); in caso di credenziali non valide viene
propagata una `ServiceException` che il client mostra come errore.

### 6.2 Creazione di una prenotazione

![Diagramma di sequenza — creazione prenotazione](uml/sequenza_prenotazione.png)

Il servizio verifica la disponibilità di posti leggendo
`v_proiezioni_disponibilita`, poi inserisce la prenotazione. Il controllo
di capienza è applicato **due volte**: a livello applicativo (prima
dell'INSERT) e a livello database dal trigger `check_capienza_sala`
(difesa in profondità, indispensabile in presenza di più client
concorrenti che potrebbero prenotare sulla stessa proiezione nello stesso
istante). Il codice prenotazione è generato lato database da
`genera_codice_prenotazione()`.

### 6.3 Modifica di una proiezione con prenotazioni associate

![Diagramma di sequenza — modifica proiezione](uml/sequenza_modifica_proiezione.png)

Mostra il caso in cui l'operazione viene rifiutata: il trigger
`check_proiezione_immutabile` (BEFORE UPDATE/DELETE) impedisce la
modifica o l'eliminazione di una proiezione che ha già prenotazioni
associate, sollevando un'eccezione che risale fino al client come
`ServiceException`. Questo trigger è lo stesso corretto durante la
progettazione del database (bug del `RETURN OLD` in caso di UPDATE, vedi
`schema_cinemax.sql`), qui verificato anche dal punto di vista del
flusso applicativo end-to-end.
