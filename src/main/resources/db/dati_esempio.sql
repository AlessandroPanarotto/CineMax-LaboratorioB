-- =====================================================================
-- CineMax — Dati di esempio (OPZIONALE, solo per test/sviluppo)
-- Da eseguire dopo schema_cinemax.sql. NON richiesto dalla specifica:
-- i dati reali di film/proiezioni andranno importati dal file
-- proiezioni.csv fornito dal docente.
-- =====================================================================

-- Un cliente di prova
INSERT INTO utenti (nome, cognome, username, password_hash, data_nascita, luogo_domicilio, ruolo)
VALUES ('Anna', 'Verdi', 'a.verdi', crypt('Cliente1!', gen_salt('bf')), '1998-04-12', 'Varese', 'cliente');

-- Alcuni film
INSERT INTO film (titolo, genere, regista, anno, durata_minuti, eta_minima) VALUES
    ('Interstellar',        'Fantascienza', 'Christopher Nolan', 2014, 169, 12),
    ('La vita e'' bella',    'Commedia',     'Roberto Benigni',   1997, 116, 0),
    ('Dune: Parte Due',     'Fantascienza', 'Denis Villeneuve',  2024, 166, 14);

-- Alcune proiezioni (date nel futuro rispetto a oggi, per poter testare le prenotazioni)
INSERT INTO proiezioni (id_film, data_proiezione, ora_proiezione, costo_biglietto)
SELECT id_film, CURRENT_DATE + 3, TIME '21:00', 8.50 FROM film WHERE titolo = 'Interstellar';

INSERT INTO proiezioni (id_film, data_proiezione, ora_proiezione, costo_biglietto)
SELECT id_film, CURRENT_DATE + 4, TIME '18:30', 6.00 FROM film WHERE titolo = 'La vita e'' bella';

INSERT INTO proiezioni (id_film, data_proiezione, ora_proiezione, costo_biglietto)
SELECT id_film, CURRENT_DATE + 5, TIME '21:15', 9.00 FROM film WHERE titolo = 'Dune: Parte Due';

-- Una prenotazione di prova
INSERT INTO prenotazioni (id_utente, id_proiezione, num_posti)
SELECT u.id_utente, p.id_proiezione, 2
FROM utenti u, proiezioni p, film f
WHERE u.username = 'a.verdi' AND p.id_film = f.id_film AND f.titolo = 'Interstellar';
