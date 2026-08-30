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
 * Implementazione RMI di {@link IAutenticazioneService} (Facade lato server,
 * vedi {@code doc/03_progettazione_uml.md} §2). Non contiene SQL: delega a
 * {@link UtenteDAO} e a {@link UtenteFactory}.
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
        validaCampoObbligatorio(nome, "nome");
        validaCampoObbligatorio(cognome, "cognome");
        validaCampoObbligatorio(username, "username");
        validaCampoObbligatorio(luogoDomicilio, "luogo di domicilio");
        if (password == null || password.length() < 6) {
            throw new ServiceException("La password deve contenere almeno 6 caratteri");
        }
        try {
            if (utenteDAO.existsUsername(username)) {
                throw new ServiceException("Lo username '" + username + "' e' gia' in uso");
            }
            long idUtente = utenteDAO.inserisciCliente(nome, cognome, username, password, dataNascita, luogoDomicilio);
            return new Cliente(idUtente, nome, cognome, username, dataNascita, luogoDomicilio);
        } catch (SQLException e) {
            if (isViolazioneUsernameUnivoco(e)) {
                throw new ServiceException("Lo username '" + username + "' e' gia' in uso");
            }
            cinemax.server.LogUtil.erroreDb("registraCliente", e);
            throw new ServiceException("Errore durante la registrazione: " + e.getMessage());
        }
    }

    @Override
    public Utente login(String username, String password) throws RemoteException, ServiceException {
        try {
            UtenteRow row = utenteDAO.autentica(username, password);
            if (row == null) {
                throw new ServiceException("Username o password non validi");
            }
            return UtenteFactory.creaUtente(row);
        } catch (SQLException e) {
            cinemax.server.LogUtil.erroreDb("login", e);
            throw new ServiceException("Errore durante l'autenticazione: " + e.getMessage());
        }
    }

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
