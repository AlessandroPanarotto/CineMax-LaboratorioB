# Database CineMax (dbCM)

## Requisiti
- PostgreSQL (testato su v16)
- estensione `pgcrypto` (abilitata automaticamente dallo script)

## Come creare il database

```bash
createdb dbCM            # oppure: psql -c "CREATE DATABASE \"dbCM\";"
psql -d dbCM -f schema_cinemax.sql
# dati di partenza forniti dal docente (725 film, 8878 proiezioni):
psql -d dbCM -f import_proiezioni.sql
# opzionale, solo per test locali:
psql -d dbCM -f dati_esempio.sql
```

## Contenuto

- `schema_cinemax.sql` — tabelle (`film`, `utenti`, `proiezioni`, `prenotazioni`), vincoli, funzioni/trigger, vista `v_proiezioni_disponibilita`, seed obbligatorio (2 proiezionisti + 5 bigliettai).
- `proiezioni.csv` — file draft fornito dal docente con i dati di partenza su film e proiezioni.
- `import_proiezioni.sql` — script che importa `proiezioni.csv` (con `\copy`) nelle tabelle `film` e `proiezioni`.
- `dati_esempio.sql` — dati facoltativi aggiuntivi per provare lo schema in locale (da NON includere nella consegna finale).

Dettagli di progettazione (analisi requisiti, schema E-R concettuale e ristrutturato, traduzione relazionale, motivazioni) in `../doc/01_progettazione_database.md`.
