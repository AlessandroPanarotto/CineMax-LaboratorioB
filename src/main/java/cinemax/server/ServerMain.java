/*
 * Progetto CineMax - Laboratorio Interdisciplinare B (a.a. 2025/2026)
 * Universita' degli Studi dell'Insubria
 *
 * Autore: Panarotto Alessandro - matricola 757930 - sede di Varese (VA)
 */
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
 * Classe main del server (serverCM).
 *
 * All'avvio chiede da tastiera i dati per collegarsi al database dbCM
 * (come richiesto dalla specifica del progetto, slide 16), poi apre un
 * registry RMI e ci pubblica i tre servizi (autenticazione, proiezioni,
 * prenotazioni). Da quel momento il server resta in ascolto e puo' servire
 * piu' client contemporaneamente.
 */
public final class ServerMain {

    /** Nomi con cui i servizi vengono pubblicati sull'RMI registry (usati anche lato client per il lookup). */
    public static final String NOME_SERVIZIO_AUTENTICAZIONE = "cinemax/AutenticazioneService";
    public static final String NOME_SERVIZIO_PROIEZIONI = "cinemax/ProiezioneService";
    public static final String NOME_SERVIZIO_PRENOTAZIONI = "cinemax/PrenotazioneService";

    private static final int DIMENSIONE_POOL_CONNESSIONI = 8;
    private static final int PORTA_RMI_DEFAULT = 1099;

    // Classe di solo main, non deve essere istanziata.
    private ServerMain() {
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== CineMax — serverCM ===");
        Scanner scanner = new Scanner(System.in);
        Console console = System.console();

        // Chiediamo all'utente (chi avvia il server) i parametri per connettersi a PostgreSQL.
        // Per ogni campo, se si preme solo invio, si usa un valore di default.
        System.out.print("Host del database dbCM [localhost]: ");
        String host = leggiConDefault(scanner, "localhost");

        System.out.print("Porta PostgreSQL [5432]: ");
        String porta = leggiConDefault(scanner, "5432");

        System.out.print("Nome del database [dbCM]: ");
        String nomeDb = leggiConDefault(scanner, "dbCM");

        System.out.print("Utente PostgreSQL: ");
        String utenteDb = scanner.nextLine().trim();

        // Per la password proviamo a usare System.console(), che non fa vedere
        // i caratteri digitati; se non c'e' una console disponibile (es. IDE)
        // la leggiamo comunque, ma avvisando che sara' visibile a schermo.
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

        // Con i dati raccolti, inizializziamo il pool di connessioni (Singleton
        // ConnectionManager). Se la connessione fallisce non ha senso continuare:
        // stampiamo l'errore e usciamo subito.
        System.out.println("Connessione a " + url + " in corso...");
        try {
            ConnectionManager.initialize(url, utenteDb, passwordDb, DIMENSIONE_POOL_CONNESSIONI);
        } catch (SQLException e) {
            System.err.println("Impossibile connettersi al database: " + e.getMessage());
            System.exit(1);
            return;
        }
        System.out.println("Connesso a dbCM (pool di " + DIMENSIONE_POOL_CONNESSIONI + " connessioni).");

        // Creiamo i DAO (uno per ogni tabella/vista principale) e poi i tre
        // service, iniettando i DAO che servono a ciascuno.
        UtenteDAO utenteDAO = new UtenteDAOPostgres();
        FilmDAO filmDAO = new FilmDAOPostgres();
        ProiezioneDAO proiezioneDAO = new ProiezioneDAOPostgres();
        PrenotazioneDAO prenotazioneDAO = new PrenotazioneDAOPostgres();

        IAutenticazioneService autenticazioneService = new AutenticazioneServiceImpl(utenteDAO);
        IProiezioneService proiezioneService = new ProiezioneServiceImpl(proiezioneDAO, filmDAO);
        IPrenotazioneService prenotazioneService = new PrenotazioneServiceImpl(prenotazioneDAO, proiezioneDAO);

        // Apriamo il registry RMI sulla porta scelta e ci pubblichiamo i tre
        // servizi con i nomi definiti sopra: da questo momento il client puo'
        // trovarli con Naming.lookup()/Registry.lookup() usando questi nomi.
        Registry registry = LocateRegistry.createRegistry(portaRmi);
        registry.rebind(NOME_SERVIZIO_AUTENTICAZIONE, autenticazioneService);
        registry.rebind(NOME_SERVIZIO_PROIEZIONI, proiezioneService);
        registry.rebind(NOME_SERVIZIO_PRENOTAZIONI, prenotazioneService);

        // Quando il server viene fermato (es. Ctrl+C) vogliamo chiudere in modo
        // pulito tutte le connessioni fisiche aperte dal pool, invece di
        // lasciarle li' aperte verso il database.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Arresto di serverCM: chiusura del pool di connessioni...");
            ConnectionManager.getInstance().shutdown();
        }));

        System.out.println("serverCM in ascolto sulla porta RMI " + portaRmi +
                " — pronto a ricevere connessioni da clientCM (Ctrl+C per terminare).");

        // I thread che RMI usa internamente per gestire le chiamate dei client
        // sono thread "daemon": se main() terminasse, la JVM chiuderebbe subito
        // il processo anche se ci sono client collegati. Per questo teniamo il
        // thread principale bloccato per sempre su un wait(), cosi' il server
        // resta vivo finche' non lo si interrompe manualmente.
        Object attesaEterna = new Object();
        synchronized (attesaEterna) {
            try {
                attesaEterna.wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // Legge una riga da tastiera; se e' vuota (l'utente ha solo premuto invio)
    // restituisce il valore di default passato come parametro.
    private static String leggiConDefault(Scanner scanner, String valoreDefault) {
        String riga = scanner.nextLine().trim();
        return riga.isEmpty() ? valoreDefault : riga;
    }
}
