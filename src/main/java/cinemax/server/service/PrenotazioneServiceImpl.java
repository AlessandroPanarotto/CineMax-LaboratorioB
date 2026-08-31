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
 * Implementazione del servizio RMI per le prenotazioni (interfaccia
 * IPrenotazioneService). Anche qui, come nelle altre classi *ServiceImpl,
 * tutta la parte SQL vera e propria e' delegata ai DAO (PrenotazioneDAO e
 * ProiezioneDAO): questa classe si occupa solo dei controlli applicativi e
 * di tradurre gli errori del database in messaggi comprensibili per il
 * client.
 *
 * Come in ProiezioneServiceImpl, quando catturiamo una SQLException non la
 * passiamo come causa dell'eccezione rilanciata: e' un accorgimento voluto
 * per evitare problemi di deserializzazione lato client via RMI (il driver
 * JDBC non e' sul classpath del client).
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
            // Regola di business: si puo' prenotare solo se ci sono
            // abbastanza posti liberi. Questo e' solo un primo controllo
            // fatto lato applicazione (Java): il controllo "vero", quello
            // definitivo, e' comunque rifatto dal database tramite il
            // trigger trg_capienza_sala. Serve perche' potrebbero esserci
            // piu' client collegati contemporaneamente, e tra il momento in
            // cui leggiamo i posti liberi qui e il momento in cui scriviamo
            // la prenotazione, un altro client potrebbe aver gia' occupato
            // quei posti (vedi doc/uml/sequenza_prenotazione.puml). Per
            // questo il controllo va fatto due volte: qui per dare subito un
            // messaggio d'errore chiaro, e sul database per essere sicuri al
            // 100% che non si superi mai la capienza della sala.
            if (proiezione.getPostiLiberi() < numPosti) {
                throw new ServiceException("Posti non disponibili: richiesti " + numPosti +
                        ", disponibili " + proiezione.getPostiLiberi());
            }
            return prenotazioneDAO.inserisci(idUtente, idProiezione, numPosti);
        } catch (SQLException e) {
            // Qui puo' arrivare l'errore del trigger trg_ruolo_cliente (se
            // l'utente che sta prenotando non e' un cliente, ad esempio e'
            // un impiegato) oppure di trg_capienza_sala nel caso raro
            // descritto sopra (due prenotazioni quasi simultanee che
            // superano insieme la capienza).
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
            // Prima recuperiamo la data della proiezione a cui e' legata la
            // prenotazione attuale, cosi' possiamo controllare che sia
            // ancora una proiezione futura: non avrebbe senso spostare una
            // prenotazione che riguarda una proiezione gia' passata.
            LocalDate vecchiaData = prenotazioneDAO.dataProiezioneDiPrenotazione(codicePrenotazione);
            if (vecchiaData == null) {
                throw new ServiceException("Prenotazione non trovata (codice=" + codicePrenotazione + ")");
            }
            if (!vecchiaData.isAfter(LocalDate.now())) {
                throw new ServiceException("Impossibile modificare: la proiezione originale non e' piu' futura");
            }

            // Allo stesso modo, anche la nuova proiezione scelta deve
            // esistere e deve essere futura: non si puo' spostare una
            // prenotazione su una proiezione gia' avvenuta.
            Proiezione nuovaProiezione = proiezioneDAO.findById(nuovaIdProiezione);
            if (nuovaProiezione == null) {
                throw new ServiceException("Proiezione di destinazione non trovata (id=" + nuovaIdProiezione + ")");
            }
            if (!nuovaProiezione.getDataProiezione().isAfter(LocalDate.now())) {
                throw new ServiceException("La nuova proiezione scelta deve essere futura");
            }
            prenotazioneDAO.aggiornaProiezione(codicePrenotazione, nuovaIdProiezione);
        } catch (SQLException e) {
            // Anche qui la capienza della nuova proiezione viene ricontrollata
            // dal database: se il trigger trg_capienza_sala si accorge che
            // non ci sono abbastanza posti liberi nella nuova proiezione,
            // l'aggiornamento viene rifiutato e finiamo in questo catch.
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
            // ATTENZIONE, leggere bene prima di modificare questa parte:
            // la specifica del progetto (slide 12) dice che si puo'
            // eliminare una prenotazione solo se la data della proiezione
            // e' PRECEDENTE alla data odierna. La condizione qui sotto e'
            // implementata esattamente cosi' come dice la specifica, anche
            // se a prima vista sembra al contrario di quello che ci si
            // aspetterebbe (di solito una prenotazione si disdice PRIMA
            // della proiezione, non dopo che e' gia' passata). Non e' un
            // errore: e' stato lasciato cosi' apposta, seguendo la
            // specifica alla lettera, e va verificato col docente. Per i
            // dettagli vedi doc/01_progettazione_database.md §6.2.
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
        // Prenotazioni relative a proiezioni di oggi, usate ad esempio dagli
        // impiegati per sapere chi si presentera' in cinema nella giornata.
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
        // Ricerca con piu' filtri opzionali: se un parametro e' null il DAO
        // semplicemente non lo applica nella query.
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
