package cinemax.server.dao;

import java.time.LocalDate;

/**
 * Dato "grezzo" restituito da {@link UtenteDAO}: i campi di una riga della
 * tabella {@code utenti}, senza ancora sapere se corrisponde a un
 * {@code Cliente}, {@code Proiezionista} o {@code Bigliettaio}. E'
 * {@code cinemax.server.factory.UtenteFactory} a leggere il campo
 * {@link #ruolo} e istanziare la sottoclasse Java corretta (vedi
 * {@code doc/uml/sequenza_login.puml}).
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
