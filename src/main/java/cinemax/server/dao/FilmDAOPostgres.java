package cinemax.server.dao;

import cinemax.common.Film;
import cinemax.server.db.ConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Implementazione di FilmDAO che parla con PostgreSQL usando JDBC. */
public class FilmDAOPostgres implements FilmDAO {

    @Override
    public List<Film> cercaPerTitolo(String titoloParziale) throws SQLException {
        // ILIKE '%...%' fa una ricerca "contiene", senza distinguere maiuscole/minuscole.
        String sql = "SELECT id_film, titolo, genere, regista, anno, durata_minuti, eta_minima " +
                "FROM film WHERE titolo ILIKE '%' || ? || '%' ORDER BY titolo";
        // Prendiamo una connessione dal pool (ConnectionManager) e la statement
        // preparata dentro lo stesso try-with-resources: alla fine del blocco
        // vengono chiuse automaticamente in ordine inverso, anche se c'e' un'eccezione.
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
                // rs.next() sposta il cursore sulla prima (ed eventualmente unica) riga:
                // se non c'e' nessuna riga (id inesistente) restituiamo null.
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    @Override
    public long inserisci(String titolo, String genere, String regista, int anno,
                           int durataMinuti, int etaMinima) throws SQLException {
        // RETURNING id_film e' una comodita' di PostgreSQL: fa tornare indietro
        // l'id generato dal database senza bisogno di una seconda query.
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
                rs.next(); // c'e' sempre esattamente una riga di ritorno, dato che l'INSERT e' appena andato a buon fine
                return rs.getLong(1);
            }
        }
    }

    // Metodo di comodo: trasforma la riga corrente del ResultSet in un oggetto Film.
    // Viene richiamato da tutti i metodi sopra per non ripetere lo stesso codice tre volte.
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
