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
package cinemax.client.view;

import cinemax.common.ServiceException;

import javax.swing.JOptionPane;
import java.awt.Component;
import java.rmi.RemoteException;

/**
 * Classe di comodo con dei metodi statici per far comparire i popup di
 * dialogo (errore, informazione, conferma) sempre nello stesso modo in
 * tutte le schermate, cosi' non dobbiamo riscrivere ogni volta il codice
 * di JOptionPane.
 *
 * La cosa interessante e' il metodo errore(): distingue se l'eccezione
 * arrivata e' una ServiceException (cioe' un errore "normale" gestito dal
 * server, tipo "posti non disponibili": il messaggio e' gia' scritto per
 * l'utente) oppure una RemoteException (il server non risponde proprio,
 * es. e' spento o e' caduta la rete).
 */
public final class Dialoghi {

    // costruttore privato: e' una classe di soli metodi statici, non va istanziata
    private Dialoghi() {
    }

    // mostra un popup di errore, scegliendo il messaggio e il titolo in base al tipo di eccezione
    public static void errore(Component parent, Exception e) {
        String messaggio;
        String titolo;
        if (e instanceof ServiceException) {
            // errore applicativo: il messaggio arriva gia' pronto dal server/controller
            messaggio = e.getMessage();
            titolo = "Operazione non consentita";
        } else if (e instanceof RemoteException) {
            // problema di connessione col server, non e' colpa dell'utente
            messaggio = "Impossibile comunicare con il server CineMax.\n" +
                    "Verificare che serverCM sia in esecuzione e riprovare.\n\n(" + e.getMessage() + ")";
            titolo = "Errore di connessione";
        } else {
            // qualsiasi altra eccezione imprevista (bug, cast sbagliato, ecc.)
            messaggio = "Si e' verificato un errore imprevisto: " + e.getMessage();
            titolo = "Errore";
        }
        JOptionPane.showMessageDialog(parent, messaggio, titolo, JOptionPane.ERROR_MESSAGE);
    }

    // popup semplice per messaggi informativi (es. "operazione riuscita")
    public static void info(Component parent, String messaggio) {
        JOptionPane.showMessageDialog(parent, messaggio, "CineMax", JOptionPane.INFORMATION_MESSAGE);
    }

    // popup si'/no, ritorna true solo se l'utente ha scelto "si'"
    public static boolean conferma(Component parent, String messaggio) {
        int scelta = JOptionPane.showConfirmDialog(parent, messaggio, "Conferma",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        return scelta == JOptionPane.YES_OPTION;
    }
}
