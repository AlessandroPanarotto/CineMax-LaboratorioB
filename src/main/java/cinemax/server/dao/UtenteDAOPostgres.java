package cinemax.server.dao;

import cinemax.server.db.ConnectionManager;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

/** Implementazione di UtenteDAO con query JDBC su PostgreSQL. */
public class UtenteDAOPostgres implements UtenteDAO {

    @Override
    public UtenteRow autentica(String username, String password) throws SQLException {
        // crypt(?, password_hash) rifa' l'hash della password inserita usando
        // lo stesso "sale" gia' salvato nel database, e il confronto con
        // password_hash avviene direttamente in SQL: in nessun momento
        // confrontiamo la password in chiaro noi in Java.
        // In piu' rispetto alle sole colonne necessarie per il login (id,
        // nome, cognome, ruolo) leggiamo anche username, data_nascita e
        // luogo_domicilio, che servono per costruire l'oggetto Utente
        // completo lato server (vedi UtenteFactory): la condizione WHERE
        // resta comunque la stessa, cambia solo cosa selezioniamo.
        String sql = "SELECT id_utente, nome, cognome, username, data_nascita, luogo_domicilio, ruolo " +
                "FROM utenti WHERE username = ? AND password_hash = crypt(?, password_hash)";
        try (Connection conn = ConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    // Nessuna riga trovata: o lo username non esiste, o la password non corrisponde.
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
                // Non ci serve leggere il valore, ci basta sapere se una riga c'e' o no.
                return rs.next();
            }
        }
    }

    @Override
    public long inserisciCliente(String nome, String cognome, String username, String password,
                                  LocalDate dataNascita, String luogoDomicilio) throws SQLException {
        // La password non viene mai salvata in chiaro: crypt(?, gen_salt('bf'))
        // genera un sale casuale (algoritmo Blowfish) e salva direttamente
        // l'hash. Il ruolo e' fisso a 'cliente', come da interfaccia.
        String sql = "INSERT INTO utenti (nome, cognome, username, password_hash, data_nascita, luogo_domicilio, ruolo) " +
                "VALUES (?, ?, ?, crypt(?, gen_salt('bf')), ?, ?, 'cliente') RETURNING id_utente";
        try (Connection conn = ConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nome);
            ps.setString(2, cognome);
            ps.setString(3, username);
            ps.setString(4, password);
            // La data di nascita e' opzionale: se non e' stata fornita impostiamo NULL sulla colonna.
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

    // Trasforma la riga corrente del ResultSet in un UtenteRow (dato "grezzo",
    // ancora senza sapere se e' un Cliente/Proiezionista/Bigliettaio: ci pensa UtenteFactory).
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
