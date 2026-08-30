package cinemax.server.dao;

import cinemax.common.Film;

import java.sql.SQLException;
import java.util.List;

/** Accesso alla tabella {@code film} (catalogo). */
public interface FilmDAO {

    /** Ricerca per titolo parziale (case-insensitive), ordinata per titolo. */
    List<Film> cercaPerTitolo(String titoloParziale) throws SQLException;

    /** @return il film, oppure {@code null} se l'id non esiste */
    Film findById(long idFilm) throws SQLException;

    /** @return l'id assegnato al nuovo film */
    long inserisci(String titolo, String genere, String regista, int anno,
                    int durataMinuti, int etaMinima) throws SQLException;
}
