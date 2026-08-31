/*
 * Progetto CineMax - Laboratorio Interdisciplinare B (a.a. 2025/2026)
 * Universita' degli Studi dell'Insubria
 *
 * Autore: Panarotto Alessandro - matricola 757930 - sede di Varese (VA)
 */
package cinemax.client.view;

import cinemax.client.MainFrame;
import cinemax.client.controller.LoginController;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Schermata di registrazione: da qui puo' registrarsi solo un nuovo cliente
 * (il proiezionista e il bigliettaio non si registrano da soli, li crea
 * chi gestisce il sistema). Dopo la registrazione l'utente viene anche
 * loggato automaticamente, lo fa il controller con registraEAccedi().
 */
public class RegistrazioneView extends JPanel {

    public RegistrazioneView(MainFrame mainFrame, LoginController controller) {
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 6, 5, 6);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.CENTER;

        JLabel titolo = new JLabel("Registrazione nuovo cliente");
        titolo.setFont(titolo.getFont().deriveFont(Font.BOLD, 22f));
        add(titolo, c);
        c.gridwidth = 1;

        // tutti i campi del form
        JTextField campoNome = new JTextField(20);
        JTextField campoCognome = new JTextField(20);
        JTextField campoUsername = new JTextField(20);
        JPasswordField campoPassword = new JPasswordField(20);
        JTextField campoDataNascita = new JTextField(20);
        JTextField campoLuogo = new JTextField(20);

        // aggiungiamo i campi uno sotto l'altro usando il metodo helper qui sotto,
        // cosi' non ripetiamo ogni volta il codice per etichetta + campo su una riga
        int riga = 1;
        riga = aggiungiCampo(c, riga, "Nome:", campoNome);
        riga = aggiungiCampo(c, riga, "Cognome:", campoCognome);
        riga = aggiungiCampo(c, riga, "Username:", campoUsername);
        riga = aggiungiCampo(c, riga, "Password (min. 6 caratteri):", campoPassword);
        riga = aggiungiCampo(c, riga, "Data di nascita (AAAA-MM-GG, facoltativa):", campoDataNascita);
        riga = aggiungiCampo(c, riga, "Luogo di domicilio:", campoLuogo);

        JButton bottoneRegistrati = new JButton("Registrati e accedi");
        c.gridx = 1;
        c.gridy = riga;
        add(bottoneRegistrati, c);

        bottoneRegistrati.addActionListener(e -> {
            // la data di nascita e' facoltativa: se il campo e' vuoto va bene,
            // altrimenti proviamo a interpretarla e se il formato e' sbagliato
            // avvisiamo l'utente e usciamo subito senza chiamare il controller
            LocalDate dataNascita = null;
            String testoData = campoDataNascita.getText().trim();
            if (!testoData.isEmpty()) {
                try {
                    dataNascita = LocalDate.parse(testoData);
                } catch (DateTimeParseException ex) {
                    Dialoghi.info(this, "Data di nascita non valida: usare il formato AAAA-MM-GG (es. 2000-05-20)");
                    return;
                }
            }
            // a questo punto proviamo davvero la registrazione: eventuali errori
            // (username gia' preso, password troppo corta, ecc.) li gestisce il
            // controller lanciando un'eccezione che finisce qui sotto
            try {
                controller.registraEAccedi(
                        campoNome.getText().trim(),
                        campoCognome.getText().trim(),
                        campoUsername.getText().trim(),
                        new String(campoPassword.getPassword()),
                        dataNascita,
                        campoLuogo.getText().trim());
                campoPassword.setText("");
            } catch (Exception ex) {
                Dialoghi.errore(this, ex);
            }
        });
    }

    // metodo di comodo: aggiunge una riga "etichetta + campo" al form e ritorna
    // la riga successiva, cosi' evitiamo di scrivere sei volte lo stesso codice
    private int aggiungiCampo(GridBagConstraints c, int riga, String etichetta, JComponent campo) {
        c.gridx = 0;
        c.gridy = riga;
        c.anchor = GridBagConstraints.EAST;
        add(new JLabel(etichetta), c);
        c.gridx = 1;
        c.anchor = GridBagConstraints.WEST;
        add(campo, c);
        return riga + 1;
    }
}
