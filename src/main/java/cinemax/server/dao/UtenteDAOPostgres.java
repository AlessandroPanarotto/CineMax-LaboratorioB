package cinemax.server.dao;

import cinemax.server.db.ConnectionManager;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

/** Implementazione PostgreSQL di {@link UtenteDAO} (vedi {@code doc/02_query_servizi.md}). */
public class UtenteDAOPostgres implements UtenteDAO {

    @Override
    public UtenteRow autentica(String username, String password) throws SQLException {
        // Query estesa rispetto a doc/02_query_servizi.md (che seleziona solo
        // id_utente, nome, cognome, ruolo): qui si aggiungono anche username,
        // data_nascita e luogo_domicilio per poter costruire un oggetto
        // Utente completo lato server (vedi UtenteFactory), a parita' di
        // condizione WHERE (nessuna modifica alla logica di autenticazione).
        String sql = "SELECT id_utente, nome, cognome, username, data_nascita, luogo_domicilio, ruolo " +
                "FROM utenti WHERE username = ? AND password_hash = crypt(?, password_hash)";
        try (Connection conn = ConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return mapRow(rs);
            }
        }
    }

    @Override
    public boolean existsUsername(String username) throws SQLException {
        String sql = "SELECT 1 FROM utenti WHERE username = ?";
        try (Connection conn = ConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    @Override
    public long inserisciCliente(String nome, String cognome, String username, String password,
                                  LocalDate dataNascita, String luogoDomicilio) throws SQLException {
        String sql = "INSERT INTO utenti (nome, cognome, username, password_hash, data_nascita, luogo_domicilio, ruolo) " +
                "VALUES (?, ?, ?, crypt(?, gen_salt('bf')), ?, ?, 'cliente') RETURNING id_utente";
        try (Connection conn = ConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nome);
            ps.setString(2, cognome);
            ps.setString(3, username);
            ps.setString(4, password);
            if (dataNascita != null) {
                ps.setDate(5, Date.valueOf(dataNascita));
            } else {
                ps.setNull(5, java.sql.Types.DATE);
            }
            ps.setString(6, luogoDomicilio);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    private UtenteRow mapRow(ResultSet rs) throws SQLException {
        Date dataNascitaSql = rs.getDate("data_nascita");
        LocalDate dataNascita = dataNascitaSql != null ? dataNascitaSql.toLocalDate() : null;
        return new UtenteRow(
                rs.getLong("id_utente"),
                rs.getString("nome"),
                rs.getString("cognome"),
                rs.getString("username"),
                dataNascita,
                rs.getString("luogo_domicilio"),
                rs.getString("ruolo"));
    }
}
