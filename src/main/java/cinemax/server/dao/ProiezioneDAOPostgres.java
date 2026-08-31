/*
 * Progetto CineMax - Laboratorio Interdisciplinare B (a.a. 2025/2026)
 * Universita' degli Studi dell'Insubria
 *
 * Autore: Panarotto Alessandro - matricola 757930 - sede di Varese (VA)
 */
package cinemax.server.dao;

import cinemax.common.Film;
import cinemax.common.Proiezione;
import cinemax.server.db.ConnectionManager;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** Implementazione di ProiezioneDAO con query JDBC su PostgreSQL. */
public class ProiezioneDAOPostgres implements ProiezioneDAO {

    // Colonne comuni a tutte le query di sola lettura, che leggono dalla
    // vista v_proiezioni_disponibilita (unisce gia' proiezioni + film e
    // calcola i posti liberi, quindi qui non serve scrivere JOIN).
    private static final String COLONNE_VISTA =
            "id_proiezione, id_film, titolo, genere, regista, anno, durata_minuti, " +
            "eta_minima, data_proiezione, ora_proiezione, costo_biglietto, posti_liberi";

    @Override
    public Proiezione findById(long idProiezione) throws SQLException {
        String sql = "SELECT " + COLONNE_VISTA + " FROM v_proiezioni_disponibilita WHERE id_proiezione = ?";
        try (Connection conn = ConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idProiezione);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    @Override
    public List<Proiezione> cerca(String titolo, String genere, LocalDate dataDa, LocalDate dataA,
                                   BigDecimal costoMin, BigDecimal costoMax) throws SQLException {
        // Stesso schema di filtri opzionali usato in PrenotazioneDAOPostgres.cerca():
        // per ogni criterio, "?::tipo IS NULL OR condizione" fa si' che un
        // parametro null non filtri nulla. Ogni criterio compare due volte
        // nella query (controllo + confronto), quindi va impostato due volte
        // sugli indici corrispondenti dei parametri.
        String sql = "SELECT " + COLONNE_VISTA + " FROM v_proiezioni_disponibilita " +
                "WHERE (?::text IS NULL OR titolo ILIKE '%' || ?::text || '%') " +
                "AND (?::text IS NULL OR genere = ?::text) " +
                "AND (?::date IS NULL OR data_proiezione >= ?::date) " +
                "AND (?::date IS NULL OR data_proiezione <= ?::date) " +
                "AND (?::numeric IS NULL OR costo_biglietto >= ?::numeric) " +
                "AND (?::numeric IS NULL OR costo_biglietto <= ?::numeric) " +
                "ORDER BY data_proiezione, ora_proiezione";
        try (Connection conn = ConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setNullableString(ps, 1, titolo);
            setNullableString(ps, 2, titolo);
            setNullableString(ps, 3, genere);
            setNullableString(ps, 4, genere);
            setNullableDate(ps, 5, dataDa);
            setNullableDate(ps, 6, dataDa);
            setNullableDate(ps, 7, dataA);
            setNullableDate(ps, 8, dataA);
            setNullableBigDecimal(ps, 9, costoMin);
            setNullableBigDecimal(ps, 10, costoMin);
            setNullableBigDecimal(ps, 11, costoMax);
            setNullableBigDecimal(ps, 12, costoMax);
            return eseguiLista(ps);
        }
    }

    @Override
    public List<Proiezione> prossimiTreMesiPerFilm(String titoloParziale) throws SQLException {
        String sql = "SELECT " + COLONNE_VISTA + " FROM v_proiezioni_disponibilita " +
                "WHERE titolo ILIKE '%' || ? || '%' " +
                "AND data_proiezione BETWEEN CURRENT_DATE AND (CURRENT_DATE + INTERVAL '3 months') " +
                "ORDER BY data_proiezione, ora_proiezione";
        try (Connection conn = ConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, titoloParziale == null ? "" : titoloParziale);
            return eseguiLista(ps);
        }
    }

    @Override
    public List<Proiezione> pianificate() throws SQLException {
        String sql = "SELECT " + COLONNE_VISTA + " FROM v_proiezioni_disponibilita " +
                "WHERE data_proiezione > CURRENT_DATE ORDER BY data_proiezione, ora_proiezione";
        try (Connection conn = ConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            return eseguiLista(ps);
        }
    }

    @Override
    public List<Proiezione> storiche() throws SQLException {
        String sql = "SELECT " + COLONNE_VISTA + " FROM v_proiezioni_disponibilita " +
                "WHERE data_proiezione < CURRENT_DATE ORDER BY data_proiezione DESC, ora_proiezione DESC";
        try (Connection conn = ConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            return eseguiLista(ps);
        }
    }

    @Override
    public long inserisci(long idFilm, LocalDate data, LocalTime ora, BigDecimal costoBiglietto) throws SQLException {
        // Nota: qui l'INSERT e' sulla tabella "vera" proiezioni, non sulla
        // vista (le viste normalmente non si possono modificare direttamente).
        // Se la nuova proiezione si sovrappone a un'altra, un trigger del
        // database blocca l'inserimento e viene lanciata una SQLException.
        String sql = "INSERT INTO proiezioni (id_film, data_proiezione, ora_proiezione, costo_biglietto) " +
                "VALUES (?, ?, ?, ?) RETURNING id_proiezione";
        try (Connection conn = ConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idFilm);
            ps.setDate(2, Date.valueOf(data));
            ps.setTime(3, Time.valueOf(ora));
            ps.setBigDecimal(4, costoBiglietto);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    @Override
    public void aggiorna(long idProiezione, LocalDate nuovaData, LocalTime nuovaOra, BigDecimal nuovoCosto)
            throws SQLException {
        String sql = "UPDATE proiezioni SET data_proiezione = ?, ora_proiezione = ?, costo_biglietto = ? " +
                "WHERE id_proiezione = ?";
        try (Connection conn = ConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(nuovaData));
            ps.setTime(2, Time.valueOf(nuovaOra));
            ps.setBigDecimal(3, nuovoCosto);
            ps.setLong(4, idProiezione);
            ps.executeUpdate();
        }
    }

    @Override
    public void elimina(long idProiezione) throws SQLException {
        String sql = "DELETE FROM proiezioni WHERE id_proiezione = ?";
        try (Connection conn = ConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, idProiezione);
            ps.executeUpdate();
        }
    }

    // Metodo di comodo: esegue la query e trasforma tutte le righe restituite in una List<Proiezione>.
    private List<Proiezione> eseguiLista(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            List<Proiezione> risultato = new ArrayList<>();
            while (rs.next()) {
                risultato.add(mapRow(rs));
            }
            return risultato;
        }
    }

    // Trasforma la riga corrente del ResultSet in un oggetto Proiezione (che contiene anche il Film collegato).
    private Proiezione mapRow(ResultSet rs) throws SQLException {
        Film film = new Film(
                rs.getLong("id_film"),
                rs.getString("titolo"),
                rs.getString("genere"),
                rs.getString("regista"),
                rs.getInt("anno"),
                rs.getInt("durata_minuti"),
                rs.getInt("eta_minima"));
        return new Proiezione(
                rs.getLong("id_proiezione"),
                film,
                rs.getDate("data_proiezione").toLocalDate(),
                rs.getTime("ora_proiezione").toLocalTime(),
                rs.getBigDecimal("costo_biglietto"),
                rs.getInt("posti_liberi"));
    }

    // Imposta un parametro String, oppure NULL SQL se il valore e' assente
    // o vuoto (cosi' un filtro di ricerca "vuoto" viene ignorato).
    private void setNullableString(PreparedStatement ps, int index, String value) throws SQLException {
        if (value == null || value.isBlank()) {
            ps.setNull(index, Types.VARCHAR);
        } else {
            ps.setString(index, value);
        }
    }

    // Stessa idea di setNullableString ma per le date.
    private void setNullableDate(PreparedStatement ps, int index, LocalDate value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.DATE);
        } else {
            ps.setDate(index, Date.valueOf(value));
        }
    }

    // Stessa idea di setNullableString ma per gli importi (costo minimo/massimo del biglietto).
    private void setNullableBigDecimal(PreparedStatement ps, int index, BigDecimal value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.NUMERIC);
        } else {
            ps.setBigDecimal(index, value);
        }
    }
}
