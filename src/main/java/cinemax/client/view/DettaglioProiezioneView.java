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
import cinemax.client.controller.PrenotazioneController;
import cinemax.common.Proiezione;

import javax.swing.*;
import java.awt.*;

/**
 * Mostra tutti i dettagli di una proiezione: dati del film (genere, regista,
 * anno, durata, eta' minima), data/ora della proiezione, costo del biglietto
 * e posti ancora liberi. Se chi sta guardando e' un cliente loggato, da qui
 * puo' anche prenotare direttamente scegliendo il numero di posti.
 */
public class DettaglioProiezioneView extends JPanel {

    private final MainFrame mainFrame;
    private final PrenotazioneController controller;

    private final JLabel etichettaTitolo = new JLabel();
    private final JLabel etichettaGenere = new JLabel();
    private final JLabel etichettaRegista = new JLabel();
    private final JLabel etichettaAnno = new JLabel();
    private final JLabel etichettaDurata = new JLabel();
    private final JLabel etichettaEtaMinima = new JLabel();
    private final JLabel etichettaDataOra = new JLabel();
    private final JLabel etichettaCosto = new JLabel();
    private final JLabel etichettaPostiLiberi = new JLabel();
    private final JSpinner selettorePosti = new JSpinner(new SpinnerNumberModel(1, 1, 200, 1));
    private final JButton bottonePrenota = new JButton("Prenota");

    private Proiezione proiezioneCorrente;

    public DettaglioProiezioneView(MainFrame mainFrame, PrenotazioneController controller) {
        this.mainFrame = mainFrame;
        this.controller = controller;

        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 10, 6, 10);
        c.gridx = 0;
        c.anchor = GridBagConstraints.WEST;

        // tutte le etichette con le info del film e della proiezione, una sotto l'altra;
        // il testo vero e proprio viene impostato dopo, nel metodo mostra()
        int riga = 0;
        etichettaTitolo.setFont(etichettaTitolo.getFont().deriveFont(Font.BOLD, 22f));
        c.gridy = riga++;
        add(etichettaTitolo, c);
        c.gridy = riga++;
        add(etichettaGenere, c);
        c.gridy = riga++;
        add(etichettaRegista, c);
        c.gridy = riga++;
        add(etichettaAnno, c);
        c.gridy = riga++;
        add(etichettaDurata, c);
        c.gridy = riga++;
        add(etichettaEtaMinima, c);
        c.gridy = riga++;
        add(new JSeparator(), c);
        c.gridy = riga++;
        add(etichettaDataOra, c);
        c.gridy = riga++;
        add(etichettaCosto, c);
        c.gridy = riga++;
        add(etichettaPostiLiberi, c);

        // pannello per prenotare: spinner per scegliere quanti posti e bottone di conferma.
        // Il bottone viene abilitato/disabilitato in mostra() a seconda di chi sta guardando
        JPanel pannelloPrenota = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pannelloPrenota.add(new JLabel("Numero di posti:"));
        pannelloPrenota.add(selettorePosti);
        pannelloPrenota.add(bottonePrenota);
        c.gridy = riga++;
        add(pannelloPrenota, c);

        // click su "Prenota": chiediamo al controller di creare la prenotazione.
        // Se va tutto bene arriva il codice della prenotazione, lo mostriamo e
        // torniamo alla schermata "Le mie prenotazioni" del cliente.
        bottonePrenota.addActionListener(e -> {
            try {
                // Attenzione: se l'utente scrive un numero nello spinner e clicca subito
                // "Prenota" senza premere Invio o Tab, il valore digitato potrebbe non
                // essere ancora stato salvato nel modello (rimarrebbe quello precedente).
                // Con commitEdit() forziamo il salvataggio del testo scritto prima di
                // leggere il valore con getValue().
                try {
                    selettorePosti.commitEdit();
                } catch (java.text.ParseException parseEx) {
                    Dialoghi.info(this, "Numero di posti non valido");
                    return;
                }
                int numPosti = (Integer) selettorePosti.getValue();
                String codice = controller.creaPrenotazione(proiezioneCorrente.getIdProiezione(), numPosti);
                Dialoghi.info(this, "Prenotazione effettuata con successo.\nCodice prenotazione: " + codice);
                mainFrame.mostraSchermata(MainFrame.SCHERMATA_AREA_CLIENTE);
            } catch (Exception ex) {
                Dialoghi.errore(this, ex);
            }
        });
    }

    /** Chiamato da MainFrame ogni volta che si apre il dettaglio di una nuova proiezione:
     * aggiorna tutte le etichette e decide se il bottone "Prenota" deve essere attivo. */
    public void mostra(Proiezione proiezione) {
        this.proiezioneCorrente = proiezione;
        etichettaTitolo.setText(proiezione.getFilm().getTitolo());
        etichettaGenere.setText("Genere: " + proiezione.getFilm().getGenere());
        etichettaRegista.setText("Regista: " + proiezione.getFilm().getRegista());
        etichettaAnno.setText("Anno: " + proiezione.getFilm().getAnno());
        etichettaDurata.setText("Durata: " + proiezione.getFilm().getDurataMinuti() + " minuti");
        etichettaEtaMinima.setText("Eta' minima consigliata: " + proiezione.getFilm().getEtaMinima());
        etichettaDataOra.setText("Proiezione: " + proiezione.getDataProiezione() + " alle " + proiezione.getOraProiezione());
        etichettaCosto.setText("Costo biglietto: € " + proiezione.getCostoBiglietto());
        etichettaPostiLiberi.setText("Posti liberi: " + proiezione.getPostiLiberi());

        // si puo' prenotare solo se si e' loggati come cliente E ci sono ancora posti liberi
        boolean puoPrenotare = mainFrame.getSessione().isCliente() && proiezione.getPostiLiberi() > 0;
        bottonePrenota.setEnabled(puoPrenotare);
        selettorePosti.setEnabled(puoPrenotare);
        ((SpinnerNumberModel) selettorePosti.getModel()).setMaximum(Math.max(1, proiezione.getPostiLiberi()));
        if (!mainFrame.getSessione().isCliente()) {
            bottonePrenota.setToolTipText("Effettua il login come cliente per prenotare");
        } else {
            bottonePrenota.setToolTipText(null);
        }
    }
}
