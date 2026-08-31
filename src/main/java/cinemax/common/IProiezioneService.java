package cinemax.common;

import java.math.BigDecimal;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Interfaccia remota per gestire il palinsesto, cioe' film e proiezioni.
 * Le ricerche sono disponibili anche senza essere loggati (utente guest);
 * le operazioni che modificano qualcosa (aggiungere/modificare/eliminare)
 * sono riservate al proiezionista. Il client nasconde questi pulsanti se
 * l'utente non e' un proiezionista, ma il controllo vero e proprio va
 * comunque rifatto anche lato server.
 */
public interface IProiezioneService extends Remote {

    /**
     * Cerca le proiezioni in base ai criteri passati: titolo (anche
     * parziale), genere, intervallo di date, intervallo di costo. Ogni
     * parametro e' facoltativo: se e' null quel criterio viene ignorato.
     */
    List<Proiezione> cercaProiezioni(String titolo, String genere, LocalDate dataDa,
                                      LocalDate dataA, BigDecimal costoMin, BigDecimal costoMax)
            throws RemoteException;

    /** Restituisce i dettagli di una singola proiezione dato il suo id. */
    Proiezione visualizzaProiezione(long idProiezione) throws RemoteException, ServiceException;

    /**
     * Restituisce le proiezioni dei prossimi tre mesi per un film (il
     * titolo puo' essere parziale). E' la schermata che vede l'utente
     * guest appena apre l'applicazione.
     */
    List<Proiezione> proiezioniProssimiTreMesi(String titoloParziale) throws RemoteException;

    /** Cerca nel catalogo film per titolo parziale (usata dal proiezionista per non inserire doppioni). */
    List<Film> cercaFilm(String titoloParziale) throws RemoteException;

    /**
     * Aggiunge un nuovo film al catalogo.
     *
     * @return l'id assegnato al nuovo film
     */
    long aggiungiFilm(String titolo, String genere, String regista, int anno,
                       int durataMinuti, int etaMinima) throws RemoteException, ServiceException;

    /**
     * Aggiunge una nuova proiezione per un film gia' presente nel catalogo.
     *
     * @return l'id assegnato alla nuova proiezione
     * @throws ServiceException se l'orario scelto si sovrappone a un'altra proiezione gia' presente (c'e' una sola sala)
     */
    long aggiungiProiezione(long idFilm, LocalDate data, LocalTime ora, BigDecimal costoBiglietto)
            throws RemoteException, ServiceException;

    /**
     * Modifica data, ora e/o costo di una proiezione gia' esistente.
     *
     * @throws ServiceException se la proiezione ha gia' delle prenotazioni
     *      (non si puo' piu' cambiare) oppure se il nuovo orario si
     *      sovrappone a un'altra proiezione
     */
    void modificaProiezione(long idProiezione, LocalDate nuovaData, LocalTime nuovaOra,
                             BigDecimal nuovoCosto) throws RemoteException, ServiceException;

    /**
     * Elimina una proiezione dal palinsesto.
     *
     * @throws ServiceException se la proiezione ha gia' delle prenotazioni
     */
    void eliminaProiezione(long idProiezione) throws RemoteException, ServiceException;

    /** Restituisce le proiezioni ancora da fare, cioe' successive a oggi (vista del proiezionista). */
    List<Proiezione> proiezioniPianificate() throws RemoteException;

    /** Restituisce le proiezioni gia' passate, cioe' precedenti a oggi (vista del proiezionista). */
    List<Proiezione> proiezioniStoriche() throws RemoteException;
}
