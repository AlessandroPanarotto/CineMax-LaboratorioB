package cinemax.common;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.time.LocalDate;
import java.util.List;

/**
 * Servizio di gestione delle prenotazioni: operazioni per il cliente
 * (crea/modifica/elimina/visualizza le proprie) e per il bigliettaio
 * (consultazione e ricerca).
 */
public interface IPrenotazioneService extends Remote {

    /**
     * Crea una prenotazione per l'utente indicato.
     *
     * @return il codice prenotazione generato dal database
     * @throws ServiceException se l'utente non ha ruolo cliente, se i posti
     *      richiesti non sono disponibili, o se la proiezione non esiste
     */
    String creaPrenotazione(long idUtente, long idProiezione, int numPosti)
            throws RemoteException, ServiceException;

    /** Prenotazioni attive (relative a proiezioni future) di un cliente. */
    List<Prenotazione> visualizzaPrenotazioni(long idUtente) throws RemoteException;

    /**
     * Cambia la proiezione associata a una prenotazione.
     *
     * @throws ServiceException se la vecchia o la nuova proiezione non sono
     *      entrambe future, o se i posti non sono disponibili sulla nuova proiezione
     */
    void modificaPrenotazione(String codicePrenotazione, long nuovaIdProiezione)
            throws RemoteException, ServiceException;

    /**
     * Elimina (disdice) una prenotazione.
     *
     * @throws ServiceException se la prenotazione non esiste
     */
    void eliminaPrenotazione(String codicePrenotazione) throws RemoteException, ServiceException;

    /** Prenotazioni relative a proiezioni della data odierna — vista bigliettaio. */
    List<Prenotazione> prenotazioniOdierne() throws RemoteException;

    /**
     * Ricerca combinabile per codice, nome/cognome cliente, titolo film,
     * intervallo di date — vista bigliettaio. Ogni criterio e' opzionale.
     */
    List<Prenotazione> cercaPrenotazioni(String codice, String nomeCognome, String titoloFilm,
                                          LocalDate dataDa, LocalDate dataA) throws RemoteException;

    /** Dettaglio di una prenotazione dato il codice — vista bigliettaio. */
    Prenotazione visualizzaPrenotazione(String codicePrenotazione)
            throws RemoteException, ServiceException;
}
