package cinemax.server;

import cinemax.common.IAutenticazioneService;
import cinemax.common.IPrenotazioneService;
import cinemax.common.IProiezioneService;
import cinemax.server.dao.FilmDAO;
import cinemax.server.dao.FilmDAOPostgres;
import cinemax.server.dao.PrenotazioneDAO;
import cinemax.server.dao.PrenotazioneDAOPostgres;
import cinemax.server.dao.ProiezioneDAO;
import cinemax.server.dao.ProiezioneDAOPostgres;
import cinemax.server.dao.UtenteDAO;
import cinemax.server.dao.UtenteDAOPostgres;
import cinemax.server.db.ConnectionManager;
import cinemax.server.service.AutenticazioneServiceImpl;
import cinemax.server.service.PrenotazioneServiceImpl;
import cinemax.server.service.ProiezioneServiceImpl;

import java.io.Console;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.sql.SQLException;
import java.util.Scanner;

/**
 * Punto di ingresso di {@code serverCM}. Come richiesto dalla specifica di
 * progetto (slide 16), al lancio chiede all'utente le credenziali di accesso
 * a {@code dbCM} e l'host del database; poi avvia un RMI registry e vi
 * pubblica i tre servizi applicativi, restando in attesa di connessioni da
 * {@code clientCM} (anche multiple, in concorrenza).
 */
public final class ServerMain {

    /** Nomi con cui i servizi sono pubblicati sull'RMI registry. */
    public static final String NOME_SERVIZIO_AUTENTICAZIONE = "cinemax/AutenticazioneService";
    public static final String NOME_SERVIZIO_PROIEZIONI = "cinemax/ProiezioneService";
    public static final String NOME_SERVIZIO_PRENOTAZIONI = "cinemax/PrenotazioneService";

    private static final int DIMENSIONE_POOL_CONNESSIONI = 8;
    private static final int PORTA_RMI_DEFAULT = 1099;

    private ServerMain() {
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== CineMax — serverCM ===");
        Scanner scanner = new Scanner(System.in);
        Console console = System.console();

        System.out.print("Host del database dbCM [localhost]: ");
        String host = leggiConDefault(scanner, "localhost");

        System.out.print("Porta PostgreSQL [5432]: ");
        String porta = leggiConDefault(scanner, "5432");

        System.out.print("Nome del database [dbCM]: ");
        String nomeDb = leggiConDefault(scanner, "dbCM");

        System.out.print("Utente PostgreSQL: ");
        String utenteDb = scanner.nextLine().trim();

        String passwordDb;
        if (console != null) {
            char[] pwd = console.readPassword("Password PostgreSQL: ");
            passwordDb = new String(pwd);
        } else {
            System.out.print("Password PostgreSQL (attenzione: sara' visibile): ");
            passwordDb = scanner.nextLine();
        }

        System.out.print("Porta RMI su cui pubblicare i servizi [" + PORTA_RMI_DEFAULT + "]: ");
        int portaRmi = Integer.parseInt(leggiConDefault(scanner, String.valueOf(PORTA_RMI_DEFAULT)));

        String url = "jdbc:postgresql://" + host + ":" + porta + "/" + nomeDb;

        System.out.println("Connessione a " + url + " in corso...");
        try {
            ConnectionManager.initialize(url, utenteDb, passwordDb, DIMENSIONE_POOL_CONNESSIONI);
        } catch (SQLException e) {
            System.err.println("Impossibile connettersi al database: " + e.getMessage());
            System.exit(1);
            return;
        }
        System.out.println("Connesso a dbCM (pool di " + DIMENSIONE_POOL_CONNESSIONI + " connessioni).");

        UtenteDAO utenteDAO = new UtenteDAOPostgres();
        FilmDAO filmDAO = new FilmDAOPostgres();
        ProiezioneDAO proiezioneDAO = new ProiezioneDAOPostgres();
        PrenotazioneDAO prenotazioneDAO = new PrenotazioneDAOPostgres();

        IAutenticazioneService autenticazioneService = new AutenticazioneServiceImpl(utenteDAO);
        IProiezioneService proiezioneService = new ProiezioneServiceImpl(proiezioneDAO, filmDAO);
        IPrenotazioneService prenotazioneService = new PrenotazioneServiceImpl(prenotazioneDAO, proiezioneDAO);

        Registry registry = LocateRegistry.createRegistry(portaRmi);
        registry.rebind(NOME_SERVIZIO_AUTENTICAZIONE, autenticazioneService);
        registry.rebind(NOME_SERVIZIO_PROIEZIONI, proiezioneService);
        registry.rebind(NOME_SERVIZIO_PRENOTAZIONI, prenotazioneService);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Arresto di serverCM: chiusura del pool di connessioni...");
            ConnectionManager.getInstance().shutdown();
        }));

        System.out.println("serverCM in ascolto sulla porta RMI " + portaRmi +
                " — pronto a ricevere connessioni da clientCM (Ctrl+C per terminare).");

        // I thread interni del runtime RMI sono daemon: senza un thread non-daemon
        // che resti attivo, la JVM terminerebbe subito dopo il ritorno di main().
        // Questo thread resta semplicemente in attesa per l'intera vita del server.
        Object attesaEterna = new Object();
        synchronized (attesaEterna) {
            try {
                attesaEterna.wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static String leggiConDefault(Scanner scanner, String valoreDefault) {
        String riga = scanner.nextLine().trim();
        return riga.isEmpty() ? valoreDefault : riga;
    }
}
