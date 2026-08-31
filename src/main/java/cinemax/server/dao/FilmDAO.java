/*
 * Progetto CineMax - Laboratorio Interdisciplinare B (a.a. 2025/2026)
 * Universita' degli Studi dell'Insubria
 *
 * Autore: Panarotto Alessandro - matricola 757930 - sede di Varese (VA)
 */
package cinemax.server.dao;

import cinemax.common.Film;

import java.sql.SQLException;
import java.util.List;

/**
 * DAO (Data Access Object) per la tabella "film": qui dentro ci sono solo le
 * firme dei metodi, la vera implementazione con le query SQL sta in
 * FilmDAOPostgres. Separare interfaccia e implementazione permette in teoria
 * di cambiare database senza toccare il resto del codice (i service usano
 * solo questa interfaccia, non conoscono PostgreSQL).
 */
public interface FilmDAO {

    /** Cerca i film il cui titolo contiene la stringa data (ricerca parziale, non case sensitive), ordinati per titolo. */
    List<Film> cercaPerTitolo(String titoloParziale) throws SQLException;

    /** Cerca un film per id. Restituisce null se non esiste. */
    Film findById(long idFilm) throws SQLException;

    /** Inserisce un nuovo film e restituisce l'id che il database gli ha assegnato. */
    long inserisci(String titolo, String genere, String regista, int anno,
                    int durataMinuti, int etaMinima) throws SQLException;
}
