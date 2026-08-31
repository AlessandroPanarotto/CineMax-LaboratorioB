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
package cinemax.client.controller;

import cinemax.client.MainFrame;
import cinemax.common.Cliente;
import cinemax.common.ServiceException;
import cinemax.common.Utente;

import java.rmi.RemoteException;
import java.time.LocalDate;

/**
 * Controller per le operazioni di login e registrazione. La view (LoginView,
 * RegistrazioneView) non chiama mai direttamente il server: chiama questi
 * metodi, che a loro volta invocano lo stub RMI di IAutenticazioneService e
 * si occupano di aggiornare la Sessione condivisa in MainFrame. Cosi' la
 * view si preoccupa solo della grafica, e la logica di "cosa fare quando
 * il login va a buon fine" sta tutta qui.
 *
 * Le eccezioni (RemoteException per problemi di rete, ServiceException per
 * errori applicativi come password sbagliata) vengono semplicemente
 * rilanciate: sara' la view a mostrarle all'utente con un messaggio.
 */
public class LoginController {

    private final MainFrame mainFrame;

    public LoginController(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
    }

    /** Esegue il login e, se riuscito, aggiorna la sessione e apre l'area riservata del ruolo. */
    public void login(String username, String password) throws RemoteException, ServiceException {
        // chiamata al server: se username/password non sono corretti il server
        // lancia una ServiceException, che qui non catturiamo e lasciamo
        // propagare fino alla view
        Utente utente = mainFrame.getAutenticazioneService().login(username, password);
        mainFrame.getSessione().login(utente);
        mainFrame.alLoginRiuscito();
    }

    /** Registra un nuovo cliente sul server e, se va a buon fine, lo autentica subito. */
    public void registraEAccedi(String nome, String cognome, String username, String password,
                                 LocalDate dataNascita, String luogoDomicilio)
            throws RemoteException, ServiceException {
        Cliente cliente = mainFrame.getAutenticazioneService()
                .registraCliente(nome, cognome, username, password, dataNascita, luogoDomicilio);
        // dopo la registrazione non serve rifare il login: lo facciamo subito
        // noi con il cliente appena creato, cosi' l'utente entra direttamente
        mainFrame.getSessione().login(cliente);
        mainFrame.alLoginRiuscito();
    }
}
