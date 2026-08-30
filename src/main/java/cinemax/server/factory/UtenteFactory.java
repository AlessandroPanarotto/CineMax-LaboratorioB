package cinemax.server.factory;

import cinemax.common.Bigliettaio;
import cinemax.common.Cliente;
import cinemax.common.Proiezionista;
import cinemax.common.Utente;
import cinemax.server.dao.UtenteRow;

/**
 * Factory Method: legge il campo {@code ruolo} di un {@link UtenteRow} (dato
 * grezzo dal database) e istanzia la sottoclasse di {@link Utente} corretta.
 * Fa da ponte fra la tabella unica {@code utenti} del database (§3.1 di
 * {@code doc/01_progettazione_database.md}) e la gerarchia OOP del dominio
 * (vedi {@code doc/uml/classi_dominio.puml} e §2 di
 * {@code doc/03_progettazione_uml.md}).
 */
public final class UtenteFactory {

    private UtenteFactory() {
    }

    public static Utente creaUtente(UtenteRow row) {
        return switch (row.ruolo) {
            case "cliente" -> new Cliente(
                    row.idUtente, row.nome, row.cognome, row.username, row.dataNascita, row.luogoDomicilio);
            case "proiezionista" -> new Proiezionista(
                    row.idUtente, row.nome, row.cognome, row.username, row.dataNascita, row.luogoDomicilio);
            case "bigliettaio" -> new Bigliettaio(
                    row.idUtente, row.nome, row.cognome, row.username, row.dataNascita, row.luogoDomicilio);
            default -> throw new IllegalStateException("Ruolo utente sconosciuto: " + row.ruolo);
        };
    }
}
