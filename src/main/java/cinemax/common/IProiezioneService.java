package cinemax.common;

import java.math.BigDecimal;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Servizio di consultazione e gestione del palinsesto (film e proiezioni).
 * Le operazioni di ricerca/consultazione sono accessibili anche a utenti
 * non autenticati (guest); le operazioni di gestione sono riservate al
 * ruolo {@code proiezionista} (il controllo di autorizzazione e' applicato
 * lato client in base all'utente loggato, e ribadito lato server).
 */
public interface IProiezioneService extends Remote {

    /**
     * Ricerca combinabile per titolo (parziale), genere, intervallo di date,
     * intervallo di costo. Ogni criterio e' opzionale ({@code null} = ignorato).
     */
    List<Proiezione> cercaProiezioni(String titolo, String genere, LocalDate dataDa,
                                      LocalDate dataA, BigDecimal costoMin, BigDecimal costoMax)
            throws RemoteException;

    /** Dettaglio di una proiezione (schermata di dettaglio dopo la ricerca). */
    Proiezione visualizzaProiezione(long idProiezione) throws RemoteException, ServiceException;

    /**
     * Proiezioni nei tre mesi successivi a oggi per un film (titolo anche
     * parziale) — schermata iniziale per l'utente guest.
     */
    List<Proiezione> proiezioniProssimiTreMesi(String titoloParziale) throws RemoteException;

    /** Ricerca nel catalogo film (usata dal proiezionista per evitare doppioni). */
    List<Film> cercaFilm(String titoloParziale) throws RemoteException;

    /**
     * Inserisce un nuovo film a catalogo.
     *
     * @return l'id assegnato al film
     */
    long aggiungiFilm(String titolo, String genere, String regista, int anno,
                       int durataMinuti, int etaMinima) throws RemoteException, ServiceException;

    /**
     * Inserisce una nuova proiezione per un film gia' a catalogo.
     *
     * @return l'id assegnato alla proiezione
     * @throws ServiceException se la proiezione si sovrappone a una gia' esistente (sala unica)
     */
    long aggiungiProiezione(long idFilm, LocalDate data, LocalTime ora, BigDecimal costoBiglietto)
            throws RemoteException, ServiceException;

    /**
     * Modifica data/ora/costo di una proiezione.
     *
     * @throws ServiceException se la proiezione ha gia' prenotazioni associate,
     *      oppure se il nuovo orario si sovrappone a un'altra proiezione
     */
    void modificaProiezione(long idProiezione, LocalDate nuovaData, LocalTime nuovaOra,
                             BigDecimal nuovoCosto) throws RemoteException, ServiceException;

    /**
     * Elimina una proiezione.
     *
     * @throws ServiceException se la proiezione ha gia' prenotazioni associate
     */
    void eliminaProiezione(long idProiezione) throws RemoteException, ServiceException;

    /** Proiezioni pianificate (successive a oggi) — vista proiezionista. */
    List<Proiezione> proiezioniPianificate() throws RemoteException;

    /** Proiezioni storiche (precedenti a oggi) — vista proiezionista. */
    List<Proiezione> proiezioniStoriche() throws RemoteException;
}
