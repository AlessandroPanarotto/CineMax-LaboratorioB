/*
 * Progetto CineMax - Laboratorio Interdisciplinare B (a.a. 2025/2026)
 * Universita' degli Studi dell'Insubria
 *
 * Autori:
 *   Panarotto Alessandro   - matricola 757930 - sede di Varese (VA)
 *   Calabrese Davide Paolo - matricola 763012 - sede di Varese (VA)
 *   Mohan Thomas Paolo     - matricola 761573 - sede di Varese (VA)
 *   Trentini Federico      - matricola 760478 - sede di Varese (VA)
 */
package cinemax.server.dao;

import cinemax.common.Prenotazione;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/** DAO per la tabella "prenotazioni": le firme dei metodi che i service usano per leggere/scrivere le prenotazioni. */
public interface PrenotazioneDAO {

    /**
     * Inserisce una nuova prenotazione e restituisce il codice generato dal database.
     * Attenzione: nel database ci sono dei trigger che possono rifiutare
     * l'inserimento (per esempio se l'utente non e' un cliente, o se non ci
     * sono abbastanza posti liberi): in quel caso questo metodo lancia una SQLException.
     */
    String inserisci(long idUtente, long idProiezione, int numPosti) throws SQLException;

    /** Restituisce le prenotazioni ancora "attive" (relative a proiezioni future) di un cliente. */
    List<Prenotazione> findByUtente(long idUtente) throws SQLException;

    /**
     * Sposta una prenotazione su un'altra proiezione. Anche qui c'e' un
     * trigger sul database che puo' rifiutare la modifica se sulla nuova
     * proiezione non ci sono abbastanza posti liberi.
     */
    void aggiornaProiezione(String codicePrenotazione, long nuovaIdProiezione) throws SQLException;

    /** Cancella una prenotazione dato il suo codice. */
    void elimina(String codicePrenotazione) throws SQLException;

    /** Restituisce le prenotazioni relative alle proiezioni di oggi. */
    List<Prenotazione> odierne() throws SQLException;

    /** Ricerca con criteri combinabili: ogni parametro che viene passato null non viene usato come filtro. */
    List<Prenotazione> cerca(String codice, String nomeCognome, String titoloFilm,
                              LocalDate dataDa, LocalDate dataA) throws SQLException;

    /** Cerca una prenotazione dal suo codice. Restituisce null se il codice non esiste. */
    Prenotazione findByCodice(String codicePrenotazione) throws SQLException;

    /**
     * Restituisce solo la data della proiezione a cui e' collegata una
     * prenotazione. Serve al service per controllare, prima di modificare o
     * cancellare una prenotazione, che la proiezione non sia gia' passata.
     * Restituisce null se il codice non esiste.
     */
    LocalDate dataProiezioneDiPrenotazione(String codicePrenotazione) throws SQLException;
}
