package cinemax.server.db;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Punto unico di accesso al pool di connessioni verso PostgreSQL, condiviso
 * da tutti i DAO lato server (pattern <b>Singleton</b>: vedi
 * {@code doc/03_progettazione_uml.md}, §2). Necessario per gestire l'accesso
 * concorrente al database da parte di piu' client RMI simultanei, dato che un
 * singolo {@link Connection} JDBC non e' thread-safe.
 *
 * <p>Il pool e' un piccolo pool "fatto in casa" (senza dipendenze esterne):
 * un numero fisso di connessioni fisiche viene aperto all'avvio; ogni
 * connessione restituita da {@link #getConnection()} e' in realta' un proxy
 * dinamico (pattern <b>Proxy</b>, come lo stub RMI) che intercetta la chiamata
 * a {@code close()} e restituisce la connessione fisica al pool invece di
 * chiuderla davvero — cosi' il codice dei DAO puo' usare normalmente
 * try-with-resources senza sapere che la connessione e' "riciclata".</p>
 */
public final class ConnectionManager {

    private static volatile ConnectionManager instance;

    private final BlockingQueue<Connection> pool;
    private final List<Connection> tutteLeConnessioni;

    private ConnectionManager(String url, String user, String password, int poolSize) throws SQLException {
        this.pool = new LinkedBlockingQueue<>(poolSize);
        this.tutteLeConnessioni = new ArrayList<>(poolSize);
        for (int i = 0; i < poolSize; i++) {
            Connection physical = DriverManager.getConnection(url, user, password);
            tutteLeConnessioni.add(physical);
            pool.add(physical);
        }
    }

    /**
     * Inizializza il Singleton con i parametri di connessione al database.
     * Va chiamato una sola volta, all'avvio del server ({@code ServerMain}).
     *
     * @param poolSize numero di connessioni fisiche aperte all'avvio
     * @throws SQLException se una qualsiasi delle connessioni non puo' essere aperta
     * @throws IllegalStateException se e' gia' stato inizializzato
     */
    public static synchronized void initialize(String url, String user, String password, int poolSize)
            throws SQLException {
        if (instance != null) {
            throw new IllegalStateException("ConnectionManager gia' inizializzato");
        }
        instance = new ConnectionManager(url, user, password, poolSize);
    }

    /** @throws IllegalStateException se {@link #initialize} non e' ancora stato chiamato */
    public static ConnectionManager getInstance() {
        ConnectionManager result = instance;
        if (result == null) {
            throw new IllegalStateException(
                    "ConnectionManager non inizializzato: chiamare initialize() all'avvio del server");
        }
        return result;
    }

    /**
     * Preleva una connessione dal pool (in attesa fino a {@code timeoutSeconds}
     * se momentaneamente tutte occupate). La connessione restituita va chiusa
     * con {@code close()} appena non serve piu' (try-with-resources): non
     * viene chiusa fisicamente, torna semplicemente disponibile nel pool.
     */
    public Connection getConnection() throws SQLException {
        Connection physical;
        try {
            physical = pool.poll(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Interrotto in attesa di una connessione dal pool", e);
        }
        if (physical == null) {
            throw new SQLException("Timeout: nessuna connessione disponibile nel pool (troppi client concorrenti)");
        }
        return wrap(physical);
    }

    private Connection wrap(Connection physical) {
        InvocationHandler handler = (proxy, method, args) -> {
            if ("close".equals(method.getName()) && (args == null || args.length == 0)) {
                pool.offer(physical);
                return null;
            }
            try {
                return method.invoke(physical, args);
            } catch (java.lang.reflect.InvocationTargetException e) {
                throw e.getCause();
            }
        };
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(), new Class<?>[]{Connection.class}, handler);
    }

    /** Chiude fisicamente tutte le connessioni del pool (spegnimento del server). */
    public synchronized void shutdown() {
        for (Connection c : tutteLeConnessioni) {
            try {
                c.close();
            } catch (SQLException ignored) {
                // in fase di spegnimento non c'e' nulla di utile da fare con l'errore
            }
        }
    }
}
