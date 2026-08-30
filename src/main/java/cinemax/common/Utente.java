package cinemax.common;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Utente registrato di CineMax. Classe astratta: ogni istanza concreta e'
 * {@link Cliente}, {@link Proiezionista} o {@link Bigliettaio} (vedi
 * {@code doc/uml/classi_dominio.puml}). Corrisponde alla riga della tabella
 * {@code utenti} nel database, dove il ruolo e' l'attributo discriminante
 * che sostituisce la gerarchia ISA concettuale (vedi
 * {@code doc/01_progettazione_database.md}, §3.1).
 *
 * <p>Attraversa la connessione RMI come oggetto serializzato: non contiene
 * la password (in nessuna forma) tra i suoi campi.</p>
 */
public abstract class Utente implements Serializable {

    private static final long serialVersionUID = 1L;

    private final long idUtente;
    private final String nome;
    private final String cognome;
    private final String username;
    private final LocalDate dataNascita;   // puo' essere null
    private final String luogoDomicilio;

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

    /** Nome del ruolo cosi' come memorizzato nella colonna {@code utenti.ruolo}. */
    public abstract String getRuolo();

    @Override
    public String toString() {
        return nome + " " + cognome + " (" + username + ", " + getRuolo() + ")";
    }
}
