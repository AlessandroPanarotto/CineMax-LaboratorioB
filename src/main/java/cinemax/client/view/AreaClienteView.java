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

import cinemax.client.MainFrame;
import cinemax.client.controller.GestioneProiezioniController;
import cinemax.client.controller.PrenotazioneController;
import cinemax.common.Prenotazione;
import cinemax.common.Proiezione;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Schermata privata del cliente loggato: mostra l'elenco delle sue
 * prenotazioni ancora attive (cioe' quelle per proiezioni non ancora
 * passate) e permette di crearne una nuova, modificarne una esistente
 * (spostandola su un'altra proiezione) oppure disdirla.
 */
public class AreaClienteView extends JPanel {

    private final MainFrame mainFrame;
    private final PrenotazioneController prenotazioneController;
    private final GestioneProiezioniController gestioneProiezioniController;

    private final DefaultTableModel modelloTabella;
    private final JTable tabella;
    private List<Prenotazione> prenotazioniCorrenti;

    public AreaClienteView(MainFrame mainFrame, PrenotazioneController prenotazioneController,
                            GestioneProiezioniController gestioneProiezioniController) {
        this.mainFrame = mainFrame;
        this.prenotazioneController = prenotazioneController;
        this.gestioneProiezioniController = gestioneProiezioniController;

        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel titolo = new JLabel("Le mie prenotazioni attive");
        titolo.setFont(titolo.getFont().deriveFont(Font.BOLD, 20f));
        add(titolo, BorderLayout.NORTH);

        // tabella con le prenotazioni attive del cliente; sola lettura, si
        // seleziona una riga e poi si usano i bottoni sotto per agire su di essa
        modelloTabella = new DefaultTableModel(
                new Object[]{"Codice", "Film", "Data", "Ora", "Posti", "Costo totale (€)"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tabella = new JTable(modelloTabella);
        add(new JScrollPane(tabella), BorderLayout.CENTER);

        // i quattro bottoni per gestire le prenotazioni
        JPanel bottoni = new JPanel(new GridLayout(1, 4, 8, 8));
        bottoni.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        JButton bottoneNuova = new JButton("Nuova prenotazione");
        JButton bottoneModifica = new JButton("Modifica selezionata");
        JButton bottoneElimina = new JButton("Disdici selezionata");
        JButton bottoneAggiorna = new JButton("Aggiorna elenco");
        bottoni.add(bottoneNuova);
        bottoni.add(bottoneModifica);
        bottoni.add(bottoneElimina);
        bottoni.add(bottoneAggiorna);
        add(bottoni, BorderLayout.SOUTH);

        bottoneNuova.addActionListener(e -> mainFrame.mostraSchermata(MainFrame.SCHERMATA_RICERCA));
        bottoneAggiorna.addActionListener(e -> aggiorna());
        bottoneModifica.addActionListener(e -> modificaSelezionata());
        bottoneElimina.addActionListener(e -> eliminaSelezionata());
    }

    /** Ricarica le prenotazioni attive del cliente autenticato dal server. */
    public void aggiorna() {
        try {
            prenotazioniCorrenti = prenotazioneController.prenotazioniAttive();
            modelloTabella.setRowCount(0);
            for (Prenotazione p : prenotazioniCorrenti) {
                modelloTabella.addRow(new Object[]{
                        p.getCodicePrenotazione(), p.getProiezione().getFilm().getTitolo(),
                        p.getProiezione().getDataProiezione(), p.getProiezione().getOraProiezione(),
                        p.getNumPosti(), p.getCostoTotale()
                });
            }
        } catch (Exception ex) {
            Dialoghi.errore(this, ex);
        }
    }

    private void modificaSelezionata() {
        int riga = tabella.getSelectedRow();
        if (riga < 0) {
            Dialoghi.info(this, "Selezionare prima una prenotazione dalla tabella");
            return;
        }
        Prenotazione selezionata = prenotazioniCorrenti.get(riga);
        try {
            // per modificare la prenotazione bisogna scegliere un'altra proiezione:
            // prendiamo tutte le proiezioni future pianificate, togliendo quella
            // attuale (non avrebbe senso "spostare" la prenotazione sulla stessa)
            List<Proiezione> pianificate = gestioneProiezioniController.pianificate();
            pianificate.removeIf(p -> p.getIdProiezione() == selezionata.getProiezione().getIdProiezione());
            if (pianificate.isEmpty()) {
                Dialoghi.info(this, "Non ci sono altre proiezioni future disponibili su cui spostare la prenotazione");
                return;
            }
            // facciamo scegliere la nuova proiezione con una combo dentro un popup di conferma
            JComboBox<Proiezione> selettore = new JComboBox<>(pianificate.toArray(new Proiezione[0]));
            int scelta = JOptionPane.showConfirmDialog(this, selettore,
                    "Scegli la nuova proiezione per " + selezionata.getCodicePrenotazione(),
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (scelta != JOptionPane.OK_OPTION) {
                return;
            }
            Proiezione nuovaProiezione = (Proiezione) selettore.getSelectedItem();
            prenotazioneController.modificaPrenotazione(selezionata.getCodicePrenotazione(), nuovaProiezione.getIdProiezione());
            Dialoghi.info(this, "Prenotazione modificata con successo");
            aggiorna();
        } catch (Exception ex) {
            Dialoghi.errore(this, ex);
        }
    }

    private void eliminaSelezionata() {
        int riga = tabella.getSelectedRow();
        if (riga < 0) {
            Dialoghi.info(this, "Selezionare prima una prenotazione dalla tabella");
            return;
        }
        Prenotazione selezionata = prenotazioniCorrenti.get(riga);
        // chiediamo sempre conferma prima di eliminare, per evitare click accidentali
        if (!Dialoghi.conferma(this, "Disdire la prenotazione " + selezionata.getCodicePrenotazione() + "?")) {
            return;
        }
        try {
            prenotazioneController.eliminaPrenotazione(selezionata.getCodicePrenotazione());
            Dialoghi.info(this, "Prenotazione eliminata");
            aggiorna();
        } catch (Exception ex) {
            Dialoghi.errore(this, ex);
        }
    }
}
