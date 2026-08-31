-- =====================================================================
-- CineMax — Import del file draft fornito dal docente (proiezioni.csv)
--
-- La specifica di progetto (slide 5) indica che il docente fornisce un
-- file "proiezioni.csv" con i dati di partenza su film e proiezioni,
-- reso disponibile sulla pagina e-learning dell'insegnamento. Questo
-- script carica quel file (gia' incluso in questa stessa cartella) nelle
-- tabelle "film" e "proiezioni".
--
-- Da eseguire DOPO schema_cinemax.sql, con psql lanciato dalla cartella
-- radice del progetto (il percorso del CSV qui sotto e' relativo a li'):
--     psql -h <host> -U <utente> -d dbCM -f src/main/resources/db/import_proiezioni.sql
--
-- Nota: usa il comando \copy di psql (lato client), quindi non richiede
-- che il file CSV sia visibile dal processo server di PostgreSQL.
-- =====================================================================

-- Tabella "di appoggio" temporanea, con le stesse colonne del CSV: ci
-- serve solo per il caricamento grezzo, viene eliminata alla fine dello
-- script (e comunque sparirebbe da sola alla fine della sessione, visto
-- che e' TEMP).
CREATE TEMP TABLE staging_proiezioni (
    data_ora_proiezione   TIMESTAMP,
    titolo_film           VARCHAR(200),
    genere                VARCHAR(50),
    regista               VARCHAR(150),
    anno                  SMALLINT,
    durata_minuti         SMALLINT,
    eta_minima             SMALLINT,
    prezzo_biglietto      NUMERIC(6,2)
);

\copy staging_proiezioni FROM 'src/main/resources/db/proiezioni.csv' WITH (FORMAT csv, HEADER true, ENCODING 'UTF8');

-- 1) FILM: un solo film per ogni titolo distinto. Nel file i dati
--    anagrafici (genere, regista, anno, durata, eta' minima) sono sempre
--    identici su tutte le righe con lo stesso titolo, quindi basta il
--    titolo come criterio per non inserire lo stesso film piu' volte.
INSERT INTO film (titolo, genere, regista, anno, durata_minuti, eta_minima)
SELECT DISTINCT ON (titolo_film) titolo_film, genere, regista, anno, durata_minuti, eta_minima
FROM staging_proiezioni
ORDER BY titolo_film;

-- 2) PROIEZIONI: una per ogni riga del file, agganciata al film appena
--    inserito tramite il titolo. Il timestamp del CSV viene spezzato in
--    data e ora separate, come richiede lo schema.
INSERT INTO proiezioni (id_film, data_proiezione, ora_proiezione, costo_biglietto)
SELECT f.id_film, sp.data_ora_proiezione::date, sp.data_ora_proiezione::time, sp.prezzo_biglietto
FROM staging_proiezioni sp
JOIN film f ON f.titolo = sp.titolo_film;

DROP TABLE staging_proiezioni;

-- Verifica finale: quanti film e quante proiezioni sono stati caricati.
SELECT (SELECT COUNT(*) FROM film) AS totale_film,
       (SELECT COUNT(*) FROM proiezioni) AS totale_proiezioni;
