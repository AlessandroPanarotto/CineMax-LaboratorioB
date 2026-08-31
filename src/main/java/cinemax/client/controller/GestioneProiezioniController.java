/*
 * Progetto CineMax - Laboratorio Interdisciplinare B (a.a. 2025/2026)
 * Universita' degli Studi dell'Insubria
 *
 * Autore: Panarotto Alessandro - matricola 757930 - sede di Varese (VA)
 */
package cinemax.client.controller;

import cinemax.client.MainFrame;
import cinemax.common.Film;
import cinemax.common.Proiezione;
import cinemax.common.ServiceException;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Controller che si occupa di tutto quello che riguarda film e proiezioni:
 * ricerche (usate anche dal guest non loggato e dal cliente) e le operazioni
 * di gestione del palinsesto riservate al proiezionista (aggiungere film,
 * aggiungere/modificare/eliminare proiezioni).
 *
 * Come gli altri controller, e' solo un "ponte" verso lo stub RMI di
 * IProiezioneService: ogni metodo prende i parametri dalla view, chiama il
 * metodo corrispondente sul server e ne restituisce il risultato. Non c'e'
 * altra logica qui perche' i controlli veri (es. permessi, validita' dei
 * dati) vengono fatti dal server.
 */
public class GestioneProiezioniController {

    private final MainFrame mainFrame;

    public GestioneProiezioniController(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
    }

    /** Ricerca proiezioni filtrando per titolo, genere, intervallo di date e di prezzo (tutti i parametri sono opzionali). */
    public List<Proiezione> cerca(String titolo, String genere, LocalDate dataDa, LocalDate dataA,
                                   BigDecimal costoMin, BigDecimal costoMax) throws RemoteException {
        return mainFrame.getProiezioneService().cercaProiezioni(titolo, genere, dataDa, dataA, costoMin, costoMax);
    }

    /** Usato dal menu iniziale per mostrare le proiezioni dei prossimi tre mesi (anche senza login). */
    public List<Proiezione> proiezioniProssimiTreMesi(String titoloParziale) throws RemoteException {
        return mainFrame.getProiezioneService().proiezioniProssimiTreMesi(titoloParziale);
    }

    public List<Film> cercaFilm(String titoloParziale) throws RemoteException {
        return mainFrame.getProiezioneService().cercaFilm(titoloParziale);
    }

    // --- da qui in giu': operazioni riservate al proiezionista ---

    public long aggiungiFilm(String titolo, String genere, String regista, int anno,
                              int durataMinuti, int etaMinima) throws RemoteException, ServiceException {
        return mainFrame.getProiezioneService().aggiungiFilm(titolo, genere, regista, anno, durataMinuti, etaMinima);
    }

    public void aggiungiProiezione(long idFilm, LocalDate data, LocalTime ora, BigDecimal costoBiglietto)
            throws RemoteException, ServiceException {
        mainFrame.getProiezioneService().aggiungiProiezione(idFilm, data, ora, costoBiglietto);
    }

    public void modificaProiezione(long idProiezione, LocalDate nuovaData, LocalTime nuovaOra, BigDecimal nuovoCosto)
            throws RemoteException, ServiceException {
        mainFrame.getProiezioneService().modificaProiezione(idProiezione, nuovaData, nuovaOra, nuovoCosto);
    }

    public void eliminaProiezione(long idProiezione) throws RemoteException, ServiceException {
        mainFrame.getProiezioneService().eliminaProiezione(idProiezione);
    }

    /** Proiezioni ancora da svolgere (data futura), per la vista del proiezionista. */
    public List<Proiezione> pianificate() throws RemoteException {
        return mainFrame.getProiezioneService().proiezioniPianificate();
    }

    /** Proiezioni gia' svolte (data passata), sempre per il proiezionista. */
    public List<Proiezione> storiche() throws RemoteException {
        return mainFrame.getProiezioneService().proiezioniStoriche();
    }
}
