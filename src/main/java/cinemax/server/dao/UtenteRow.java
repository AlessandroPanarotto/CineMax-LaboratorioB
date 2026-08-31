/*
 * Progetto CineMax - Laboratorio Interdisciplinare B (a.a. 2025/2026)
 * Universita' degli Studi dell'Insubria
 *
 * Autori:
 *   Panarotto Alessandro   - matricola 757930 - sede di Varese (VA)
 *   Calabrese Davide Paolo - matricola 763012 - sede di Varese (VA)
 *   Mohan Thomas Paolo     - matricola 761573 - sede di Varese (VA)
 *   Trentini Federico      - matricola 760478 - sede di Varese (VA)
 */
package cinemax.server.dao;

import java.time.LocalDate;

/**
 * Rappresenta una riga "grezza" della tabella utenti, cosi' come esce dal
 * database: a questo livello non sappiamo ancora se l'utente e' un Cliente,
 * un Proiezionista o un Bigliettaio, sappiamo solo il valore del campo
 * "ruolo". E' UtenteFactory (nel package factory) a leggere quel campo e
 * creare l'oggetto Java della sottoclasse giusta.
 */
public final class UtenteRow {

    public final long idUtente;
    public final String nome;
    public final String cognome;
    public final String username;
    public final LocalDate dataNascita;
    public final String luogoDomicilio;
    public final String ruolo;

    public UtenteRow(long idUtente, String nome, String cognome, String username,
                      LocalDate dataNascita, String luogoDomicilio, String ruolo) {
        this.idUtente = idUtente;
        this.nome = nome;
        this.cognome = cognome;
        this.username = username;
        this.dataNascita = dataNascita;
        this.luogoDomicilio = luogoDomicilio;
        this.ruolo = ruolo;
    }
}
