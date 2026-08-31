/*
 * Progetto CineMax - Laboratorio Interdisciplinare B (a.a. 2025/2026)
 * Universita' degli Studi dell'Insubria
 *
 * Autore: Panarotto Alessandro - matricola 757930 - sede di Varese (VA)
 */
package cinemax.client;

import cinemax.client.controller.BigliettaioController;
import cinemax.client.controller.GestioneProiezioniController;
import cinemax.client.controller.LoginController;
import cinemax.client.controller.PrenotazioneController;
import cinemax.client.view.AreaBigliettaioView;
import cinemax.client.view.AreaClienteView;
import cinemax.client.view.AreaProiezionistaView;
import cinemax.client.view.DettaglioProiezioneView;
import cinemax.client.view.LoginView;
import cinemax.client.view.MenuInizialeView;
import cinemax.client.view.RegistrazioneView;
import cinemax.client.view.RicercaProiezioniView;
import cinemax.common.Bigliettaio;
import cinemax.common.Cliente;
import cinemax.common.IAutenticazioneService;
import cinemax.common.IPrenotazioneService;
import cinemax.common.IProiezioneService;
import cinemax.common.Proiezione;
import cinemax.common.Proiezionista;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.BorderFactory;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Font;

/**
 * Questa e' la finestra principale dell'applicazione: c'e' un solo JFrame,
 * e le varie schermate (menu, login, ricerca, area cliente...) sono dei
 * pannelli che si scambiano dentro di essa usando un CardLayout, un po'
 * come un mazzo di carte dove si vede sempre solo quella in cima.
 * Cambiare schermata quindi non vuol dire aprire una nuova finestra, ma solo
 * dire al CardLayout "adesso mostra questa".
 *
 * MainFrame tiene anche gli stub RMI dei tre servizi (passati da ClientMain
 * dopo il lookup) e l'oggetto Sessione con l'utente loggato: sono condivisi
 * da tutti i controller, che li recuperano tramite i metodi getXxxService()
 * qui sotto invece di doverli passare in giro ovunque.
 */
public class MainFrame extends JFrame {

    // nomi delle "carte" del CardLayout: ogni schermata viene registrata con
    // uno di questi nomi e per mostrarla basta chiamare mostraSchermata(nome)
    public static final String SCHERMATA_MENU = "menu";
    public static final String SCHERMATA_LOGIN = "login";
    public static final String SCHERMATA_REGISTRAZIONE = "registrazione";
    public static final String SCHERMATA_RICERCA = "ricerca";
    public static final String SCHERMATA_DETTAGLIO = "dettaglio";
    public static final String SCHERMATA_AREA_CLIENTE = "areaCliente";
    public static final String SCHERMATA_AREA_PROIEZIONISTA = "areaProiezionista";
    public static final String SCHERMATA_AREA_BIGLIETTAIO = "areaBigliettaio";

    private final Sessione sessione = new Sessione();

    private final IAutenticazioneService autenticazioneService;
    private final IProiezioneService proiezioneService;
    private final IPrenotazioneService prenotazioneService;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel schermate = new JPanel(cardLayout);
    private final JLabel etichettaUtente = new JLabel();

    private DettaglioProiezioneView dettaglioProiezioneView;
    private RicercaProiezioniView ricercaProiezioniView;
    private AreaClienteView areaClienteView;
    private AreaProiezionistaView areaProiezionistaView;
    private AreaBigliettaioView areaBigliettaioView;

    public MainFrame(IAutenticazioneService autenticazioneService, IProiezioneService proiezioneService,
                      IPrenotazioneService prenotazioneService) {
        super("CineMax");
        this.autenticazioneService = autenticazioneService;
        this.proiezioneService = proiezioneService;
        this.prenotazioneService = prenotazioneService;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1050, 700);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // in alto la barra con logo/utente/logout (sempre visibile), al centro
        // il pannello con il CardLayout che contiene tutte le schermate
        add(costruisciIntestazione(), BorderLayout.NORTH);
        add(schermate, BorderLayout.CENTER);

        // creiamo un controller per ogni "area" dell'applicazione. Ogni
        // controller riceve MainFrame cosi' puo' arrivare agli stub RMI e
        // alla Sessione. Le view non parlano mai direttamente con RMI: passano
        // sempre dal controller, che e' l'unico che sa come chiamare il server
        // e come interpretare eventuali errori (pattern MVC).
        LoginController loginController = new LoginController(this);
        GestioneProiezioniController gestioneProiezioniController = new GestioneProiezioniController(this);
        PrenotazioneController prenotazioneController = new PrenotazioneController(this);
        BigliettaioController bigliettaioController = new BigliettaioController(this);

        // registriamo ogni schermata nel CardLayout con il suo nome. Le view
        // che dobbiamo richiamare dopo (per aggiornarle o passargli dei dati)
        // le teniamo anche come campi della classe.
        schermate.add(new MenuInizialeView(this), SCHERMATA_MENU);
        schermate.add(new LoginView(this, loginController), SCHERMATA_LOGIN);
        schermate.add(new RegistrazioneView(this, loginController), SCHERMATA_REGISTRAZIONE);
        ricercaProiezioniView = new RicercaProiezioniView(this, gestioneProiezioniController);
        schermate.add(ricercaProiezioniView, SCHERMATA_RICERCA);

        dettaglioProiezioneView = new DettaglioProiezioneView(this, prenotazioneController);
        schermate.add(dettaglioProiezioneView, SCHERMATA_DETTAGLIO);

        areaClienteView = new AreaClienteView(this, prenotazioneController, gestioneProiezioniController);
        schermate.add(areaClienteView, SCHERMATA_AREA_CLIENTE);

        areaProiezionistaView = new AreaProiezionistaView(this, gestioneProiezioniController);
        schermate.add(areaProiezionistaView, SCHERMATA_AREA_PROIEZIONISTA);

        areaBigliettaioView = new AreaBigliettaioView(this, bigliettaioController);
        schermate.add(areaBigliettaioView, SCHERMATA_AREA_BIGLIETTAIO);

        // all'avvio si parte sempre dal menu iniziale (nessuno e' ancora loggato)
        mostraSchermata(SCHERMATA_MENU);
    }

    private JPanel costruisciIntestazione() {
        JPanel intestazione = new JPanel(new BorderLayout());
        intestazione.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        intestazione.setBackground(new Color(25, 40, 65));

        JLabel titolo = new JLabel("CineMax");
        titolo.setFont(titolo.getFont().deriveFont(Font.BOLD, 20f));
        titolo.setForeground(Color.WHITE);
        intestazione.add(titolo, BorderLayout.WEST);

        etichettaUtente.setForeground(Color.WHITE);
        JButton bottoneMenu = new JButton("Menu");
        bottoneMenu.addActionListener(e -> mostraSchermata(SCHERMATA_MENU));
        JButton bottoneLogout = new JButton("Logout");
        bottoneLogout.addActionListener(e -> {
            sessione.logout();
            aggiornaIntestazione();
            mostraSchermata(SCHERMATA_MENU);
        });

        JPanel destra = new JPanel();
        destra.setOpaque(false);
        destra.add(etichettaUtente);
        destra.add(bottoneMenu);
        destra.add(bottoneLogout);
        intestazione.add(destra, BorderLayout.EAST);

        aggiornaIntestazione();
        return intestazione;
    }

    // aggiorna la scritta in alto a destra con il nome dell'utente loggato
    // (o "guest" se nessuno ha ancora fatto login)
    public void aggiornaIntestazione() {
        if (sessione.isAutenticato()) {
            etichettaUtente.setText(sessione.getUtenteCorrente().toString() + "   ");
        } else {
            etichettaUtente.setText("Non autenticato (guest)   ");
        }
    }

    /** Mostra la schermata (la "carta" del CardLayout) con il nome indicato. */
    public void mostraSchermata(String nome) {
        cardLayout.show(schermate, nome);
    }

    /** Apre la schermata di dettaglio per la proiezione indicata. */
    public void mostraDettaglioProiezione(Proiezione proiezione) {
        dettaglioProiezioneView.mostra(proiezione);
        mostraSchermata(SCHERMATA_DETTAGLIO);
    }

    /** Apre la schermata di ricerca proiezioni gia' popolata con dei risultati (usato dal guest). */
    public void mostraRisultatiRicerca(java.util.List<Proiezione> risultati) {
        ricercaProiezioniView.mostraRisultati(risultati);
        mostraSchermata(SCHERMATA_RICERCA);
    }

    /**
     * Da chiamare subito dopo un login (o una registrazione) andata a buon
     * fine: capisce di che tipo e' l'utente appena entrato con una serie di
     * instanceof (Cliente, Proiezionista o Bigliettaio, sono le sottoclassi
     * di Utente definite lato server) e apre l'area riservata giusta.
     */
    public void alLoginRiuscito() {
        aggiornaIntestazione();
        if (sessione.getUtenteCorrente() instanceof Cliente) {
            areaClienteView.aggiorna();
            mostraSchermata(SCHERMATA_AREA_CLIENTE);
        } else if (sessione.getUtenteCorrente() instanceof Proiezionista) {
            areaProiezionistaView.aggiorna();
            mostraSchermata(SCHERMATA_AREA_PROIEZIONISTA);
        } else if (sessione.getUtenteCorrente() instanceof Bigliettaio) {
            areaBigliettaioView.aggiorna();
            mostraSchermata(SCHERMATA_AREA_BIGLIETTAIO);
        }
    }

    // getter usati dai controller per accedere a sessione e stub RMI
    public Sessione getSessione() {
        return sessione;
    }

    public IAutenticazioneService getAutenticazioneService() {
        return autenticazioneService;
    }

    public IProiezioneService getProiezioneService() {
        return proiezioneService;
    }

    public IPrenotazioneService getPrenotazioneService() {
        return prenotazioneService;
    }
}
