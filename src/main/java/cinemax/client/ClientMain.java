/*
 * Progetto CineMax - Laboratorio Interdisciplinare B (a.a. 2025/2026)
 * Universita' degli Studi dell'Insubria
 *
 * Autore: Panarotto Alessandro - matricola 757930 - sede di Varese (VA)
 */
package cinemax.client;

import cinemax.common.IAutenticazioneService;
import cinemax.common.IPrenotazioneService;
import cinemax.common.IProiezioneService;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Classe di avvio del client. Il metodo main fa tre cose in ordine:
 * 1) chiede all'utente host e porta del server tramite due finestre di dialogo,
 * 2) si collega al registry RMI e recupera i tre "stub" dei servizi (in pratica
 *    degli oggetti che sembrano locali ma che quando li chiami eseguono il
 *    metodo sul server, tutto gestito da RMI),
 * 3) crea e mostra la finestra principale (MainFrame) passandole gli stub.
 */
public final class ClientMain {

    private static final String NOME_SERVIZIO_AUTENTICAZIONE = "cinemax/AutenticazioneService";
    private static final String NOME_SERVIZIO_PROIEZIONI = "cinemax/ProiezioneService";
    private static final String NOME_SERVIZIO_PRENOTAZIONI = "cinemax/PrenotazioneService";

    // costruttore privato: questa classe serve solo per il main, non va istanziata
    private ClientMain() {
    }

    public static void main(String[] args) {
        // proviamo a usare il look and feel del sistema operativo invece di quello
        // grigio di default di Swing, ma se non e' disponibile va bene anche il default
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // nessun problema, si continua con il look and feel di default
        }

        // chiediamo all'utente dove si trova il server (host e porta RMI)
        String host = JOptionPane.showInputDialog(null,
                "Host del server CineMax (serverCM):", "localhost");
        if (host == null) {
            return; // ha premuto Annulla, chiudiamo tutto
        }
        host = host.isBlank() ? "localhost" : host.trim();

        String portaTesto = JOptionPane.showInputDialog(null,
                "Porta RMI di serverCM:", "1099");
        if (portaTesto == null) {
            return; // annullato anche qui
        }
        int porta;
        try {
            porta = portaTesto.isBlank() ? 1099 : Integer.parseInt(portaTesto.trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "Porta non valida", "Errore", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            // ci colleghiamo al registry RMI e recuperiamo i tre servizi remoti.
            // registry.lookup() restituisce uno stub: un oggetto locale che
            // implementa la stessa interfaccia del servizio sul server, ma che
            // sotto sotto manda la richiesta in rete e aspetta la risposta.
            Registry registry = LocateRegistry.getRegistry(host, porta);
            IAutenticazioneService autenticazioneService =
                    (IAutenticazioneService) registry.lookup(NOME_SERVIZIO_AUTENTICAZIONE);
            IProiezioneService proiezioneService =
                    (IProiezioneService) registry.lookup(NOME_SERVIZIO_PROIEZIONI);
            IPrenotazioneService prenotazioneService =
                    (IPrenotazioneService) registry.lookup(NOME_SERVIZIO_PRENOTAZIONI);

            // le componenti Swing vanno create e aggiornate sull'Event Dispatch
            // Thread, per questo la creazione della finestra e' dentro invokeLater
            SwingUtilities.invokeLater(() -> {
                MainFrame frame = new MainFrame(autenticazioneService, proiezioneService, prenotazioneService);
                frame.setVisible(true);
            });
        } catch (Exception e) {
            // se qualcosa va storto nella connessione (server spento, host sbagliato,
            // servizio non registrato...) mostriamo un messaggio invece di far
            // crashare il programma
            JOptionPane.showMessageDialog(null,
                    "Impossibile connettersi a serverCM su " + host + ":" + porta + "\n\n" + e,
                    "Errore di connessione", JOptionPane.ERROR_MESSAGE);
        }
    }
}
