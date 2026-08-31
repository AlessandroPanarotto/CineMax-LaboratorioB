/*
 * Progetto CineMax - Laboratorio Interdisciplinare B (a.a. 2025/2026)
 * Universita' degli Studi dell'Insubria
 *
 * Autore: Panarotto Alessandro - matricola 757930 - sede di Varese (VA)
 */
package cinemax.client.view;

import cinemax.client.MainFrame;
import cinemax.client.controller.GestioneProiezioniController;
import cinemax.common.Film;
import cinemax.common.Proiezione;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Schermata del proiezionista. E' quella con piu' cose da fare:
 * - in alto, un pannello per aggiungere un nuovo film al catalogo e/o una
 *   nuova proiezione per un film gia' esistente
 * - poi due schede: le proiezioni ancora da fare (pianificate, modificabili
 *   o eliminabili) e quelle gia' passate (storiche, solo consultazione)
 */
public class AreaProiezionistaView extends JPanel {

    private final GestioneProiezioniController controller;

    // tiene traccia di quale film e' stato scelto nella combo di ricerca, per
    // sapere a quale film agganciare la proiezione quando si clicca "Aggiungi proiezione"
    private Film filmSelezionatoPerNuovaProiezione;

    private final DefaultTableModel modelloPianificate;
    private final JTable tabellaPianificate;
    private List<Proiezione> pianificateCorrenti;

    private final DefaultTableModel modelloStoriche;
    private final JTable tabellaStoriche;

    public AreaProiezionistaView(MainFrame mainFrame, GestioneProiezioniController controller) {
        this.controller = controller;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(costruisciPannelloAggiunta(), BorderLayout.NORTH);

        // tabella delle proiezioni pianificate (future), con i bottoni per modificarle
        // o eliminarle: il server rifiuta l'operazione se ci sono gia' prenotazioni
        modelloPianificate = new DefaultTableModel(
                new Object[]{"Titolo", "Data", "Ora", "Costo (€)", "Posti liberi"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tabellaPianificate = new JTable(modelloPianificate);
        JPanel pannelloPianificate = new JPanel(new BorderLayout(5, 5));
        pannelloPianificate.add(new JScrollPane(tabellaPianificate), BorderLayout.CENTER);
        JPanel bottoniPianificate = new JPanel(new GridLayout(1, 2, 8, 8));
        bottoniPianificate.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        JButton bottoneModifica = new JButton("Modifica selezionata");
        JButton bottoneElimina = new JButton("Elimina selezionata");
        bottoniPianificate.add(bottoneModifica);
        bottoniPianificate.add(bottoneElimina);
        pannelloPianificate.add(bottoniPianificate, BorderLayout.SOUTH);
        bottoneModifica.addActionListener(e -> modificaSelezionata());
        bottoneElimina.addActionListener(e -> eliminaSelezionata());

        // tabella delle proiezioni storiche (gia' passate): solo consultazione, niente bottoni
        modelloStoriche = new DefaultTableModel(
                new Object[]{"Titolo", "Data", "Ora", "Costo (€)"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tabellaStoriche = new JTable(modelloStoriche);

        // le due tabelle stanno in due schede separate
        JTabbedPane schede = new JTabbedPane();
        schede.addTab("Proiezioni pianificate", pannelloPianificate);
        schede.addTab("Proiezioni storiche", new JScrollPane(tabellaStoriche));
        add(schede, BorderLayout.CENTER);
    }

    // costruisce il pannello in alto per aggiungere film e proiezioni. E' diviso
    // in tre "righe" (pannelli con FlowLayout, uno sopra l'altro dentro un BoxLayout
    // verticale): 1) dati del nuovo film, 2) ricerca di un film gia' a catalogo,
    // 3) dati della nuova proiezione da agganciare al film trovato al punto 2
    private JPanel costruisciPannelloAggiunta() {
        JPanel pannello = new JPanel();
        pannello.setLayout(new BoxLayout(pannello, BoxLayout.Y_AXIS));
        pannello.setBorder(BorderFactory.createTitledBorder("Aggiungi film e proiezione"));

        // --- riga 1: aggiungi film ---
        JPanel rigaFilm = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        JTextField campoTitoloFilm = new JTextField(14);
        JTextField campoGenere = new JTextField(8);
        JTextField campoRegista = new JTextField(12);
        JTextField campoAnno = new JTextField(5);
        JTextField campoDurata = new JTextField(4);
        JTextField campoEtaMinima = new JTextField(3);
        JButton bottoneAggiungiFilm = new JButton("Aggiungi nuovo film a catalogo");
        rigaFilm.add(new JLabel("Titolo:"));
        rigaFilm.add(campoTitoloFilm);
        rigaFilm.add(new JLabel("Genere:"));
        rigaFilm.add(campoGenere);
        rigaFilm.add(new JLabel("Regista:"));
        rigaFilm.add(campoRegista);
        rigaFilm.add(new JLabel("Anno:"));
        rigaFilm.add(campoAnno);
        rigaFilm.add(new JLabel("Durata (min):"));
        rigaFilm.add(campoDurata);
        rigaFilm.add(new JLabel("Eta' minima:"));
        rigaFilm.add(campoEtaMinima);
        pannello.add(rigaFilm);

        JPanel rigaBottoneFilm = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        rigaBottoneFilm.add(bottoneAggiungiFilm);
        pannello.add(rigaBottoneFilm);

        // click su "Aggiungi nuovo film a catalogo": convertiamo i campi numerici,
        // chiamiamo il controller e mostriamo l'id assegnato al nuovo film. L'eta'
        // minima e' l'unico campo davvero facoltativo, se vuoto usiamo 0.
        bottoneAggiungiFilm.addActionListener(e -> {
            try {
                int anno = Integer.parseInt(campoAnno.getText().trim());
                int durata = Integer.parseInt(campoDurata.getText().trim());
                int etaMinima = campoEtaMinima.getText().trim().isEmpty() ? 0 : Integer.parseInt(campoEtaMinima.getText().trim());
                long idFilm = controller.aggiungiFilm(campoTitoloFilm.getText().trim(), campoGenere.getText().trim(),
                        campoRegista.getText().trim(), anno, durata, etaMinima);
                Dialoghi.info(this, "Film aggiunto a catalogo (id=" + idFilm + "). Ora puoi cercarlo qui sotto per aggiungere una proiezione.");
            } catch (NumberFormatException ex) {
                Dialoghi.info(this, "Anno, durata ed eta' minima devono essere numeri interi");
            } catch (Exception ex) {
                Dialoghi.errore(this, ex);
            }
        });

        // --- riga 2: cerca film esistente ---
        JPanel rigaCerca = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        JTextField campoCercaFilm = new JTextField(14);
        JComboBox<Film> selettoreFilm = new JComboBox<>();
        JButton bottoneCercaFilm = new JButton("Cerca film a catalogo");
        rigaCerca.add(new JLabel("Titolo (anche parziale):"));
        rigaCerca.add(campoCercaFilm);
        rigaCerca.add(bottoneCercaFilm);
        rigaCerca.add(new JLabel("Film trovati:"));
        rigaCerca.add(selettoreFilm);
        pannello.add(rigaCerca);

        // cerca a catalogo i film che matchano il testo (anche parziale) e li mette
        // nella combo qui sopra, cosi' il proiezionista puo' scegliere quello giusto
        bottoneCercaFilm.addActionListener(e -> {
            try {
                List<Film> risultati = controller.cercaFilm(campoCercaFilm.getText().trim());
                selettoreFilm.setModel(new DefaultComboBoxModel<>(risultati.toArray(new Film[0])));
                // NOTA: JComboBox.setModel() seleziona automaticamente il primo elemento
                // ma non genera un ActionEvent, quindi il listener sotto non verrebbe
                // invocato: si imposta esplicitamente qui il film selezionato di default.
                filmSelezionatoPerNuovaProiezione = risultati.isEmpty() ? null : risultati.get(0);
            } catch (Exception ex) {
                Dialoghi.errore(this, ex);
            }
        });
        // quando l'utente cambia manualmente la selezione nella combo, aggiorniamo il film scelto
        selettoreFilm.addActionListener(e -> filmSelezionatoPerNuovaProiezione = (Film) selettoreFilm.getSelectedItem());

        // --- riga 3: aggiungi proiezione per il film selezionato ---
        JPanel rigaProiezione = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        JTextField campoData = new JTextField(10);
        JTextField campoOra = new JTextField(6);
        JTextField campoCosto = new JTextField(6);
        JButton bottoneAggiungiProiezione = new JButton("Aggiungi proiezione per il film selezionato");
        rigaProiezione.add(new JLabel("Data (AAAA-MM-GG):"));
        rigaProiezione.add(campoData);
        rigaProiezione.add(new JLabel("Ora (HH:MM):"));
        rigaProiezione.add(campoOra);
        rigaProiezione.add(new JLabel("Costo biglietto (€):"));
        rigaProiezione.add(campoCosto);
        rigaProiezione.add(bottoneAggiungiProiezione);
        pannello.add(rigaProiezione);

        // click su "Aggiungi proiezione": serve prima aver selezionato un film
        // dalla ricerca qui sopra, altrimenti non sapremmo a quale film agganciarla
        bottoneAggiungiProiezione.addActionListener(e -> {
            if (filmSelezionatoPerNuovaProiezione == null) {
                Dialoghi.info(this, "Cerca e seleziona prima un film dall'elenco qui sopra");
                return;
            }
            try {
                LocalDate data = LocalDate.parse(campoData.getText().trim());
                LocalTime ora = LocalTime.parse(campoOra.getText().trim());
                BigDecimal costo = new BigDecimal(campoCosto.getText().trim());
                controller.aggiungiProiezione(filmSelezionatoPerNuovaProiezione.getIdFilm(), data, ora, costo);
                Dialoghi.info(this, "Proiezione aggiunta con successo");
                aggiorna();
            } catch (java.time.format.DateTimeParseException ex) {
                Dialoghi.info(this, "Data (AAAA-MM-GG) oppure ora (HH:MM) non valide");
            } catch (NumberFormatException ex) {
                Dialoghi.info(this, "Costo non valido: inserire un numero (es. 8.50)");
            } catch (Exception ex) {
                Dialoghi.errore(this, ex);
            }
        });

        return pannello;
    }

    /** Ricarica le liste di proiezioni pianificate e storiche dal server. */
    public void aggiorna() {
        try {
            pianificateCorrenti = controller.pianificate();
            modelloPianificate.setRowCount(0);
            for (Proiezione p : pianificateCorrenti) {
                modelloPianificate.addRow(new Object[]{
                        p.getFilm().getTitolo(), p.getDataProiezione(), p.getOraProiezione(),
                        p.getCostoBiglietto(), p.getPostiLiberi()
                });
            }
            modelloStoriche.setRowCount(0);
            for (Proiezione p : controller.storiche()) {
                modelloStoriche.addRow(new Object[]{
                        p.getFilm().getTitolo(), p.getDataProiezione(), p.getOraProiezione(), p.getCostoBiglietto()
                });
            }
        } catch (Exception ex) {
            Dialoghi.errore(this, ex);
        }
    }

    private void modificaSelezionata() {
        int riga = tabellaPianificate.getSelectedRow();
        if (riga < 0) {
            Dialoghi.info(this, "Selezionare prima una proiezione dalla tabella");
            return;
        }
        Proiezione selezionata = pianificateCorrenti.get(riga);
        // pre-compiliamo i campi con i valori attuali, cosi' il proiezionista deve
        // cambiare solo quello che vuole modificare davvero
        JTextField campoData = new JTextField(selezionata.getDataProiezione().toString());
        JTextField campoOra = new JTextField(selezionata.getOraProiezione().toString());
        JTextField campoCosto = new JTextField(selezionata.getCostoBiglietto().toString());
        JPanel pannello = new JPanel(new GridLayout(3, 2, 5, 5));
        pannello.add(new JLabel("Nuova data (AAAA-MM-GG):"));
        pannello.add(campoData);
        pannello.add(new JLabel("Nuova ora (HH:MM):"));
        pannello.add(campoOra);
        pannello.add(new JLabel("Nuovo costo (€):"));
        pannello.add(campoCosto);

        int scelta = JOptionPane.showConfirmDialog(this, pannello, "Modifica proiezione: " + selezionata.getFilm().getTitolo(),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (scelta != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            LocalDate nuovaData = LocalDate.parse(campoData.getText().trim());
            LocalTime nuovaOra = LocalTime.parse(campoOra.getText().trim());
            BigDecimal nuovoCosto = new BigDecimal(campoCosto.getText().trim());
            controller.modificaProiezione(selezionata.getIdProiezione(), nuovaData, nuovaOra, nuovoCosto);
            Dialoghi.info(this, "Proiezione modificata con successo");
            aggiorna();
        } catch (java.time.format.DateTimeParseException ex) {
            Dialoghi.info(this, "Data (AAAA-MM-GG) oppure ora (HH:MM) non valide");
        } catch (NumberFormatException ex) {
            Dialoghi.info(this, "Costo non valido");
        } catch (Exception ex) {
            Dialoghi.errore(this, ex);
        }
    }

    private void eliminaSelezionata() {
        int riga = tabellaPianificate.getSelectedRow();
        if (riga < 0) {
            Dialoghi.info(this, "Selezionare prima una proiezione dalla tabella");
            return;
        }
        Proiezione selezionata = pianificateCorrenti.get(riga);
        // conferma obbligatoria: eliminare una proiezione non si puo' annullare
        if (!Dialoghi.conferma(this, "Eliminare la proiezione \"" + selezionata.getFilm().getTitolo()
                + "\" del " + selezionata.getDataProiezione() + "?")) {
            return;
        }
        try {
            controller.eliminaProiezione(selezionata.getIdProiezione());
            Dialoghi.info(this, "Proiezione eliminata");
            aggiorna();
        } catch (Exception ex) {
            Dialoghi.errore(this, ex);
        }
    }
}
