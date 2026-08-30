package cinemax.common;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.time.LocalDate;

/**
 * Servizio di registrazione/autenticazione, pubblicato dal server sull'RMI
 * registry. Facciata (pattern Facade, vedi {@code doc/03_progettazione_uml.md})
 * verso il client: nasconde DAO e logica di hashing password.
 */
public interface IAutenticazioneService extends Remote {

    /**
     * Registra un nuovo utente con ruolo {@code cliente}.
     *
     * @param dataNascita facoltativa, puo' essere {@code null}
     * @throws ServiceException se lo username e' gia' in uso
     */
    Cliente registraCliente(String nome, String cognome, String username, String password,
                             LocalDate dataNascita, String luogoDomicilio)
            throws RemoteException, ServiceException;

    /**
     * Verifica le credenziali e restituisce l'utente autenticato (nella
     * sottoclasse corrispondente al suo ruolo).
     *
     * @throws ServiceException se username/password non sono validi
     */
    Utente login(String username, String password) throws RemoteException, ServiceException;
}
