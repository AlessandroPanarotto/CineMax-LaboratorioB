package cinemax.common;

/**
 * Eccezione usata dai servizi RMI di CineMax per segnalare un errore
 * "normale" dell'applicazione, cioe' una cosa che ci si puo' aspettare
 * possa succedere (per esempio: credenziali sbagliate, posti non
 * disponibili, username gia' usato, operazione non permessa...).
 * Il messaggio viene mostrato direttamente all'utente.
 *
 * E' diversa da RemoteException, che invece indica un problema tecnico
 * di rete o del server (quello e' un errore "imprevisto").
 *
 * E' una checked exception (come RemoteException): infatti nelle
 * interfacce I*Service ogni metodo che puo' lanciarla lo dichiara con
 * "throws ServiceException".
 */
public class ServiceException extends Exception {

    private static final long serialVersionUID = 1L;

    public ServiceException(String message) {
        super(message);
    }

    // costruttore usato quando l'errore nasconde una causa tecnica sottostante (es. eccezione SQL)
    public ServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
