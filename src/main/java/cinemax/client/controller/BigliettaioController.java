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
import java.time.LocalDate;
import java.util.List;

/**
 * Controller per la parte riservata al bigliettaio: a differenza del
 * cliente, il bigliettaio puo' vedere le prenotazioni di tutti gli utenti
 * (per fare i controlli in sala), non solo le proprie. Anche qui i metodi
 * si limitano a richiamare lo stub RMI di IPrenotazioneService.
 */
public class BigliettaioController {

    private final MainFrame mainFrame;

    public BigliettaioController(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
    }

    /** Tutte le prenotazioni relative alle proiezioni di oggi. */
    public List<Prenotazione> prenotazioniOdierne() throws RemoteException {
        return mainFrame.getPrenotazioneService().prenotazioniOdierne();
    }

    /** Ricerca prenotazioni per codice, nome/cognome del cliente, titolo del film e intervallo di date. */
    public List<Prenotazione> cerca(String codice, String nomeCognome, String titoloFilm,
                                     LocalDate dataDa, LocalDate dataA) throws RemoteException {
        return mainFrame.getPrenotazioneService().cercaPrenotazioni(codice, nomeCognome, titoloFilm, dataDa, dataA);
    }

    public Prenotazione visualizzaPrenotazione(String codicePrenotazione) throws RemoteException, ServiceException {
        return mainFrame.getPrenotazioneService().visualizzaPrenotazione(codicePrenotazione);
    }
}
