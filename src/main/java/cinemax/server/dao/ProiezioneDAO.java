package cinemax.server.dao;

import cinemax.common.Proiezione;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Accesso alla tabella {@code proiezioni}, tramite la vista
 * {@code v_proiezioni_disponibilita} per le sole letture (espone anche il
 * dato derivato {@code posti_liberi}, vedi {@code doc/01_progettazione_database.md} §3.3).
 */
public interface ProiezioneDAO {

    /** @return la proiezione, oppure {@code null} se l'id non esiste */
    Proiezione findById(long idProiezione) throws SQLException;

    /** Ricerca combinabile: ogni criterio {@code null} viene ignorato. */
    List<Proiezione> cerca(String titolo, String genere, LocalDate dataDa, LocalDate dataA,
                            BigDecimal costoMin, BigDecimal costoMax) throws SQLException;

    /** Proiezioni nei tre mesi successivi a oggi per un film (titolo anche parziale). */
    List<Proiezione> prossimiTreMesiPerFilm(String titoloParziale) throws SQLException;

    /** Proiezioni successive a oggi, ordinate cronologicamente. */
    List<Proiezione> pianificate() throws SQLException;

    /** Proiezioni precedenti a oggi, dalla piu' recente. */
    List<Proiezione> storiche() throws SQLException;

    /**
     * @return l'id assegnato alla nuova proiezione
     * @throws SQLException se il trigger {@code trg_sovrapposizione_proiezione}
     *      rifiuta l'inserimento (proiezione sovrapposta a un'altra, sala unica)
     */
    long inserisci(long idFilm, LocalDate data, LocalTime ora, BigDecimal costoBiglietto) throws SQLException;

    /**
     * @throws SQLException se il trigger {@code trg_proiezione_immutabile_update}
     *      rifiuta la modifica (esistono prenotazioni) o se la nuova sovrapposizione
     *      viene rifiutata da {@code trg_sovrapposizione_proiezione}
     */
    void aggiorna(long idProiezione, LocalDate nuovaData, LocalTime nuovaOra, BigDecimal nuovoCosto) throws SQLException;

    /**
     * @throws SQLException se il trigger {@code trg_proiezione_immutabile_delete}
     *      rifiuta la cancellazione (esistono prenotazioni)
     */
    void elimina(long idProiezione) throws SQLException;
}
