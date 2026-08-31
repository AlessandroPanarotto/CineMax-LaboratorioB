package cinemax.server.service;

import cinemax.common.Film;
import cinemax.common.IProiezioneService;
import cinemax.common.Proiezione;
import cinemax.common.ServiceException;
import cinemax.server.LogUtil;
import cinemax.server.dao.FilmDAO;
import cinemax.server.dao.ProiezioneDAO;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Implementazione del servizio RMI per la gestione di film e proiezioni
 * (interfaccia IProiezioneService). Come le altre classi *ServiceImpl,
 * non scrive query SQL direttamente ma usa i DAO (ProiezioneDAO e FilmDAO).
 *
 * Attenzione: quando catturiamo una SQLException NON la mettiamo mai come
 * causa dentro l'eccezione che rilanciamo verso il client. E' voluto: il
 * driver JDBC non e' presente sul classpath del client, quindi se provassimo
 * a mandare quella causa attraverso RMI il client non riuscirebbe a
 * deserializzarla e otterremmo un errore ancora peggiore. Per questo l'errore
 * viene solo loggato sul server con LogUtil, e al client arriva un messaggio
 * pulito.
 */
public class ProiezioneServiceImpl extends UnicastRemoteObject implements IProiezioneService {

    private final ProiezioneDAO proiezioneDAO;
    private final FilmDAO filmDAO;

    public ProiezioneServiceImpl(ProiezioneDAO proiezioneDAO, FilmDAO filmDAO) throws RemoteException {
        super();
        this.proiezioneDAO = proiezioneDAO;
        this.filmDAO = filmDAO;
    }

    @Override
    public List<Proiezione> cercaProiezioni(String titolo, String genere, LocalDate dataDa, LocalDate dataA,
                                             BigDecimal costoMin, BigDecimal costoMax) throws RemoteException {
        // Ricerca con filtri opzionali: se un parametro e' null il DAO lo
        // ignora e non lo usa nella query (non e' compito nostro costruire
        // la query, ci pensa il DAO).
        try {
            return proiezioneDAO.cerca(titolo, genere, dataDa, dataA, costoMin, costoMax);
        } catch (SQLException e) {
            LogUtil.erroreDb("cercaProiezioni", e);
            throw new RemoteException("Errore durante la ricerca delle proiezioni");
        }
    }

    @Override
    public Proiezione visualizzaProiezione(long idProiezione) throws RemoteException, ServiceException {
        try {
            Proiezione p = proiezioneDAO.findById(idProiezione);
            // Il DAO restituisce null se non trova niente con quell'id,
            // quindi il controllo va fatto qui: trasformiamo il "non trovato"
            // in un errore chiaro per l'utente, invece di restituire null.
            if (p == null) {
                throw new ServiceException("Proiezione non trovata (id=" + idProiezione + ")");
            }
            return p;
        } catch (SQLException e) {
            LogUtil.erroreDb("visualizzaProiezione", e);
            throw new RemoteException("Errore durante la lettura della proiezione");
        }
    }

    @Override
    public List<Proiezione> proiezioniProssimiTreMesi(String titoloParziale) throws RemoteException {
        try {
            return proiezioneDAO.prossimiTreMesiPerFilm(titoloParziale);
        } catch (SQLException e) {
            LogUtil.erroreDb("proiezioniProssimiTreMesi", e);
            throw new RemoteException("Errore durante la ricerca delle proiezioni");
        }
    }

    @Override
    public List<Film> cercaFilm(String titoloParziale) throws RemoteException {
        try {
            return filmDAO.cercaPerTitolo(titoloParziale);
        } catch (SQLException e) {
            LogUtil.erroreDb("cercaFilm", e);
            throw new RemoteException("Errore durante la ricerca dei film");
        }
    }

    @Override
    public long aggiungiFilm(String titolo, String genere, String regista, int anno,
                              int durataMinuti, int etaMinima) throws RemoteException, ServiceException {
        // Validazioni minime lato applicazione, prima di andare sul
        // database: un film senza titolo o con durata zero/negativa non
        // avrebbe senso.
        if (titolo == null || titolo.isBlank()) {
            throw new ServiceException("Il titolo del film e' obbligatorio");
        }
        if (durataMinuti <= 0) {
            throw new ServiceException("La durata del film deve essere positiva");
        }
        try {
            return filmDAO.inserisci(titolo, genere, regista, anno, durataMinuti, etaMinima);
        } catch (SQLException e) {
            LogUtil.erroreDb("aggiungiFilm", e);
            throw new ServiceException("Errore durante l'inserimento del film: " + e.getMessage());
        }
    }

    @Override
    public long aggiungiProiezione(long idFilm, LocalDate data, LocalTime ora, BigDecimal costoBiglietto)
            throws RemoteException, ServiceException {
        // Il costo del biglietto deve essere un numero positivo, altrimenti
        // non ha senso e blocchiamo l'operazione prima ancora di controllare
        // il film.
        if (costoBiglietto == null || costoBiglietto.signum() <= 0) {
            throw new ServiceException("Il costo del biglietto deve essere positivo");
        }
        try {
            // Non possiamo creare una proiezione per un film che non esiste,
            // quindi controlliamo prima che l'id del film sia valido.
            if (filmDAO.findById(idFilm) == null) {
                throw new ServiceException("Film non trovato (id=" + idFilm + ")");
            }
            return proiezioneDAO.inserisci(idFilm, data, ora, costoBiglietto);
        } catch (SQLException e) {
            // Nota: qui puo' entrare in gioco anche il trigger del database
            // trg_sovrapposizione_proiezione, che impedisce di creare due
            // proiezioni sovrapposte nella stessa sala (il cinema ha una
            // sola sala). Questo controllo NON lo facciamo in Java: e' il
            // database stesso a bloccare l'inserimento e a far scattare
            // questo catch.
            LogUtil.erroreDb("aggiungiProiezione", e);
            throw new ServiceException("Impossibile aggiungere la proiezione: " + e.getMessage());
        }
    }

    @Override
    public void modificaProiezione(long idProiezione, LocalDate nuovaData, LocalTime nuovaOra, BigDecimal nuovoCosto)
            throws RemoteException, ServiceException {
        if (nuovoCosto == null || nuovoCosto.signum() <= 0) {
            throw new ServiceException("Il costo del biglietto deve essere positivo");
        }
        try {
            if (proiezioneDAO.findById(idProiezione) == null) {
                throw new ServiceException("Proiezione non trovata (id=" + idProiezione + ")");
            }
            proiezioneDAO.aggiorna(idProiezione, nuovaData, nuovaOra, nuovoCosto);
        } catch (SQLException e) {
            // Se la proiezione ha gia' delle prenotazioni collegate, il
            // trigger trg_proiezione_immutabile_update la blocca (regola di
            // business: una proiezione gia' prenotata da qualcuno non si
            // puo' piu' spostare di data/ora, altrimenti si romperebbero le
            // prenotazioni esistenti). Puo' scattare anche
            // trg_sovrapposizione_proiezione se il nuovo orario si sovrappone
            // con un'altra proiezione. In entrambi i casi arriviamo qui.
            LogUtil.erroreDb("modificaProiezione", e);
            throw new ServiceException("Impossibile modificare la proiezione: " + e.getMessage());
        }
    }

    @Override
    public void eliminaProiezione(long idProiezione) throws RemoteException, ServiceException {
        try {
            if (proiezioneDAO.findById(idProiezione) == null) {
                throw new ServiceException("Proiezione non trovata (id=" + idProiezione + ")");
            }
            proiezioneDAO.elimina(idProiezione);
        } catch (SQLException e) {
            // Stessa idea del metodo sopra: se esistono prenotazioni legate a
            // questa proiezione, il trigger trg_proiezione_immutabile_delete
            // impedisce la cancellazione (non avrebbe senso eliminare una
            // proiezione per cui dei clienti hanno gia' prenotato).
            LogUtil.erroreDb("eliminaProiezione", e);
            throw new ServiceException("Impossibile eliminare la proiezione: " + e.getMessage());
        }
    }

    @Override
    public List<Proiezione> proiezioniPianificate() throws RemoteException {
        // Proiezioni ancora da fare (data futura), usate ad esempio nella
        // schermata dell'impiegato per vedere il calendario del cinema.
        try {
            return proiezioneDAO.pianificate();
        } catch (SQLException e) {
            LogUtil.erroreDb("proiezioniPianificate", e);
            throw new RemoteException("Errore durante la lettura delle proiezioni pianificate");
        }
    }

    @Override
    public List<Proiezione> proiezioniStoriche() throws RemoteException {
        // Proiezioni gia' passate, tenute per lo storico.
        try {
            return proiezioneDAO.storiche();
        } catch (SQLException e) {
            LogUtil.erroreDb("proiezioniStoriche", e);
            throw new RemoteException("Errore durante la lettura delle proiezioni storiche");
        }
    }
}
