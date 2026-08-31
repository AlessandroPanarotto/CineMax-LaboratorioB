/*
 * Progetto CineMax - Laboratorio Interdisciplinare B (a.a. 2025/2026)
 * Universita' degli Studi dell'Insubria
 *
 * Autore: Panarotto Alessandro - matricola 757930 - sede di Varese (VA)
 */
package cinemax.server;

/**
 * Classe di utilita' per loggare lato server gli errori che arrivano dal database.
 *
 * A cosa serve: se un DAO prende una SQLException (magari lanciata proprio
 * dal driver di PostgreSQL) e la lasciasse passare cosi' com'e' fino al
 * client tramite RMI, il client andrebbe in errore, perche' non ha il driver
 * PostgreSQL nel suo classpath e quindi non riuscirebbe a deserializzare
 * quella eccezione (una ClassNotFoundException). Per questo motivo i service
 * prendono solo il messaggio testuale dell'eccezione e lo incapsulano dentro
 * una loro eccezione applicativa; lo stack trace completo, quello utile per
 * il debug, viene stampato solo qui nel log del server.
 */
public final class LogUtil {

    // Costruttore privato: questa classe ha solo metodi statici, non ha senso istanziarla.
    private LogUtil() {
    }

    /**
     * Stampa sul log del server il messaggio di un'eccezione, insieme a un
     * "contesto" (di solito il nome del metodo dove e' successo l'errore)
     * cosi' e' piu' facile capire da dove arriva il problema.
     */
    public static void erroreDb(String contesto, Exception e) {
        System.err.println("[serverCM] " + contesto + ": " + e.getMessage());
        // stampiamo comunque lo stack trace completo: serve solo lato server per il debug
        e.printStackTrace();
    }
}
