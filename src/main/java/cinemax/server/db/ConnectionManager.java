/*
 * Progetto CineMax - Laboratorio Interdisciplinare B (a.a. 2025/2026)
 * Universita' degli Studi dell'Insubria
 *
 * Autori:
 *   Panarotto Alessandro   - matricola 757930 - sede di Varese (VA)
 *   Calabrese Davide Paolo - matricola 763012 - sede di Varese (VA)
 *   Mohan Thomas Paolo     - matricola 761573 - sede di Varese (VA)
 *   Trentini Federico      - matricola 760478 - sede di Varese (VA)
 */
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
 * Gestisce l'accesso al database PostgreSQL per tutto il server, mettendo
 * insieme due design pattern visti a lezione: Singleton e Proxy.
 *
 * PERCHE' SINGLETON: tutti i DAO devono usare lo stesso pool di connessioni,
 * non uno a testa. Se ogni DAO aprisse le proprie connessioni non avremmo
 * nessun controllo su quante connessioni totali sono aperte verso il
 * database, e con piu' client RMI collegati contemporaneamente rischieremmo
 * di aprirne troppe. Con il Singleton c'e' una sola istanza di
 * ConnectionManager in tutto il programma, creata una volta sola
 * all'avvio del server (in ServerMain) e recuperata da tutti i DAO con
 * getInstance().
 *
 * PERCHE' UN POOL: aprire una connessione fisica verso PostgreSQL e' un'
 * operazione costosa (handshake di rete, autenticazione, ecc.). Aprirne e
 * chiuderne una ad ogni singola query sarebbe molto lento. Il pool invece
 * apre un numero fisso di connessioni una sola volta all'avvio, e poi le
 * "presta" ai DAO che ne fanno richiesta: quando un DAO ha finito, la
 * connessione torna disponibile per il prossimo che la chiede, senza essere
 * mai chiusa davvero (finche' il server non si spegne).
 *
 * PERCHE' UN PROXY: il problema e' che il codice dei DAO e' scritto con il
 * classico try-with-resources su una Connection JDBC:
 *
 *     try (Connection conn = ...) { ... }
 *
 * e a fine blocco Java chiama automaticamente conn.close(). Se close()
 * chiudesse davvero la connessione fisica, la perderemmo per sempre dopo il
 * primo utilizzo, e il pool si svuoterebbe subito. La soluzione e' non dare
 * mai ai DAO la connessione fisica vera, ma un "proxy": un oggetto che dal
 * punto di vista del codice chiamante si comporta esattamente come una
 * Connection (implementa la stessa interfaccia), ma che intercetta solo la
 * chiamata a close() e, invece di chiudere per davvero, restituisce la
 * connessione fisica al pool. Tutte le altre chiamate (prepareStatement,
 * commit, ecc.) vengono semplicemente inoltrate alla connessione fisica
 * "vera" che sta dietro al proxy. E' lo stesso principio usato dagli stub
 * RMI: un oggetto locale che sembra quello remoto, ma aggiunge un
 * comportamento speciale.
 */
public final class ConnectionManager {

    // L'unica istanza del Singleton. E' "volatile" perche' potrebbe essere
    // letta da piu' thread contemporaneamente (piu' client RMI in parallelo):
    // senza volatile un thread potrebbe vedere un valore "vecchio" a causa
    // delle ottimizzazioni della JVM.
    private static volatile ConnectionManager instance;

    // pool contiene solo le connessioni fisiche attualmente libere (non in
    // uso da nessun DAO). E' una coda bloccante: chi chiede una connessione
    // quando la coda e' vuota si mette in attesa finche' qualcuno non ne
    // restituisce una (vedi getConnection()).
    private final BlockingQueue<Connection> pool;

    // tutteLeConnessioni tiene traccia di TUTTE le connessioni fisiche aperte
    // all'avvio, sia quelle libere sia quelle attualmente prestate a un DAO.
    // Serve solo per lo shutdown, per essere sicuri di chiuderle tutte anche
    // se in quel momento qualcuna e' "in prestito" e quindi non e' nella coda pool.
    private final List<Connection> tutteLeConnessioni;

    // Costruttore privato (tipico del Singleton): nessuno puo' creare un
    // ConnectionManager con "new" dall'esterno della classe, l'unico modo e'
    // passare da initialize()/getInstance().
    private ConnectionManager(String url, String user, String password, int poolSize) throws SQLException {
        this.pool = new LinkedBlockingQueue<>(poolSize);
        this.tutteLeConnessioni = new ArrayList<>(poolSize);
        // Apriamo subito, una per una, tutte le connessioni fisiche del pool.
        // Se una fallisce, la SQLException risale al chiamante e il pool non
        // viene creato (ConnectionManager.initialize fallisce).
        for (int i = 0; i < poolSize; i++) {
            Connection physical = DriverManager.getConnection(url, user, password);
            tutteLeConnessioni.add(physical);
            pool.add(physical);
        }
    }

    /**
     * Crea l'istanza Singleton con i parametri di connessione al database.
     * Va chiamato una volta sola, all'avvio del server (in ServerMain).
     *
     * @param poolSize numero di connessioni fisiche da aprire subito
     * @throws SQLException se una delle connessioni non si riesce ad aprire
     * @throws IllegalStateException se e' gia' stato chiamato in precedenza
     */
    public static synchronized void initialize(String url, String user, String password, int poolSize)
            throws SQLException {
        if (instance != null) {
            throw new IllegalStateException("ConnectionManager gia' inizializzato");
        }
        instance = new ConnectionManager(url, user, password, poolSize);
    }

    /**
     * Restituisce l'istanza Singleton gia' creata da initialize().
     *
     * @throws IllegalStateException se initialize() non e' ancora stato chiamato
     */
    public static ConnectionManager getInstance() {
        ConnectionManager result = instance;
        if (result == null) {
            throw new IllegalStateException(
                    "ConnectionManager non inizializzato: chiamare initialize() all'avvio del server");
        }
        return result;
    }

    /**
     * Preleva una connessione libera dal pool. Se in quel momento sono tutte
     * occupate, aspetta fino a 10 secondi che se ne liberi una prima di
     * arrendersi con un timeout.
     *
     * IMPORTANTE: la connessione restituita non e' quella fisica, ma un
     * proxy (vedi wrap() piu' sotto): il codice chiamante puo' comunque
     * usarla in un normale try-with-resources, senza doversi preoccupare di
     * "restituirla" manualmente al pool, perche' close() lo fa per lui.
     */
    public Connection getConnection() throws SQLException {
        Connection physical;
        try {
            // poll(10, TimeUnit.SECONDS): aspetta al massimo 10 secondi, poi
            // restituisce null se non e' arrivata nessuna connessione libera.
            physical = pool.poll(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // Se il thread viene interrotto mentre aspetta, ripristiniamo il
            // flag di interruzione (buona pratica Java) e trasformiamo
            // l'InterruptedException in una SQLException, coerente con la
            // firma del metodo.
            Thread.currentThread().interrupt();
            throw new SQLException("Interrotto in attesa di una connessione dal pool", e);
        }
        if (physical == null) {
            // Timeout scaduto: vuol dire che tutte le connessioni del pool
            // erano occupate da altri client per piu' di 10 secondi.
            throw new SQLException("Timeout: nessuna connessione disponibile nel pool (troppi client concorrenti)");
        }
        return wrap(physical);
    }

    // Questo e' il cuore del pattern Proxy: costruiamo dinamicamente (con
    // java.lang.reflect.Proxy) un oggetto che implementa l'interfaccia
    // Connection, ma la cui implementazione e' scritta qui a mano tramite un
    // InvocationHandler, invece che con una classe concreta scritta da noi.
    private Connection wrap(Connection physical) {
        // L'InvocationHandler e' l'oggetto che riceve OGNI chiamata di
        // metodo fatta sul proxy: "proxy" e' l'oggetto proxy stesso, "method"
        // e' il metodo invocato (es. close(), prepareStatement(...), ecc.) e
        // "args" sono i suoi argomenti.
        InvocationHandler handler = (proxy, method, args) -> {
            // Caso speciale: se il metodo chiamato e' close() senza argomenti,
            // NON chiudiamo davvero la connessione fisica. La rimettiamo
            // semplicemente disponibile nel pool (offer), cosi' un altro DAO
            // la potra' riutilizzare. E' questo il trucco che rende
            // trasparente il riciclo delle connessioni.
            if ("close".equals(method.getName()) && (args == null || args.length == 0)) {
                pool.offer(physical);
                return null;
            }
            // Per tutti gli altri metodi (prepareStatement, commit, ecc.) ci
            // limitiamo a inoltrarli alla connessione fisica vera tramite
            // reflection, e restituiamo il suo risultato cosi' com'e': dal
            // punto di vista del DAO e' come se stesse chiamando il metodo
            // direttamente sulla connessione reale.
            try {
                return method.invoke(physical, args);
            } catch (java.lang.reflect.InvocationTargetException e) {
                // method.invoke() incapsula qualsiasi eccezione lanciata dal
                // metodo "vero" dentro una InvocationTargetException. La
                // "svolgiamo" e rilanciamo la causa originale (es. la
                // SQLException lanciata da prepareStatement), cosi' il
                // codice chiamante vede l'eccezione che si aspetta.
                throw e.getCause();
            }
        };
        // Creiamo l'oggetto proxy vero e proprio: gli diciamo che deve
        // implementare l'interfaccia Connection e che ogni chiamata deve
        // passare dal nostro handler definito sopra.
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(), new Class<?>[]{Connection.class}, handler);
    }

    /** Chiude fisicamente tutte le connessioni del pool: da chiamare solo allo spegnimento del server. */
    public synchronized void shutdown() {
        for (Connection c : tutteLeConnessioni) {
            try {
                c.close();
            } catch (SQLException ignored) {
                // Il server si sta comunque spegnendo: non c'e' nulla di
                // utile da fare con un eventuale errore di chiusura qui.
            }
        }
    }
}
