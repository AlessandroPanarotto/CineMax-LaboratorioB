package cinemax.client.view;

import cinemax.client.MainFrame;
import cinemax.client.controller.GestioneProiezioniController;
import cinemax.common.Proiezione;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Schermata di ricerca proiezioni, accessibile anche senza login (ospite).
 * Si possono combinare piu' filtri, tutti facoltativi: titolo, genere,
 * intervallo di date e intervallo di costo. I risultati finiscono in una
 * tabella, da cui si puo' aprire il dettaglio della proiezione selezionata.
 */
public class RicercaProiezioniView extends JPanel {

    private final MainFrame mainFrame;
    private final DefaultTableModel modelloTabella;
    private final JTable tabella;
    private List<Proiezione> risultatiCorrenti;

    private final JTextField campoTitolo = new JTextField(14);
    private final JTextField campoGenere = new JTextField(10);
    private final JTextField campoDataDa = new JTextField(10);
    private final JTextField campoDataA = new JTextField(10);
    private final JTextField campoCostoMin = new JTextField(6);
    private final JTextField campoCostoMax = new JTextField(6);

    public RicercaProiezioniView(MainFrame mainFrame, GestioneProiezioniController controller) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // form dei filtri: due righe, la prima con titolo/genere/bottone cerca,
        // la seconda con l'intervallo di date e l'intervallo di costo
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        JPanel rigaTitoloGenere = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        rigaTitoloGenere.add(new JLabel("Titolo:"));
        rigaTitoloGenere.add(campoTitolo);
        rigaTitoloGenere.add(new JLabel("Genere:"));
        rigaTitoloGenere.add(campoGenere);
        JButton bottoneCerca = new JButton("Cerca");
        rigaTitoloGenere.add(bottoneCerca);
        form.add(rigaTitoloGenere);

        JPanel rigaFiltri = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        rigaFiltri.add(new JLabel("Data da (AAAA-MM-GG):"));
        rigaFiltri.add(campoDataDa);
        rigaFiltri.add(new JLabel("a:"));
        rigaFiltri.add(campoDataA);
        rigaFiltri.add(new JLabel("Costo da:"));
        rigaFiltri.add(campoCostoMin);
        rigaFiltri.add(new JLabel("a:"));
        rigaFiltri.add(campoCostoMax);
        form.add(rigaFiltri);

        add(form, BorderLayout.NORTH);

        // tabella con i risultati della ricerca: non editabile, l'utente puo' solo
        // selezionare una riga
        modelloTabella = new DefaultTableModel(
                new Object[]{"Titolo", "Genere", "Data", "Ora", "Costo (€)", "Posti liberi"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tabella = new JTable(modelloTabella);
        add(new JScrollPane(tabella), BorderLayout.CENTER);

        JButton bottoneDettaglio = new JButton("Vedi dettaglio proiezione selezionata");
        add(bottoneDettaglio, BorderLayout.SOUTH);

        // click su "Cerca": leggiamo i campi (quelli vuoti diventano null, cioe'
        // "nessun filtro su questo campo") e passiamo tutto al controller.
        // Se un campo data o costo e' scritto in modo sbagliato mostriamo un
        // messaggio specifico invece del generico errore del server.
        bottoneCerca.addActionListener(e -> {
            try {
                LocalDate dataDa = parseData(campoDataDa.getText());
                LocalDate dataA = parseData(campoDataA.getText());
                BigDecimal costoMin = parseCosto(campoCostoMin.getText());
                BigDecimal costoMax = parseCosto(campoCostoMax.getText());
                List<Proiezione> risultati = controller.cerca(
                        vuotoANull(campoTitolo.getText()), vuotoANull(campoGenere.getText()),
                        dataDa, dataA, costoMin, costoMax);
                mostraRisultati(risultati);
            } catch (DateTimeParseException ex) {
                Dialoghi.info(this, "Data non valida: usare il formato AAAA-MM-GG");
            } catch (NumberFormatException ex) {
                Dialoghi.info(this, "Costo non valido: inserire un numero (es. 7.50)");
            } catch (Exception ex) {
                Dialoghi.errore(this, ex);
            }
        });

        // apre il dettaglio della proiezione selezionata nella tabella; se non e'
        // selezionata nessuna riga avvisiamo l'utente invece di fare un errore strano
        bottoneDettaglio.addActionListener(e -> {
            int riga = tabella.getSelectedRow();
            if (riga < 0) {
                Dialoghi.info(this, "Selezionare prima una proiezione dalla tabella");
                return;
            }
            mainFrame.mostraDettaglioProiezione(risultatiCorrenti.get(riga));
        });
    }

    /** Popola la tabella con dei risultati gia' ottenuti altrove (es. ricerca guest dal menu iniziale). */
    public void mostraRisultati(List<Proiezione> risultati) {
        this.risultatiCorrenti = risultati;
        modelloTabella.setRowCount(0);
        for (Proiezione p : risultati) {
            modelloTabella.addRow(new Object[]{
                    p.getFilm().getTitolo(), p.getFilm().getGenere(),
                    p.getDataProiezione(), p.getOraProiezione(),
                    p.getCostoBiglietto(), p.getPostiLiberi()
            });
        }
    }

    // se il campo e' vuoto vuol dire "nessun filtro", quindi ritorniamo null
    private LocalDate parseData(String testo) {
        String t = testo.trim();
        return t.isEmpty() ? null : LocalDate.parse(t);
    }

    private BigDecimal parseCosto(String testo) {
        String t = testo.trim();
        return t.isEmpty() ? null : new BigDecimal(t);
    }

    // stessa idea ma per i campi testo semplici (titolo, genere, ...)
    private String vuotoANull(String testo) {
        String t = testo.trim();
        return t.isEmpty() ? null : t;
    }
}
