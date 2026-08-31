/*
 * Progetto CineMax - Laboratorio Interdisciplinare B (a.a. 2025/2026)
 * Universita' degli Studi dell'Insubria
 *
 * Autore: Panarotto Alessandro - matricola 757930 - sede di Varese (VA)
 */
package cinemax.client.view;

import cinemax.client.controller.BigliettaioController;
import cinemax.client.MainFrame;
import cinemax.common.Prenotazione;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Schermata del bigliettaio, divisa in due schede (JTabbedPane):
 * - "Prenotazioni di oggi": elenco veloce di tutte le prenotazioni per
 *   le proiezioni della giornata, utile allo sportello
 * - "Cerca prenotazioni": ricerca piu' generale con vari filtri opzionali
 *   (codice, cliente, film, intervallo di date)
 */
public class AreaBigliettaioView extends JPanel {

    private static final Object[] COLONNE =
            {"Codice", "Cliente", "Film", "Data", "Ora", "Posti", "Costo unitario (€)", "Costo totale (€)"};

    private final BigliettaioController controller;
    private final DefaultTableModel modelloOdierne;
    private final DefaultTableModel modelloRicerca;

    public AreaBigliettaioView(MainFrame mainFrame, BigliettaioController controller) {
        this.controller = controller;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- prima scheda: prenotazioni della giornata odierna ---
        modelloOdierne = nuovoModello();
        JTable tabellaOdierne = new JTable(modelloOdierne);
        JPanel pannelloOdierne = new JPanel(new BorderLayout(5, 5));
        pannelloOdierne.add(new JScrollPane(tabellaOdierne), BorderLayout.CENTER);
        JButton bottoneAggiornaOdierne = new JButton("Aggiorna");
        bottoneAggiornaOdierne.addActionListener(e -> caricaOdierne());
        pannelloOdierne.add(bottoneAggiornaOdierne, BorderLayout.SOUTH);

        // --- seconda scheda: ricerca prenotazioni con filtri ---
        modelloRicerca = nuovoModello();
        JTable tabellaRicerca = new JTable(modelloRicerca);
        JPanel pannelloRicerca = new JPanel(new BorderLayout(5, 5));

        // form di ricerca su due righe (i filtri sono tutti facoltativi e si possono combinare)
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        JTextField campoCodice = new JTextField(10);
        JTextField campoNomeCognome = new JTextField(12);
        JTextField campoTitolo = new JTextField(12);
        JTextField campoDataDa = new JTextField(10);
        JTextField campoDataA = new JTextField(10);
        JButton bottoneCerca = new JButton("Cerca prenotazioni");

        JPanel riga1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        riga1.add(new JLabel("Codice:"));
        riga1.add(campoCodice);
        riga1.add(new JLabel("Nome/cognome cliente:"));
        riga1.add(campoNomeCognome);
        riga1.add(new JLabel("Titolo film:"));
        riga1.add(campoTitolo);
        form.add(riga1);

        JPanel riga2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        riga2.add(new JLabel("Data da:"));
        riga2.add(campoDataDa);
        riga2.add(new JLabel("a:"));
        riga2.add(campoDataA);
        riga2.add(bottoneCerca);
        form.add(riga2);

        pannelloRicerca.add(form, BorderLayout.NORTH);
        pannelloRicerca.add(new JScrollPane(tabellaRicerca), BorderLayout.CENTER);

        // i campi vuoti vengono passati come null al controller, che li tratta
        // come "nessun filtro su questo criterio" (i filtri sono tutti opzionali)
        bottoneCerca.addActionListener(e -> {
            try {
                LocalDate dataDa = parseData(campoDataDa.getText());
                LocalDate dataA = parseData(campoDataA.getText());
                List<Prenotazione> risultati = controller.cerca(
                        vuotoANull(campoCodice.getText()), vuotoANull(campoNomeCognome.getText()),
                        vuotoANull(campoTitolo.getText()), dataDa, dataA);
                popola(modelloRicerca, risultati);
            } catch (DateTimeParseException ex) {
                Dialoghi.info(this, "Data non valida: usare il formato AAAA-MM-GG");
            } catch (Exception ex) {
                Dialoghi.errore(this, ex);
            }
        });

        JTabbedPane schede = new JTabbedPane();
        schede.addTab("Prenotazioni di oggi", pannelloOdierne);
        schede.addTab("Cerca prenotazioni", pannelloRicerca);
        add(schede, BorderLayout.CENTER);
    }

    // le due tabelle (odierne e ricerca) hanno le stesse colonne, quindi
    // usiamo questo metodo per non duplicare la creazione del modello
    private DefaultTableModel nuovoModello() {
        return new DefaultTableModel(COLONNE, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    // svuota il modello passato e lo riempie con i dati delle prenotazioni date
    private void popola(DefaultTableModel modello, List<Prenotazione> prenotazioni) {
        modello.setRowCount(0);
        for (Prenotazione p : prenotazioni) {
            modello.addRow(new Object[]{
                    p.getCodicePrenotazione(), p.getNomeCliente() + " " + p.getCognomeCliente(),
                    p.getProiezione().getFilm().getTitolo(), p.getProiezione().getDataProiezione(),
                    p.getProiezione().getOraProiezione(), p.getNumPosti(),
                    p.getProiezione().getCostoBiglietto(), p.getCostoTotale()
            });
        }
    }

    /** Ricarica le prenotazioni della data odierna dal server. */
    public void aggiorna() {
        caricaOdierne();
    }

    private void caricaOdierne() {
        try {
            popola(modelloOdierne, controller.prenotazioniOdierne());
        } catch (Exception ex) {
            Dialoghi.errore(this, ex);
        }
    }

    private LocalDate parseData(String testo) {
        String t = testo.trim();
        return t.isEmpty() ? null : LocalDate.parse(t);
    }

    private String vuotoANull(String testo) {
        String t = testo.trim();
        return t.isEmpty() ? null : t;
    }
}
