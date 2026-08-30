package cinemax.server.dao;

import java.sql.SQLException;
import java.time.LocalDate;

/** Accesso alla tabella {@code utenti} (registrazione e autenticazione). */
public interface UtenteDAO {

    /**
     * Verifica username e password (confronto cifrato lato database con
     * {@code crypt()}, vedi {@code schema_cinemax.sql}).
     *
     * @return la riga dell'utente autenticato, oppure {@code null} se le
     *      credenziali non sono valide
     */
    UtenteRow autentica(String username, String password) throws SQLException;

    /** @return {@code true} se lo username e' gia' registrato */
    boolean existsUsername(String username) throws SQLException;

    /**
     * Inserisce un nuovo utente con ruolo {@code cliente} (unico ruolo che un
     * utente puo' auto-registrarsi: proiezionisti e bigliettai sono inseriti
     * come dati di seed, vedi {@code schema_cinemax.sql}).
     *
     * @return l'id assegnato al nuovo utente
     */
    long inserisciCliente(String nome, String cognome, String username, String password,
                           LocalDate dataNascita, String luogoDomicilio) throws SQLException;
}
