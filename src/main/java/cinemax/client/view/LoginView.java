package cinemax.client.view;

import cinemax.client.MainFrame;
import cinemax.client.controller.LoginController;

import javax.swing.*;
import java.awt.*;

/**
 * Schermata di login con i classici due campi username e password.
 * Usiamo GridBagLayout per allineare bene etichette e campi su due colonne.
 */
public class LoginView extends JPanel {

    public LoginView(MainFrame mainFrame, LoginController controller) {
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.EAST;

        // titolo centrato in cima, occupa le due colonne
        JLabel titolo = new JLabel("Login");
        titolo.setFont(titolo.getFont().deriveFont(Font.BOLD, 22f));
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.CENTER;
        add(titolo, c);
        c.gridwidth = 1;

        JTextField campoUsername = new JTextField(18);
        JPasswordField campoPassword = new JPasswordField(18);

        // riga username: etichetta a destra, campo a sinistra della colonna successiva
        c.gridy = 1;
        c.anchor = GridBagConstraints.EAST;
        add(new JLabel("Username:"), c);
        c.gridx = 1;
        c.anchor = GridBagConstraints.WEST;
        add(campoUsername, c);

        // riga password
        c.gridx = 0;
        c.gridy = 2;
        c.anchor = GridBagConstraints.EAST;
        add(new JLabel("Password:"), c);
        c.gridx = 1;
        c.anchor = GridBagConstraints.WEST;
        add(campoPassword, c);

        JButton bottoneLogin = new JButton("Accedi");
        c.gridx = 1;
        c.gridy = 3;
        add(bottoneLogin, c);

        // al click proviamo il login tramite il controller: se le credenziali
        // sono sbagliate arriva un'eccezione che mostriamo con Dialoghi.errore.
        // Puliamo comunque il campo password dopo il tentativo, per sicurezza.
        bottoneLogin.addActionListener(e -> {
            try {
                controller.login(campoUsername.getText().trim(), new String(campoPassword.getPassword()));
                campoPassword.setText("");
            } catch (Exception ex) {
                Dialoghi.errore(this, ex);
            }
        });
    }
}
