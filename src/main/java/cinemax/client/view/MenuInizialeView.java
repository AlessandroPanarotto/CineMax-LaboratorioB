package cinemax.client.view;

import cinemax.client.MainFrame;
import cinemax.common.Proiezione;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * E' la prima schermata che si vede aprendo il programma. Da qui si puo':
 * - fare login o registrarsi
 * - oppure entrare come "ospite" (senza account) cercando un film per nome,
 *   per vedere le sue proiezioni nei prossimi tre mesi
 */
public class MenuInizialeView extends JPanel {

    public MenuInizialeView(MainFrame mainFrame) {
        // il layout con GridBagLayout non si tocca, e' gia' stato sistemato
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.insets = new Insets(10, 10, 10, 10);
        c.fill = GridBagConstraints.HORIZONTAL;

        // titolo grande in alto
        JLabel titolo = new JLabel("Benvenuto su CineMax", SwingConstants.CENTER);
        titolo.setFont(titolo.getFont().deriveFont(Font.BOLD, 26f));
        c.gridy = 0;
        add(titolo, c);

        // riga con i due bottoni per login e registrazione: aprono le rispettive schermate
        JPanel bottoniAccount = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        JButton bottoneLogin = new JButton("Login");
        bottoneLogin.addActionListener(e -> mainFrame.mostraSchermata(MainFrame.SCHERMATA_LOGIN));
        JButton bottoneRegistrati = new JButton("Registrati");
        bottoneRegistrati.addActionListener(e -> mainFrame.mostraSchermata(MainFrame.SCHERMATA_REGISTRAZIONE));
        bottoniAccount.add(bottoneLogin);
        bottoniAccount.add(bottoneRegistrati);
        c.gridy = 1;
        add(bottoniAccount, c);

        JSeparator separatore = new JSeparator();
        c.gridy = 2;
        add(separatore, c);

        JLabel etichettaGuest = new JLabel("Oppure prosegui come ospite: cerca un film per vedere le sue "
                + "proiezioni nei prossimi tre mesi", SwingConstants.CENTER);
        c.gridy = 3;
        add(etichettaGuest, c);

        // parte "ospite": si puo' cercare un film senza fare login, in due modi
        // (ricerca rapida per titolo qui, oppure ricerca avanzata con piu' filtri)
        JPanel pannelloGuest = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 10));
        JTextField campoTitolo = new JTextField(22);
        JButton bottoneCerca = new JButton("Cerca come ospite");
        JButton bottoneRicercaCompleta = new JButton("Ricerca avanzata (ospite)");
        pannelloGuest.add(new JLabel("Titolo del film:"));
        pannelloGuest.add(campoTitolo);
        pannelloGuest.add(bottoneCerca);
        pannelloGuest.add(bottoneRicercaCompleta);
        c.gridy = 4;
        add(pannelloGuest, c);

        // ricerca rapida: chiama direttamente il servizio (non passa dal controller,
        // dato che qui non serve essere autenticati) e mostra le proiezioni trovate
        bottoneCerca.addActionListener(e -> {
            try {
                List<Proiezione> risultati = mainFrame.getProiezioneService()
                        .proiezioniProssimiTreMesi(campoTitolo.getText().trim());
                if (risultati.isEmpty()) {
                    Dialoghi.info(this, "Nessuna proiezione trovata nei prossimi tre mesi per questo film.");
                } else {
                    mainFrame.mostraRisultatiRicerca(risultati);
                }
            } catch (Exception ex) {
                Dialoghi.errore(this, ex);
            }
        });

        // porta alla schermata di ricerca completa, con tutti i filtri
        bottoneRicercaCompleta.addActionListener(e -> mainFrame.mostraSchermata(MainFrame.SCHERMATA_RICERCA));
    }
}
