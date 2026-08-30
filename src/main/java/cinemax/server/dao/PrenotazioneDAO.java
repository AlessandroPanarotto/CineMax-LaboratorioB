package cinemax.server.dao;

import cinemax.common.Prenotazione;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/** Accesso alla tabella {@code prenotazioni} (vedi {@code doc/02_query_servizi.md}). */
public interface PrenotazioneDAO {

    /**
     * @return il codice prenotazione generato dal database
     * @throws SQLException se un trigger rifiuta l'inserimento: utente non
     *      cliente ({@code trg_ruolo_cliente}) o posti insufficienti
     *      ({@code trg_capienza_sala})
     */
    String inserisci(long idUtente, long idProiezione, int numPosti) throws SQLException;

    /** Prenotazioni attive (relative a proiezioni future) di un cliente. */
    List<Prenotazione> findByUtente(long idUtente) throws SQLException;

    /**
     * @throws SQLException se il trigger {@code trg_capienza_sala} rifiuta
     *      la modifica (posti insufficienti sulla nuova proiezione)
     */
    void aggiornaProiezione(String codicePrenotazione, long nuovaIdProiezione) throws SQLException;

    void elimina(String codicePrenotazione) throws SQLException;

    /** Prenotazioni relative a proiezioni della data odierna. */
    List<Prenotazione> odierne() throws SQLException;

    /** Ricerca combinabile: ogni criterio {@code null} viene ignorato. */
    List<Prenotazione> cerca(String codice, String nomeCognome, String titoloFilm,
                              LocalDate dataDa, LocalDate dataA) throws SQLException;

    /** @return la prenotazione, oppure {@code null} se il codice non esiste */
    Prenotazione findByCodice(String codicePrenotazione) throws SQLException;

    /**
     * Data della proiezione attualmente associata a una prenotazione — usata
     * dal service per verificare le precondizioni temporali di
     * {@code modificaPrenotazione}/{@code eliminaPrenotazione} prima di
     * eseguire l'operazione (vedi {@code doc/02_query_servizi.md}).
     *
     * @return la data, oppure {@code null} se il codice non esiste
     */
    LocalDate dataProiezioneDiPrenotazione(String codicePrenotazione) throws SQLException;
}
