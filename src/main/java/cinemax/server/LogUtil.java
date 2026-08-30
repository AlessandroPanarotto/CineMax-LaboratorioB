package cinemax.server;

/**
 * Log minimale lato server per le eccezioni SQL intercettate nei service RMI.
 *
 * <p>Le eccezioni del driver JDBC (es. {@code org.postgresql.util.PSQLException})
 * non devono mai attraversare la connessione RMI come "cause" di una
 * {@code ServiceException}/{@code RemoteException}: il client non ha (e non
 * deve avere) il driver PostgreSQL sul classpath, quindi la deserializzazione
 * fallirebbe con una {@code ClassNotFoundException} lato client. Il messaggio
 * testuale dell'eccezione viene comunque incluso nell'eccezione applicativa;
 * lo stack trace completo resta solo nel log del server.</p>
 */
public final class LogUtil {

    private LogUtil() {
    }

    public static void erroreDb(String contesto, Exception e) {
        System.err.println("[serverCM] " + contesto + ": " + e.getMessage());
        e.printStackTrace();
    }
}
