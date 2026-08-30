# CineMax — Query SQL a supporto dei servizi (Interfacce di Programmazione)

Laboratorio Interdisciplinare B — a.a. 2025/2026

Questo documento raccoglie, per ciascuna funzionalità richiesta dalle specifiche di progetto, la query SQL (PostgreSQL) che la implementa. È il completamento richiesto esplicitamente a specifica: *"Documentare [...] le query SQL a supporto dei servizi erogati da Interfacce di Programmazione"*.

Convenzioni:
- I placeholder `?` indicano i parametri che verranno passati come `PreparedStatement` da JDBC (nell'ordine in cui compaiono).
- Le query di ricerca con criteri opzionali usano il pattern `(? IS NULL OR condizione)`: se il parametro è `NULL` la condizione è ignorata. Con JDBC, il parametro va passato **due volte** (una per il test `IS NULL`, una per il confronto) — alternativa più pulita: costruire la query dinamicamente lato Java concatenando solo le condizioni per i criteri effettivamente impostati (scelta implementativa da fissare in fase di sviluppo).
- Tutte le query sono state validate eseguendole su un'istanza PostgreSQL 16 con lo schema di `schema_cinemax.sql` e i dati di `dati_esempio.sql` (vedi in fondo al documento).

---

## Consultazione proiezioni (login non necessario)

### `cercaProiezione(titolo, genere, dataDa, dataA, costoMin, costoMax)`

Ricerca combinabile per titolo (parziale), genere, intervallo di date, intervallo di costo.

```sql
SELECT id_proiezione, id_film, titolo, genere, regista, anno, durata_minuti,
       eta_minima, data_proiezione, ora_proiezione, costo_biglietto, posti_liberi
FROM v_proiezioni_disponibilita
WHERE (?::text  IS NULL OR titolo ILIKE '%' || ?::text  || '%')
  AND (?::text  IS NULL OR genere = ?::text)
  AND (?::date  IS NULL OR data_proiezione >= ?::date)
  AND (?::date  IS NULL OR data_proiezione <= ?::date)
  AND (?::numeric IS NULL OR costo_biglietto >= ?::numeric)
  AND (?::numeric IS NULL OR costo_biglietto <= ?::numeric)
ORDER BY data_proiezione, ora_proiezione;
```

### `visualizzaProiezione(idProiezione)`

Dettagli di una proiezione selezionata dopo la ricerca: caratteristiche del film, data/ora, costo, posti liberi (dato derivato — vedi `doc/01_progettazione_database.md`, §3.3).

```sql
SELECT id_proiezione, id_film, titolo, genere, regista, anno, durata_minuti,
       eta_minima, data_proiezione, ora_proiezione, costo_biglietto, posti_liberi
FROM v_proiezioni_disponibilita
WHERE id_proiezione = ?;
```

### Schermata iniziale per l'utente `guest`

Proiezioni nei tre mesi successivi alla data odierna per un film indicato (anche parzialmente).

```sql
SELECT id_proiezione, id_film, titolo, genere, regista, anno, durata_minuti,
       eta_minima, data_proiezione, ora_proiezione, costo_biglietto, posti_liberi
FROM v_proiezioni_disponibilita
WHERE titolo ILIKE '%' || ? || '%'
  AND data_proiezione BETWEEN CURRENT_DATE AND (CURRENT_DATE + INTERVAL '3 months')
ORDER BY data_proiezione, ora_proiezione;
```

---

## Registrazione (login non necessario)

### `registraCliente(nome, cognome, username, password, dataNascita, luogoDomicilio)`

Verifica preliminare di disponibilità dello `username` (in alternativa: tentare direttamente l'`INSERT` e intercettare la violazione del vincolo `UNIQUE`):

```sql
SELECT 1 FROM utenti WHERE username = ?;
```

Inserimento (password cifrata con `pgcrypto`; `dataNascita` è facoltativa, può essere passata come `NULL`):

```sql
INSERT INTO utenti (nome, cognome, username, password_hash, data_nascita, luogo_domicilio, ruolo)
VALUES (?, ?, ?, crypt(?, gen_salt('bf')), ?, ?, 'cliente')
RETURNING id_utente;
```

---

## Autenticazione (comune a cliente, proiezionista, bigliettaio)

```sql
SELECT id_utente, nome, cognome, ruolo
FROM utenti
WHERE username = ?
  AND password_hash = crypt(?, password_hash);
```

Nessuna riga restituita ⇒ credenziali errate. Il `ruolo` letto determina il menù da presentare all'utente.

---

## Interazione dei clienti (login necessario)

### `visualizzaPrenotazione(idUtente)` — prenotazioni attive del cliente

"Attive" = relative a una proiezione successiva alla data odierna (vedi specifica, slide 15).

```sql
SELECT pr.codice_prenotazione, pr.num_posti, pr.data_prenotazione,
       f.titolo, p.data_proiezione, p.ora_proiezione, p.costo_biglietto,
       (pr.num_posti * p.costo_biglietto) AS costo_totale
FROM prenotazioni pr
JOIN proiezioni p ON p.id_proiezione = pr.id_proiezione
JOIN film f       ON f.id_film = p.id_film
WHERE pr.id_utente = ?
  AND p.data_proiezione > CURRENT_DATE
ORDER BY p.data_proiezione, p.ora_proiezione;
```

### `creaPrenotazione(idUtente, idProiezione, numPosti)`

La verifica "posti richiesti < posti disponibili" può essere anticipata lato applicativo con una query di controllo, ma è comunque garantita a livello di database dal trigger `trg_capienza_sala` (che impedisce il superamento dei 200 posti) e da `trg_ruolo_cliente` (solo utenti con ruolo `cliente` possono prenotare) — vedi `schema_cinemax.sql`.

```sql
-- (opzionale) controllo preliminare posti disponibili
SELECT posti_liberi FROM v_proiezioni_disponibilita WHERE id_proiezione = ?;

-- inserimento; il codice prenotazione è generato automaticamente dal DB
INSERT INTO prenotazioni (id_utente, id_proiezione, num_posti)
VALUES (?, ?, ?)
RETURNING codice_prenotazione;
```

### `modificaPrenotazione(codicePrenotazione, nuovaIdProiezione)` — cambio proiezione/data

Precondizione di specifica: sia la vecchia sia la nuova data di proiezione devono essere successive alla data odierna. Questo è un vincolo **temporale**, verificato lato applicativo (non è esprimibile con un trigger sullo stato del database in modo pulito, perché dipende dalla proiezione scelta *prima* dell'update):

```sql
-- 1) recupero la data della proiezione attualmente associata alla prenotazione
SELECT p.data_proiezione
FROM prenotazioni pr JOIN proiezioni p ON p.id_proiezione = pr.id_proiezione
WHERE pr.codice_prenotazione = ?;

-- 2) recupero la data della nuova proiezione scelta
SELECT data_proiezione FROM proiezioni WHERE id_proiezione = ?;

-- 3) se entrambe le date sono > CURRENT_DATE (verificato in Java), si applica l'update
--    (il trigger di capienza sala si riattiva automaticamente anche in UPDATE)
UPDATE prenotazioni
SET id_proiezione = ?
WHERE codice_prenotazione = ?;
```

### `eliminaPrenotazione(codicePrenotazione)`

**Nota**: la specifica condiziona l'eliminazione a "data di proiezione precedente alla data odierna" (slide 12), condizione che appare invertita rispetto alla logica applicativa attesa (si veda `doc/01_progettazione_database.md`, §1.4) — da verificare col docente. La query di cancellazione non cambia in base a quale sia l'interpretazione corretta; cambia solo il controllo (in Java) sulla data prima di eseguirla:

```sql
-- controllo preliminare (verificare in Java la condizione sulla data secondo l'interpretazione confermata)
SELECT p.data_proiezione
FROM prenotazioni pr JOIN proiezioni p ON p.id_proiezione = pr.id_proiezione
WHERE pr.codice_prenotazione = ?;

DELETE FROM prenotazioni WHERE codice_prenotazione = ?;
```

---

## Interazione dei bigliettai (login necessario)

### Prenotazioni nella data odierna

```sql
SELECT pr.codice_prenotazione, u.nome, u.cognome, f.titolo,
       p.data_proiezione, p.ora_proiezione, pr.num_posti, p.costo_biglietto,
       (pr.num_posti * p.costo_biglietto) AS costo_totale
FROM prenotazioni pr
JOIN utenti u     ON u.id_utente = pr.id_utente
JOIN proiezioni p ON p.id_proiezione = pr.id_proiezione
JOIN film f       ON f.id_film = p.id_film
WHERE p.data_proiezione = CURRENT_DATE
ORDER BY p.ora_proiezione;
```

### `cercaPrenotazione(codice, nomeCognome, titoloFilm, dataDa, dataA)`

```sql
SELECT pr.codice_prenotazione, u.nome, u.cognome, f.titolo,
       p.data_proiezione, p.ora_proiezione, pr.num_posti, p.costo_biglietto,
       (pr.num_posti * p.costo_biglietto) AS costo_totale
FROM prenotazioni pr
JOIN utenti u     ON u.id_utente = pr.id_utente
JOIN proiezioni p ON p.id_proiezione = pr.id_proiezione
JOIN film f       ON f.id_film = p.id_film
WHERE (?::text IS NULL OR pr.codice_prenotazione = ?::text)
  AND (?::text IS NULL OR u.nome ILIKE '%' || ?::text || '%' OR u.cognome ILIKE '%' || ?::text || '%')
  AND (?::text IS NULL OR f.titolo ILIKE '%' || ?::text || '%')
  AND (?::date IS NULL OR p.data_proiezione >= ?::date)
  AND (?::date IS NULL OR p.data_proiezione <= ?::date)
ORDER BY p.data_proiezione, p.ora_proiezione;
```

### `visualizzaPrenotazione(codicePrenotazione)` — dettaglio (bigliettaio)

```sql
SELECT pr.codice_prenotazione, u.nome, u.cognome, f.titolo,
       p.data_proiezione, p.ora_proiezione, pr.num_posti, p.costo_biglietto,
       (pr.num_posti * p.costo_biglietto) AS costo_totale
FROM prenotazioni pr
JOIN utenti u     ON u.id_utente = pr.id_utente
JOIN proiezioni p ON p.id_proiezione = pr.id_proiezione
JOIN film f       ON f.id_film = p.id_film
WHERE pr.codice_prenotazione = ?;
```

---

## Interazione dei proiezionisti (login necessario)

### `aggiungiProiezione(...)` — nuovo film + proiezione

Per specifica, il proiezionista prima inserisce un film e poi, per quel film, data e costo di ogni proiezione. Se il film esiste già a catalogo si esegue solo il secondo passo.

```sql
-- 1) nuovo film (solo se non già presente a catalogo)
INSERT INTO film (titolo, genere, regista, anno, durata_minuti, eta_minima)
VALUES (?, ?, ?, ?, ?, ?)
RETURNING id_film;

-- 2) nuova proiezione per il film (id_film noto dal passo precedente o da catalogo esistente)
-- il trigger trg_sovrapposizione_proiezione impedisce automaticamente le sovrapposizioni
INSERT INTO proiezioni (id_film, data_proiezione, ora_proiezione, costo_biglietto)
VALUES (?, ?, ?, ?)
RETURNING id_proiezione;
```

### Proiezioni pianificate (successive alla data odierna)

```sql
SELECT id_proiezione, id_film, titolo, data_proiezione, ora_proiezione,
       costo_biglietto, posti_liberi
FROM v_proiezioni_disponibilita
WHERE data_proiezione > CURRENT_DATE
ORDER BY data_proiezione, ora_proiezione;
```

### Proiezioni storiche (precedenti alla data odierna)

```sql
SELECT id_proiezione, id_film, titolo, data_proiezione, ora_proiezione,
       costo_biglietto, posti_liberi
FROM v_proiezioni_disponibilita
WHERE data_proiezione < CURRENT_DATE
ORDER BY data_proiezione DESC, ora_proiezione DESC;
```

### `modificaProiezione(idProiezione, nuovaData, nuovaOra, nuovoCosto)`

Ammessa solo se non esistono prenotazioni per la proiezione: vincolo garantito dal trigger `trg_proiezione_immutabile_update` (fallisce con eccezione se ci sono prenotazioni). Il trigger di sovrapposizione (`trg_sovrapposizione_proiezione`) si riattiva automaticamente anche in `UPDATE`.

```sql
UPDATE proiezioni
SET data_proiezione = ?, ora_proiezione = ?, costo_biglietto = ?
WHERE id_proiezione = ?;
```

### `eliminaProiezione(idProiezione)`

Ammessa solo se non esistono prenotazioni: vincolo garantito dal trigger `trg_proiezione_immutabile_delete`.

```sql
DELETE FROM proiezioni WHERE id_proiezione = ?;
```

---

## Verifica

Tutte le query sopra sono state eseguite (con valori di prova) su un'istanza PostgreSQL 16 con lo schema definito in `src/main/resources/db/schema_cinemax.sql`, popolato con i dati di `src/main/resources/db/dati_esempio.sql`: hanno prodotto risultati corretti e i trigger di vincolo si sono attivati correttamente nei casi limite (posti insufficienti, ruolo non cliente, sovrapposizione proiezioni, proiezione con prenotazioni).
