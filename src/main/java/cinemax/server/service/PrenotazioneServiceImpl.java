package cinemax.server.service;

import cinemax.common.IPrenotazioneService;
import cinemax.common.Prenotazione;
import cinemax.common.Proiezione;
import cinemax.common.ServiceException;
import cinemax.server.LogUtil;
import cinemax.server.dao.PrenotazioneDAO;
import cinemax.server.dao.ProiezioneDAO;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Implementazione RMI di {@link IPrenotazioneService}. Non contiene SQL: delega ai DAO.
 *
 * <p>Come in {@link ProiezioneServiceImpl}, le {@link SQLException} non vengono
 * mai propagate come "cause" attraverso RMI (vedi {@link LogUtil}).</p>
 */
public class PrenotazioneServiceImpl extends UnicastRemoteObject implements IPrenotazioneService {

    private final PrenotazioneDAO prenotazioneDAO;
    private final ProiezioneDAO proiezioneDAO;

    public PrenotazioneServiceImpl(PrenotazioneDAO prenotazioneDAO, ProiezioneDAO proiezioneDAO)
            throws RemoteException {
        super();
        this.prenotazioneDAO = prenotazioneDAO;
        this.proiezioneDAO = proiezioneDAO;
    }

    @Override
    public String creaPrenotazione(long idUtente, long idProiezione, int numPosti)
            throws RemoteException, ServiceException {
        if (numPosti <= 0) {
            throw new ServiceException("Il numero di posti richiesti deve essere positivo");
        }
        try {
            Proiezione proiezione = proiezioneDAO.findById(idProiezione);
            if (proiezione == null) {
                throw new ServiceException("Proiezione non trovata (id=" + idProiezione + ")");
            }
            // Controllo applicativo preliminare: e' comunque ribadito a livello
            // di database dal trigger trg_capienza_sala (difesa in profondita',
            // indispensabile in presenza di piu' client concorrenti — vedi
            // doc/uml/sequenza_prenotazione.puml).
            if (proiezione.getPostiLiberi() < numPosti) {
                throw new ServiceException("Posti non disponibili: richiesti " + numPosti +
                        ", disponibili " + proiezione.getPostiLiberi());
            }
            return prenotazioneDAO.inserisci(idUtente, idProiezione, numPosti);
        } catch (SQLException e) {
            // trg_ruolo_cliente (utente non cliente) o trg_capienza_sala
            // (corsa critica fra due client) sollevano qui.
            LogUtil.erroreDb("creaPrenotazione", e);
            throw new ServiceException("Impossibile creare la prenotazione: " + e.getMessage());
        }
    }

    @Override
    public List<Prenotazione> visualizzaPrenotazioni(long idUtente) throws RemoteException {
        try {
            return prenotazioneDAO.findByUtente(idUtente);
        } catch (SQLException e) {
            LogUtil.erroreDb("visualizzaPrenotazioni", e);
            throw new RemoteException("Errore durante la lettura delle prenotazioni");
        }
    }

    @Override
    public void modificaPrenotazione(String codicePrenotazione, long nuovaIdProiezione)
            throws RemoteException, ServiceException {
        try {
            LocalDate vecchiaData = prenotazioneDAO.dataProiezioneDiPrenotazione(codicePrenotazione);
            if (vecchiaData == null) {
                throw new ServiceException("Prenotazione non trovata (codice=" + codicePrenotazione + ")");
            }
            if (!vecchiaData.isAfter(LocalDate.now())) {
                throw new ServiceException("Impossibile modificare: la proiezione originale non e' piu' futura");
            }
            Proiezione nuovaProiezione = proiezioneDAO.findById(nuovaIdProiezione);
            if (nuovaProiezione == null) {
                throw new ServiceException("Proiezione di destinazione non trovata (id=" + nuovaIdProiezione + ")");
            }
            if (!nuovaProiezione.getDataProiezione().isAfter(LocalDate.now())) {
                throw new ServiceException("La nuova proiezione scelta deve essere futura");
            }
            prenotazioneDAO.aggiornaProiezione(codicePrenotazione, nuovaIdProiezione);
        } catch (SQLException e) {
            // trg_capienza_sala solleva qui se la nuova proiezione non ha posti sufficienti
            LogUtil.erroreDb("modificaPrenotazione", e);
            throw new ServiceException("Impossibile modificare la prenotazione: " + e.getMessage());
        }
    }

    @Override
    public void eliminaPrenotazione(String codicePrenotazione) throws RemoteException, ServiceException {
        try {
            LocalDate dataProiezione = prenotazioneDAO.dataProiezioneDiPrenotazione(codicePrenotazione);
            if (dataProiezione == null) {
                throw new ServiceException("Prenotazione non trovata (codice=" + codicePrenotazione + ")");
            }
            // NOTA: la specifica di progetto (slide 12) condiziona la
            // cancellazione a "data di proiezione precedente alla data
            // odierna" — condizione implementata qui alla lettera, pur
            // apparendo invertita rispetto alla logica applicativa attesa
            // (di norma si disdice una prenotazione PRIMA della proiezione,
            // non dopo). Segnalato per verifica col docente, vedi
            // doc/01_progettazione_database.md §6.2.
            if (!dataProiezione.isBefore(LocalDate.now())) {
                throw new ServiceException(
                        "Impossibile eliminare: per specifica, la cancellazione e' ammessa solo per " +
                        "prenotazioni relative a proiezioni con data precedente a quella odierna");
            }
            prenotazioneDAO.elimina(codicePrenotazione);
        } catch (SQLException e) {
            LogUtil.erroreDb("eliminaPrenotazione", e);
            throw new ServiceException("Impossibile eliminare la prenotazione: " + e.getMessage());
        }
    }

    @Override
    public List<Prenotazione> prenotazioniOdierne() throws RemoteException {
        try {
            return prenotazioneDAO.odierne();
        } catch (SQLException e) {
            LogUtil.erroreDb("prenotazioniOdierne", e);
            throw new RemoteException("Errore durante la lettura delle prenotazioni odierne");
        }
    }

    @Override
    public List<Prenotazione> cercaPrenotazioni(String codice, String nomeCognome, String titoloFilm,
                                                 LocalDate dataDa, LocalDate dataA) throws RemoteException {
        try {
            return prenotazioneDAO.cerca(codice, nomeCognome, titoloFilm, dataDa, dataA);
        } catch (SQLException e) {
            LogUtil.erroreDb("cercaPrenotazioni", e);
            throw new RemoteException("Errore durante la ricerca delle prenotazioni");
        }
    }

    @Override
    public Prenotazione visualizzaPrenotazione(String codicePrenotazione) throws RemoteException, ServiceException {
        try {
            Prenotazione p = prenotazioneDAO.findByCodice(codicePrenotazione);
            if (p == null) {
                throw new ServiceException("Prenotazione non trovata (codice=" + codicePrenotazione + ")");
            }
            return p;
        } catch (SQLException e) {
            LogUtil.erroreDb("visualizzaPrenotazione", e);
            throw new RemoteException("Errore durante la lettura della prenotazione");
        }
    }
}
