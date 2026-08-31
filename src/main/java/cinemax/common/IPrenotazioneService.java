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
package cinemax.common;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.time.LocalDate;
import java.util.List;

/**
 * Interfaccia remota per gestire le prenotazioni: da un lato ci sono le
 * operazioni del cliente (creare, modificare, cancellare e vedere le
 * proprie prenotazioni), dall'altro quelle del bigliettaio (che puo' solo
 * consultare e cercare le prenotazioni di tutti).
 */
public interface IPrenotazioneService extends Remote {

    /**
     * Crea una nuova prenotazione per l'utente indicato.
     *
     * @return il codice della prenotazione appena creata (generato dal database)
     * @throws ServiceException se l'utente non e' un cliente, se non ci
     *      sono abbastanza posti liberi, oppure se la proiezione non esiste
     */
    String creaPrenotazione(long idUtente, long idProiezione, int numPosti)
            throws RemoteException, ServiceException;

    /** Restituisce le prenotazioni ancora attive (cioe' per proiezioni future) di un cliente. */
    List<Prenotazione> visualizzaPrenotazioni(long idUtente) throws RemoteException;

    /**
     * Sposta una prenotazione su un'altra proiezione.
     *
     * @throws ServiceException se la proiezione vecchia o quella nuova non
     *      sono entrambe future, oppure se sulla nuova proiezione non ci
     *      sono abbastanza posti liberi
     */
    void modificaPrenotazione(String codicePrenotazione, long nuovaIdProiezione)
            throws RemoteException, ServiceException;

    /**
     * Elimina (cioe' disdice) una prenotazione.
     *
     * @throws ServiceException se la prenotazione con quel codice non esiste
     */
    void eliminaPrenotazione(String codicePrenotazione) throws RemoteException, ServiceException;

    /** Restituisce le prenotazioni relative alle proiezioni di oggi (vista del bigliettaio). */
    List<Prenotazione> prenotazioniOdierne() throws RemoteException;

    /**
     * Cerca le prenotazioni in base ai criteri passati: codice, nome o
     * cognome del cliente, titolo del film, intervallo di date. Ogni
     * criterio e' facoltativo (vista del bigliettaio).
     */
    List<Prenotazione> cercaPrenotazioni(String codice, String nomeCognome, String titoloFilm,
                                          LocalDate dataDa, LocalDate dataA) throws RemoteException;

    /** Restituisce i dettagli di una prenotazione dato il suo codice (vista del bigliettaio). */
    Prenotazione visualizzaPrenotazione(String codicePrenotazione)
            throws RemoteException, ServiceException;
}
