package cinemax.common;

/**
 * Eccezione applicativa sollevata dai servizi RMI di CineMax per segnalare
 * un errore "atteso" (credenziali non valide, posti insufficienti, username
 * gia' in uso, operazione non consentita, ...) da mostrare all'utente cosi'
 * com'e', a differenza di un {@link java.rmi.RemoteException} che segnala
 * un problema di infrastruttura (rete, server irraggiungibile, ...).
 *
 * <p>Checked, cosi' come {@code RemoteException}: ogni metodo delle
 * interfacce {@code cinemax.common.I*Service} la dichiara esplicitamente
 * dove puo' verificarsi.</p>
 */
public class ServiceException extends Exception {

    private static final long serialVersionUID = 1L;

    public ServiceException(String message) {
        super(message);
    }

    public ServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
