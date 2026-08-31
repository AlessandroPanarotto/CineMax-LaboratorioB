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
package cinemax.common;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Classe astratta che rappresenta un utente registrato di CineMax.
 * Un utente vero e proprio non e' mai un Utente "generico": e' sempre
 * uno tra {@link Cliente}, {@link Proiezionista} e {@link Bigliettaio},
 * che estendono questa classe e ne implementano il ruolo.
 *
 * Nel database corrisponde a una riga della tabella "utenti", dove il
 * ruolo e' salvato in una colonna (non ci sono tabelle separate per
 * ogni tipo di utente).
 *
 * Nota: questa classe viaggia tra client e server tramite RMI (per
 * questo implementa Serializable), quindi NON deve mai contenere la
 * password dell'utente tra i suoi campi.
 */
public abstract class Utente implements Serializable {

    private static final long serialVersionUID = 1L;

    private final long idUtente;
    private final String nome;
    private final String cognome;
    private final String username;
    private final LocalDate dataNascita;   // puo' essere null, non e' obbligatoria
    private final String luogoDomicilio;

    // costruttore comune a tutte le sottoclassi: viene richiamato con super(...)
    protected Utente(long idUtente, String nome, String cognome, String username,
                      LocalDate dataNascita, String luogoDomicilio) {
        this.idUtente = idUtente;
        this.nome = nome;
        this.cognome = cognome;
        this.username = username;
        this.dataNascita = dataNascita;
        this.luogoDomicilio = luogoDomicilio;
    }

    public long getIdUtente() {
        return idUtente;
    }

    public String getNome() {
        return nome;
    }

    public String getCognome() {
        return cognome;
    }

    public String getUsername() {
        return username;
    }

    public LocalDate getDataNascita() {
        return dataNascita;
    }

    public String getLuogoDomicilio() {
        return luogoDomicilio;
    }

    // ogni sottoclasse restituisce il proprio ruolo (cliente, proiezionista o bigliettaio)
    public abstract String getRuolo();

    @Override
    public String toString() {
        return nome + " " + cognome + " (" + username + ", " + getRuolo() + ")";
    }
}
