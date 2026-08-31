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
package cinemax.server.dao;

import cinemax.common.Proiezione;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * DAO per la tabella "proiezioni". Per le sole letture le query si appoggiano
 * a una vista del database, v_proiezioni_disponibilita, che oltre ai dati
 * della proiezione calcola anche i posti liberi rimasti (posti_liberi).
 */
public interface ProiezioneDAO {

    /** Cerca una proiezione per id. Restituisce null se non esiste. */
    Proiezione findById(long idProiezione) throws SQLException;

    /** Ricerca con criteri combinabili: ogni parametro null viene ignorato come filtro. */
    List<Proiezione> cerca(String titolo, String genere, LocalDate dataDa, LocalDate dataA,
                            BigDecimal costoMin, BigDecimal costoMax) throws SQLException;

    /** Proiezioni nei prossimi tre mesi per un film (il titolo puo' essere parziale). */
    List<Proiezione> prossimiTreMesiPerFilm(String titoloParziale) throws SQLException;

    /** Tutte le proiezioni ancora da svolgere, in ordine cronologico. */
    List<Proiezione> pianificate() throws SQLException;

    /** Le proiezioni gia' passate, dalla piu' recente alla piu' vecchia. */
    List<Proiezione> storiche() throws SQLException;

    /**
     * Inserisce una nuova proiezione e restituisce l'id assegnato.
     * Puo' fallire (SQLException) se un trigger del database rileva che la
     * proiezione si sovrappone a un'altra gia' presente (nel cinema c'e' una sala sola).
     */
    long inserisci(long idFilm, LocalDate data, LocalTime ora, BigDecimal costoBiglietto) throws SQLException;

    /**
     * Modifica data, ora e/o costo di una proiezione. Puo' fallire se
     * esistono gia' prenotazioni per quella proiezione (in tal caso non e'
     * piu' modificabile) o se la nuova data/ora si sovrappone ad un'altra proiezione.
     */
    void aggiorna(long idProiezione, LocalDate nuovaData, LocalTime nuovaOra, BigDecimal nuovoCosto) throws SQLException;

    /** Cancella una proiezione. Puo' fallire se esistono gia' prenotazioni collegate. */
    void elimina(long idProiezione) throws SQLException;
}
