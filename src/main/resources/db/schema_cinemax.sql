-- =====================================================================
-- CineMax — Script di creazione del database dbCM (PostgreSQL)
-- Laboratorio Interdisciplinare B — a.a. 2025/2026
--
-- Ordine di esecuzione: questo script e' pensato per essere eseguito
-- per intero, in ordine, su un database vuoto (dbCM):
--     psql -h <host> -U <utente> -d dbCM -f schema_cinemax.sql
-- (oppure richiamato dal target Maven, vedi pom.xml / README)
-- =====================================================================


-- ---------------------------------------------------------------------
-- 0. ESTENSIONI
-- ---------------------------------------------------------------------
-- pgcrypto fornisce crypt()/gen_salt() per l'hashing delle password
-- (dimostrativo: in alternativa l'hashing puo' essere fatto lato Java,
--  ad es. con la libreria jBCrypt, e la colonna password_hash valorizzata
--  gia' cifrata dall'applicazione).
CREATE EXTENSION IF NOT EXISTS pgcrypto;


-- ---------------------------------------------------------------------
-- 1. TABELLE
-- ---------------------------------------------------------------------

-- FILM ------------------------------------------------------------------
CREATE TABLE film (
    id_film         BIGSERIAL PRIMARY KEY,
    titolo          VARCHAR(200)  NOT NULL,
    genere          VARCHAR(50)   NOT NULL,
    regista         VARCHAR(150)  NOT NULL,
    anno            SMALLINT      NOT NULL CHECK (anno BETWEEN 1888 AND 2100),
    durata_minuti   SMALLINT      NOT NULL CHECK (durata_minuti > 0),
    eta_minima      SMALLINT      NOT NULL DEFAULT 0 CHECK (eta_minima >= 0)
);

COMMENT ON TABLE film IS 'Catalogo dei film disponibili sulla piattaforma CineMax';

-- UTENTI ------------------------------------------------------------------
-- Tabella unica richiesta da specifica: la generalizzazione concettuale
-- Utente/Cliente/Proiezionista/Bigliettaio viene qui accorpata nel padre,
-- con il ruolo come attributo discriminante (vedi doc/01_progettazione_database.md, §3.1).
CREATE TABLE utenti (
    id_utente       BIGSERIAL PRIMARY KEY,
    nome            VARCHAR(100)  NOT NULL,
    cognome         VARCHAR(100)  NOT NULL,
    username        VARCHAR(50)   NOT NULL UNIQUE,
    password_hash   VARCHAR(200)  NOT NULL,
    data_nascita    DATE          NULL,
    luogo_domicilio VARCHAR(150)  NOT NULL,
    ruolo           VARCHAR(20)   NOT NULL
                        CHECK (ruolo IN ('cliente', 'proiezionista', 'bigliettaio'))
);

COMMENT ON TABLE utenti IS 'Utenti registrati (clienti, proiezionisti, bigliettai) — ruolo come discriminante ex-gerarchia ISA';
COMMENT ON COLUMN utenti.password_hash IS 'Password cifrata (mai in chiaro)';

-- PROIEZIONI ------------------------------------------------------------------
CREATE TABLE proiezioni (
    id_proiezione   BIGSERIAL PRIMARY KEY,
    id_film         BIGINT        NOT NULL REFERENCES film (id_film) ON DELETE RESTRICT,
    data_proiezione DATE          NOT NULL,
    ora_proiezione  TIME          NOT NULL,
    costo_biglietto NUMERIC(6,2)  NOT NULL CHECK (costo_biglietto > 0)
);

COMMENT ON TABLE proiezioni IS 'Palinsesto: singola proiezione di un film in una data/ora, con relativo prezzo';
CREATE INDEX idx_proiezioni_film ON proiezioni (id_film);
CREATE INDEX idx_proiezioni_data ON proiezioni (data_proiezione);

-- PRENOTAZIONI ------------------------------------------------------------------
-- funzione di generazione codice prenotazione leggibile (8 caratteri,
-- alfabeto senza caratteri ambigui 0/O, 1/I/L)
CREATE OR REPLACE FUNCTION genera_codice_prenotazione() RETURNS VARCHAR(8) AS $$
DECLARE
    alfabeto  CONSTANT TEXT := 'ABCDEFGHJKMNPQRSTUVWXYZ23456789';
    codice    TEXT := '';
    i         INT;
BEGIN
    FOR i IN 1..8 LOOP
        codice := codice || substr(alfabeto, (floor(random() * length(alfabeto)) + 1)::int, 1);
    END LOOP;
    RETURN codice;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE prenotazioni (
    codice_prenotazione VARCHAR(8)   PRIMARY KEY DEFAULT genera_codice_prenotazione(),
    id_utente            BIGINT       NOT NULL REFERENCES utenti (id_utente) ON DELETE RESTRICT,
    id_proiezione         BIGINT       NOT NULL REFERENCES proiezioni (id_proiezione) ON DELETE RESTRICT,
    num_posti             SMALLINT     NOT NULL CHECK (num_posti > 0),
    data_prenotazione     TIMESTAMP    NOT NULL DEFAULT now()
);

COMMENT ON TABLE prenotazioni IS 'Prenotazione di posti, effettuata da un utente con ruolo cliente, per una proiezione';
CREATE INDEX idx_prenotazioni_utente ON prenotazioni (id_utente);
CREATE INDEX idx_prenotazioni_proiezione ON prenotazioni (id_proiezione);


-- ---------------------------------------------------------------------
-- 2. VINCOLI PROCEDURALI (trigger) — vincoli non esprimibili con CHECK
--    perche' richiedono di leggere altre righe/tabelle
--    (vedi doc/01_progettazione_database.md, §2.3 e §4.2)
-- ---------------------------------------------------------------------

-- 2.1 Un utente puo' comparire in una prenotazione solo se ruolo = 'cliente'
CREATE OR REPLACE FUNCTION check_ruolo_cliente() RETURNS TRIGGER AS $$
DECLARE
    v_ruolo TEXT;
BEGIN
    SELECT ruolo INTO v_ruolo FROM utenti WHERE id_utente = NEW.id_utente;
    IF v_ruolo IS DISTINCT FROM 'cliente' THEN
        RAISE EXCEPTION 'Solo un utente con ruolo ''cliente'' puo'' effettuare una prenotazione (utente id=%, ruolo=%)',
            NEW.id_utente, v_ruolo;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_ruolo_cliente
    BEFORE INSERT OR UPDATE OF id_utente ON prenotazioni
    FOR EACH ROW EXECUTE FUNCTION check_ruolo_cliente();

-- 2.2 Capienza sala: la somma dei posti prenotati per una proiezione non
--     puo' superare i 200 posti della sala (cinema monosala)
CREATE OR REPLACE FUNCTION check_capienza_sala() RETURNS TRIGGER AS $$
DECLARE
    v_capienza  CONSTANT SMALLINT := 200;
    v_occupati  INT;
BEGIN
    SELECT COALESCE(SUM(num_posti), 0) INTO v_occupati
    FROM prenotazioni
    WHERE id_proiezione = NEW.id_proiezione
      AND codice_prenotazione <> COALESCE(NEW.codice_prenotazione, '');

    IF v_occupati + NEW.num_posti > v_capienza THEN
        RAISE EXCEPTION 'Posti richiesti (%) superiori ai posti disponibili per la proiezione % (occupati % / %)',
            NEW.num_posti, NEW.id_proiezione, v_occupati, v_capienza;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_capienza_sala
    BEFORE INSERT OR UPDATE OF num_posti, id_proiezione ON prenotazioni
    FOR EACH ROW EXECUTE FUNCTION check_capienza_sala();

-- 2.3 Non sovrapposizione delle proiezioni (sala unica): l'intervallo
--     [data+ora, data+ora+durata_film) di una nuova/modificata proiezione
--     non puo' intersecare quello di un'altra proiezione esistente
CREATE OR REPLACE FUNCTION check_sovrapposizione_proiezione() RETURNS TRIGGER AS $$
DECLARE
    v_durata      SMALLINT;
    v_inizio_new  TIMESTAMP;
    v_fine_new    TIMESTAMP;
BEGIN
    SELECT durata_minuti INTO v_durata FROM film WHERE id_film = NEW.id_film;
    v_inizio_new := NEW.data_proiezione + NEW.ora_proiezione;
    v_fine_new   := v_inizio_new + make_interval(mins => v_durata);

    IF EXISTS (
        SELECT 1
        FROM proiezioni p
        JOIN film f ON f.id_film = p.id_film
        WHERE p.id_proiezione <> COALESCE(NEW.id_proiezione, -1)
          AND (p.data_proiezione + p.ora_proiezione,
               p.data_proiezione + p.ora_proiezione + make_interval(mins => f.durata_minuti))
              OVERLAPS (v_inizio_new, v_fine_new)
    ) THEN
        RAISE EXCEPTION 'La proiezione (film id=%, % %) si sovrappone a una proiezione gia'' esistente (sala unica)',
            NEW.id_film, NEW.data_proiezione, NEW.ora_proiezione;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_sovrapposizione_proiezione
    BEFORE INSERT OR UPDATE OF id_film, data_proiezione, ora_proiezione ON proiezioni
    FOR EACH ROW EXECUTE FUNCTION check_sovrapposizione_proiezione();

-- 2.4 Una proiezione con prenotazioni non puo' essere modificata ne' eliminata
CREATE OR REPLACE FUNCTION check_proiezione_immutabile() RETURNS TRIGGER AS $$
DECLARE
    v_id BIGINT := COALESCE(OLD.id_proiezione, NULL);
    v_n  INT;
BEGIN
    SELECT COUNT(*) INTO v_n FROM prenotazioni WHERE id_proiezione = v_id;
    IF v_n > 0 THEN
        RAISE EXCEPTION 'Impossibile modificare/eliminare la proiezione % : esistono % prenotazioni associate',
            v_id, v_n;
    END IF;
    -- In UPDATE un trigger BEFORE deve restituire NEW per lasciar applicare le modifiche
    -- (restituire OLD annullerebbe silenziosamente l'update); in DELETE, NEW e' NULL
    -- quindi va restituito OLD per consentire la cancellazione.
    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    ELSE
        RETURN NEW;
    END IF;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_proiezione_immutabile_update
    BEFORE UPDATE ON proiezioni
    FOR EACH ROW EXECUTE FUNCTION check_proiezione_immutabile();

CREATE TRIGGER trg_proiezione_immutabile_delete
    BEFORE DELETE ON proiezioni
    FOR EACH ROW EXECUTE FUNCTION check_proiezione_immutabile();


-- ---------------------------------------------------------------------
-- 3. VISTA DI COMODO: posti liberi per proiezione (dato derivato, §3.3)
-- ---------------------------------------------------------------------
CREATE OR REPLACE VIEW v_proiezioni_disponibilita AS
SELECT
    p.id_proiezione,
    p.id_film,
    f.titolo,
    f.genere,
    f.regista,
    f.anno,
    f.durata_minuti,
    f.eta_minima,
    p.data_proiezione,
    p.ora_proiezione,
    p.costo_biglietto,
    200 - COALESCE(SUM(pr.num_posti), 0) AS posti_liberi
FROM proiezioni p
JOIN film f ON f.id_film = p.id_film
LEFT JOIN prenotazioni pr ON pr.id_proiezione = p.id_proiezione
GROUP BY p.id_proiezione, p.id_film, f.titolo, f.genere, f.regista, f.anno,
         f.durata_minuti, f.eta_minima, p.data_proiezione, p.ora_proiezione, p.costo_biglietto;


-- ---------------------------------------------------------------------
-- 4. DATI DI SEED OBBLIGATORI: 2 proiezionisti + 5 bigliettai
--    (vedi specifica, slide 6: "N.B. La tabella dovra' gia' contenere
--     2 proiezionisti e 5 bigliettai")
--    Password dimostrative cifrate con pgcrypto (crypt/bf); in fase di
--    login l'applicazione Java verifichera' con: crypt(input, password_hash) = password_hash
-- ---------------------------------------------------------------------
INSERT INTO utenti (nome, cognome, username, password_hash, luogo_domicilio, ruolo) VALUES
    ('Marco',    'Bianchi',  'm.bianchi',  crypt('Proiez1!2026', gen_salt('bf')), 'Varese',  'proiezionista'),
    ('Elena',    'Ferrari',  'e.ferrari',  crypt('Proiez2!2026', gen_salt('bf')), 'Como',    'proiezionista'),
    ('Giulia',   'Colombo',  'g.colombo',  crypt('Bigl1!2026',   gen_salt('bf')), 'Varese',  'bigliettaio'),
    ('Luca',     'Romano',   'l.romano',   crypt('Bigl2!2026',   gen_salt('bf')), 'Como',    'bigliettaio'),
    ('Sara',     'Ricci',    's.ricci',    crypt('Bigl3!2026',   gen_salt('bf')), 'Varese',  'bigliettaio'),
    ('Davide',   'Marino',   'd.marino',   crypt('Bigl4!2026',   gen_salt('bf')), 'Busto Arsizio', 'bigliettaio'),
    ('Chiara',   'Greco',    'c.greco',    crypt('Bigl5!2026',   gen_salt('bf')), 'Como',    'bigliettaio');

-- =====================================================================
-- Fine script. I dati su film/proiezioni verranno importati a partire dal
-- file draft "proiezioni.csv" fornito dal docente (script di import
-- separato: vedi db/import_proiezioni.sql o utility Java con JDBC —
-- dipende dall'esatto formato/colonne del CSV, non ancora disponibile
-- in questa sessione).
-- =====================================================================
