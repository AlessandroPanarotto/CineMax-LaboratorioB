/*
 * Progetto CineMax - Laboratorio Interdisciplinare B (a.a. 2025/2026)
 * Universita' degli Studi dell'Insubria
 *
 * Autore: Panarotto Alessandro - matricola 757930 - sede di Varese (VA)
 */
package cinemax.client.controller;

import cinemax.client.MainFrame;
import cinemax.common.Prenotazione;
import cinemax.common.ServiceException;

import java.rmi.RemoteException;
import java.util.List;

/**
 * Controller per le prenotazioni fatte dal cliente loggato: prenotare
 * posti per una proiezione, vedere le proprie prenotazioni, modificarle o
 * cancellarle. Ogni metodo recupera l'id dell'utente corrente dalla
 * Sessione (tenuta in MainFrame) e poi chiama lo stub RMI di
 * IPrenotazioneService.
 */
public class PrenotazioneController {

    private final MainFrame mainFrame;

    public PrenotazioneController(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
    }

    /** Prenota numPosti posti per la proiezione indicata; restituisce il codice della prenotazione creata. */
    public String creaPrenotazione(long idProiezione, int numPosti) throws RemoteException, ServiceException {
        long idUtente = mainFrame.getSessione().getUtenteCorrente().getIdUtente();
        return mainFrame.getPrenotazioneService().creaPrenotazione(idUtente, idProiezione, numPosti);
    }

    /** Prenotazioni attualmente attive dell'utente loggato. */
    public List<Prenotazione> prenotazioniAttive() throws RemoteException {
        long idUtente = mainFrame.getSessione().getUtenteCorrente().getIdUtente();
        return mainFrame.getPrenotazioneService().visualizzaPrenotazioni(idUtente);
    }

    /** Sposta una prenotazione esistente su un'altra proiezione (stesso codice, nuova data/ora). */
    public void modificaPrenotazione(String codicePrenotazione, long nuovaIdProiezione)
            throws RemoteException, ServiceException {
        mainFrame.getPrenotazioneService().modificaPrenotazione(codicePrenotazione, nuovaIdProiezione);
    }

    public void eliminaPrenotazione(String codicePrenotazione) throws RemoteException, ServiceException {
        mainFrame.getPrenotazioneService().eliminaPrenotazione(codicePrenotazione);
    }
}
