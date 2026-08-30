package cinemax.server.dao;

import cinemax.common.Film;
import cinemax.common.Prenotazione;
import cinemax.common.Proiezione;
import cinemax.server.db.ConnectionManager;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Implementazione PostgreSQL di {@link PrenotazioneDAO} (vedi {@code doc/02_query_servizi.md}). */
public class PrenotazioneDAOPostgres implements PrenotazioneDAO {

    private static final String COLONNE_JOIN =
            "pr.codice_prenotazione, pr.id_utente, u.nome, u.cognome, " +
            "p.id_proiezione, f.id_film, f.titolo, f.genere, f.regista, f.anno, f.durata_minuti, f.eta_minima, " +
            "p.data_proiezione, p.ora_proiezione, p.costo_biglietto, " +
            "pr.num_posti, pr.data_prenotazione";

    private static final String FROM_JOIN =
            "FROM prenotazioni pr " +
            "JOIN utenti u     ON u.id_utente = pr.id_utente " +
            "JOIN proiezioni p ON p.id_proiezione = pr.id_proiezione " +
            "JOIN film f       ON f.id_film = p.id_film ";

    @Override
    public String inserisci(long idUtente, long idProiezione, int numPosti) throws SQLException {
        String sql = "INSERT INTO prenotazioni (id_utente, id_proiezione, num_posti) " +
                "VALUES (?, ?, ?) RETURNING codice_prenotazione";
        try (Connection conn = ConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idUtente);
            ps.setLong(2, idProiezione);
            ps.setInt(3, numPosti);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getString(1);
            }
        }
    }

    @Override
    public List<Prenotazione> findByUtente(long idUtente) throws SQLException {
        String sql = "SELECT " + COLONNE_JOIN + " " + FROM_JOIN +
                "WHERE pr.id_utente = ? AND p.data_proiezione > CURRENT_DATE " +
                "ORDER BY p.data_proiezione, p.ora_proiezione";
        try (Connection conn = ConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idUtente);
            return eseguiLista(ps);
        }
    }

    @Override
    public void aggiornaProiezione(String codicePrenotazione, long nuovaIdProiezione) throws SQLException {
        String sql = "UPDATE prenotazioni SET id_proiezione = ? WHERE codice_prenotazione = ?";
        try (Connection conn = ConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, nuovaIdProiezione);
            ps.setString(2, codicePrenotazione);
            ps.executeUpdate();
        }
    }

    @Override
    public void elimina(String codicePrenotazione) throws SQLException {
        String sql = "DELETE FROM prenotazioni WHERE codice_prenotazione = ?";
        try (Connection conn = ConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, codicePrenotazione);
            ps.executeUpdate();
        }
    }

    @Override
    public List<Prenotazione> odierne() throws SQLException {
        String sql = "SELECT " + COLONNE_JOIN + " " + FROM_JOIN +
                "WHERE p.data_proiezione = CURRENT_DATE ORDER BY p.ora_proiezione";
        try (Connection conn = ConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            return eseguiLista(ps);
        }
    }

    @Override
    public List<Prenotazione> cerca(String codice, String nomeCognome, String titoloFilm,
                                     LocalDate dataDa, LocalDate dataA) throws SQLException {
        String sql = "SELECT " + COLONNE_JOIN + " " + FROM_JOIN +
                "WHERE (?::text IS NULL OR pr.codice_prenotazione = ?::text) " +
                "AND (?::text IS NULL OR u.nome ILIKE '%' || ?::text || '%' OR u.cognome ILIKE '%' || ?::text || '%') " +
                "AND (?::text IS NULL OR f.titolo ILIKE '%' || ?::text || '%') " +
                "AND (?::date IS NULL OR p.data_proiezione >= ?::date) " +
                "AND (?::date IS NULL OR p.data_proiezione <= ?::date) " +
                "ORDER BY p.data_proiezione, p.ora_proiezione";
        try (Connection conn = ConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setNullableString(ps, 1, codice);
            setNullableString(ps, 2, codice);
            setNullableString(ps, 3, nomeCognome);
            setNullableString(ps, 4, nomeCognome);
            setNullableString(ps, 5, nomeCognome);
            setNullableString(ps, 6, titoloFilm);
            setNullableString(ps, 7, titoloFilm);
            setNullableDate(ps, 8, dataDa);
            setNullableDate(ps, 9, dataDa);
            setNullableDate(ps, 10, dataA);
            setNullableDate(ps, 11, dataA);
            return eseguiLista(ps);
        }
    }

    @Override
    public Prenotazione findByCodice(String codicePrenotazione) throws SQLException {
        String sql = "SELECT " + COLONNE_JOIN + " " + FROM_JOIN + "WHERE pr.codice_prenotazione = ?";
        try (Connection conn = ConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, codicePrenotazione);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    @Override
    public LocalDate dataProiezioneDiPrenotazione(String codicePrenotazione) throws SQLException {
        String sql = "SELECT p.data_proiezione FROM prenotazioni pr " +
                "JOIN proiezioni p ON p.id_proiezione = pr.id_proiezione " +
                "WHERE pr.codice_prenotazione = ?";
        try (Connection conn = ConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, codicePrenotazione);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDate(1).toLocalDate() : null;
            }
        }
    }

    private List<Prenotazione> eseguiLista(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            List<Prenotazione> risultato = new ArrayList<>();
            while (rs.next()) {
                risultato.add(mapRow(rs));
            }
            return risultato;
        }
    }

    private Prenotazione mapRow(ResultSet rs) throws SQLException {
        Film film = new Film(
                rs.getLong("id_film"),
                rs.getString("titolo"),
                rs.getString("genere"),
                rs.getString("regista"),
                rs.getInt("anno"),
                rs.getInt("durata_minuti"),
                rs.getInt("eta_minima"));
        Proiezione proiezione = new Proiezione(
                rs.getLong("id_proiezione"),
                film,
                rs.getDate("data_proiezione").toLocalDate(),
                rs.getTime("ora_proiezione").toLocalTime(),
                rs.getBigDecimal("costo_biglietto"),
                0); // posti_liberi non necessario in questa vista: non fa parte della join prenotazioni
        return new Prenotazione(
                rs.getString("codice_prenotazione"),
                rs.getLong("id_utente"),
                rs.getString("nome"),
                rs.getString("cognome"),
                proiezione,
                rs.getInt("num_posti"),
                rs.getTimestamp("data_prenotazione").toLocalDateTime());
    }

    private void setNullableString(PreparedStatement ps, int index, String value) throws SQLException {
        if (value == null || value.isBlank()) {
            ps.setNull(index, Types.VARCHAR);
        } else {
            ps.setString(index, value);
        }
    }

    private void setNullableDate(PreparedStatement ps, int index, LocalDate value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.DATE);
        } else {
            ps.setDate(index, Date.valueOf(value));
        }
    }
}
