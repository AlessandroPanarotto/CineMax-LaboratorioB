package cinemax.server.dao;

import java.sql.SQLException;
import java.time.LocalDate;

/** DAO per la tabella "utenti": login e registrazione dei clienti. */
public interface UtenteDAO {

    /**
     * Controlla se username e password corrispondono a un utente esistente
     * (la password nel database e' cifrata, il confronto viene fatto
     * direttamente in SQL con la funzione crypt(), vedi schema_cinemax.sql).
     *
     * Restituisce i dati dell'utente se le credenziali sono corrette,
     * altrimenti null.
     */
    UtenteRow autentica(String username, String password) throws SQLException;

    /** Restituisce true se esiste gia' un utente con quello username. */
    boolean existsUsername(String username) throws SQLException;

    /**
     * Registra un nuovo cliente. Il ruolo e' fissato a "cliente": e' l'unico
     * ruolo che un utente puo' scegliere registrandosi da solo, gli altri
     * ruoli (proiezionista, bigliettaio) vengono inseriti direttamente nel
     * database come dati iniziali.
     *
     * Restituisce l'id assegnato al nuovo utente.
     */
    long inserisciCliente(String nome, String cognome, String username, String password,
                           LocalDate dataNascita, String luogoDomicilio) throws SQLException;
}
