package cinemax.server.dao;

import cinemax.common.Film;
import cinemax.server.db.ConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Implementazione PostgreSQL di {@link FilmDAO}. */
public class FilmDAOPostgres implements FilmDAO {

    @Override
    public List<Film> cercaPerTitolo(String titoloParziale) throws SQLException {
        String sql = "SELECT id_film, titolo, genere, regista, anno, durata_minuti, eta_minima " +
                "FROM film WHERE titolo ILIKE '%' || ? || '%' ORDER BY titolo";
        try (Connection conn = ConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, titoloParziale == null ? "" : titoloParziale);
            try (ResultSet rs = ps.executeQuery()) {
                List<Film> risultato = new ArrayList<>();
                while (rs.next()) {
                    risultato.add(mapRow(rs));
                }
                return risultato;
            }
        }
    }

    @Override
    public Film findById(long idFilm) throws SQLException {
        String sql = "SELECT id_film, titolo, genere, regista, anno, durata_minuti, eta_minima " +
                "FROM film WHERE id_film = ?";
        try (Connection conn = ConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idFilm);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    @Override
    public long inserisci(String titolo, String genere, String regista, int anno,
                           int durataMinuti, int etaMinima) throws SQLException {
        String sql = "INSERT INTO film (titolo, genere, regista, anno, durata_minuti, eta_minima) " +
                "VALUES (?, ?, ?, ?, ?, ?) RETURNING id_film";
        try (Connection conn = ConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, titolo);
            ps.setString(2, genere);
            ps.setString(3, regista);
            ps.setInt(4, anno);
            ps.setInt(5, durataMinuti);
            ps.setInt(6, etaMinima);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    private Film mapRow(ResultSet rs) throws SQLException {
        return new Film(
                rs.getLong("id_film"),
                rs.getString("titolo"),
                rs.getString("genere"),
                rs.getString("regista"),
                rs.getInt("anno"),
                rs.getInt("durata_minuti"),
                rs.getInt("eta_minima"));
    }
}
