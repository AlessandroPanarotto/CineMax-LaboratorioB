# Database CineMax (dbCM)

## Requisiti
- PostgreSQL (testato su v16)
- estensione `pgcrypto` (abilitata automaticamente dallo script)

## Come creare il database

```bash
createdb dbCM            # oppure: psql -c "CREATE DATABASE \"dbCM\";"
psql -d dbCM -f schema_cinemax.sql
# opzionale, solo per test locali:
psql -d dbCM -f dati_esempio.sql
```

## Contenuto

- `schema_cinemax.sql` — tabelle (`film`, `utenti`, `proiezioni`, `prenotazioni`), vincoli, funzioni/trigger, vista `v_proiezioni_disponibilita`, seed obbligatorio (2 proiezionisti + 5 bigliettai).
- `dati_esempio.sql` — dati facoltativi per provare lo schema in locale (da NON includere nella consegna finale: i dati reali verranno importati da `proiezioni.csv`).

Dettagli di progettazione (analisi requisiti, schema E-R concettuale e ristrutturato, traduzione relazionale, motivazioni) in `../doc/01_progettazione_database.md`.
