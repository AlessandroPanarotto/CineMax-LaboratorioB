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
 * Implementazione RMI di {@link IProiezioneService}. Non contiene SQL: delega ai DAO.
 *
 * <p>Nota implementativa: le {@link SQLException} intercettate non vengono mai
 * incluse come "cause" delle eccezioni rilanciate (vedi {@link LogUtil}), per
 * evitare che una classe specifica del driver JDBC (assente sul classpath del
 * client) debba essere deserializzata attraverso la connessione RMI.</p>
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
        if (costoBiglietto == null || costoBiglietto.signum() <= 0) {
            throw new ServiceException("Il costo del biglietto deve essere positivo");
        }
        try {
            if (filmDAO.findById(idFilm) == null) {
                throw new ServiceException("Film non trovato (id=" + idFilm + ")");
            }
            return proiezioneDAO.inserisci(idFilm, data, ora, costoBiglietto);
        } catch (SQLException e) {
            // trg_sovrapposizione_proiezione solleva qui in caso di conflitto (sala unica)
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
            // trg_proiezione_immutabile_update (prenotazioni associate) o
            // trg_sovrapposizione_proiezione sollevano qui
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
            // trg_proiezione_immutabile_delete solleva qui se esistono prenotazioni
            LogUtil.erroreDb("eliminaProiezione", e);
            throw new ServiceException("Impossibile eliminare la proiezione: " + e.getMessage());
        }
    }

    @Override
    public List<Proiezione> proiezioniPianificate() throws RemoteException {
        try {
            return proiezioneDAO.pianificate();
        } catch (SQLException e) {
            LogUtil.erroreDb("proiezioniPianificate", e);
            throw new RemoteException("Errore durante la lettura delle proiezioni pianificate");
        }
    }

    @Override
    public List<Proiezione> proiezioniStoriche() throws RemoteException {
        try {
            return proiezioneDAO.storiche();
        } catch (SQLException e) {
            LogUtil.erroreDb("proiezioniStoriche", e);
            throw new RemoteException("Errore durante la lettura delle proiezioni storiche");
        }
    }
}
