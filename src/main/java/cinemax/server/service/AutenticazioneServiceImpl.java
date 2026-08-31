package cinemax.server.service;

import cinemax.common.Cliente;
import cinemax.common.IAutenticazioneService;
import cinemax.common.ServiceException;
import cinemax.common.Utente;
import cinemax.server.dao.UtenteDAO;
import cinemax.server.dao.UtenteRow;
import cinemax.server.factory.UtenteFactory;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLException;
import java.time.LocalDate;

/**
 * Questa classe implementa il servizio RMI di autenticazione (interfaccia
 * IAutenticazioneService). Si occupa di registrazione e login degli utenti.
 *
 * Non contiene direttamente istruzioni SQL: per parlare con il database usa
 * la classe UtenteDAO, mentre per costruire l'oggetto Utente giusto (Cliente,
 * Impiegato, ecc.) usa la UtenteFactory.
 */
public class AutenticazioneServiceImpl extends UnicastRemoteObject implements IAutenticazioneService {

    private final UtenteDAO utenteDAO;

    public AutenticazioneServiceImpl(UtenteDAO utenteDAO) throws RemoteException {
        super();
        this.utenteDAO = utenteDAO;
    }

    @Override
    public Cliente registraCliente(String nome, String cognome, String username, String password,
                                    LocalDate dataNascita, String luogoDomicilio)
            throws RemoteException, ServiceException {
        // Prima di tutto controlliamo che i campi obbligatori siano stati
        // compilati. Meglio bloccare subito qui piuttosto che arrivare fino
        // al database e scoprire l'errore troppo tardi.
        validaCampoObbligatorio(nome, "nome");
        validaCampoObbligatorio(cognome, "cognome");
        validaCampoObbligatorio(username, "username");
        validaCampoObbligatorio(luogoDomicilio, "luogo di domicilio");

        // Regola scelta per il progetto: la password deve avere almeno 6
        // caratteri, altrimenti la registrazione viene rifiutata subito.
        if (password == null || password.length() < 6) {
            throw new ServiceException("La password deve contenere almeno 6 caratteri");
        }

        try {
            // Controllo applicativo: verifichiamo se lo username e' gia'
            // usato da qualcun altro. Questo controllo non basta da solo
            // (due richieste potrebbero arrivare quasi insieme), per questo
            // sul database c'e' comunque un vincolo UNIQUE su username che
            // fa da rete di sicurezza (vedi il catch piu' sotto).
            if (utenteDAO.existsUsername(username)) {
                throw new ServiceException("Lo username '" + username + "' e' gia' in uso");
            }
            long idUtente = utenteDAO.inserisciCliente(nome, cognome, username, password, dataNascita, luogoDomicilio);
            return new Cliente(idUtente, nome, cognome, username, dataNascita, luogoDomicilio);
        } catch (SQLException e) {
            // Se e' scattato proprio il vincolo UNIQUE sullo username (caso
            // raro della corsa critica di cui sopra), diamo lo stesso
            // messaggio "amichevole" invece di un errore generico di database.
            if (isViolazioneUsernameUnivoco(e)) {
                throw new ServiceException("Lo username '" + username + "' e' gia' in uso");
            }
            // Negli altri casi logghiamo l'errore sul server e rilanciamo
            // una ServiceException "pulita" (senza passare la SQLException
            // come causa: e' voluto, altrimenti RMI avrebbe problemi a
            // deserializzare l'eccezione lato client).
            cinemax.server.LogUtil.erroreDb("registraCliente", e);
            throw new ServiceException("Errore durante la registrazione: " + e.getMessage());
        }
    }

    @Override
    public Utente login(String username, String password) throws RemoteException, ServiceException {
        try {
            // Il DAO fa il controllo su username e password e restituisce
            // null se le credenziali non sono corrette (non lancia
            // eccezioni per un semplice "utente/password sbagliati").
            UtenteRow row = utenteDAO.autentica(username, password);
            if (row == null) {
                throw new ServiceException("Username o password non validi");
            }
            // A partire dalla riga letta dal database, la factory capisce
            // che tipo di utente e' (cliente, impiegato...) e crea
            // l'oggetto giusto da restituire al client.
            return UtenteFactory.creaUtente(row);
        } catch (SQLException e) {
            cinemax.server.LogUtil.erroreDb("login", e);
            throw new ServiceException("Errore durante l'autenticazione: " + e.getMessage());
        }
    }

    // Metodo di appoggio: controlla che una stringa non sia null e non sia
    // vuota/fatta solo di spazi. Usato per i campi obbligatori della
    // registrazione.
    private void validaCampoObbligatorio(String valore, String nomeCampo) throws ServiceException {
        if (valore == null || valore.isBlank()) {
            throw new ServiceException("Il campo '" + nomeCampo + "' e' obbligatorio");
        }
    }

    /** SQLState 23505 = unique_violation (vincolo UNIQUE su utenti.username). */
    private boolean isViolazioneUsernameUnivoco(SQLException e) {
        return "23505".equals(e.getSQLState());
    }
}
