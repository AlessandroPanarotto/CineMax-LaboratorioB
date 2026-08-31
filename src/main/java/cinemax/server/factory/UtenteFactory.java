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
package cinemax.server.factory;

import cinemax.common.Bigliettaio;
import cinemax.common.Cliente;
import cinemax.common.Proiezionista;
import cinemax.common.Utente;
import cinemax.server.dao.UtenteRow;

/**
 * Questa classe implementa il pattern Factory Method: dato un UtenteRow (il
 * dato "grezzo" che arriva dal database, dove tutti gli utenti stanno in
 * un'unica tabella "utenti") crea l'oggetto Java della sottoclasse giusta di
 * Utente, guardando il valore del campo ruolo. In questo modo il resto del
 * codice (i service) chiede semplicemente "dammi l'oggetto Utente" senza
 * doversi occupare di distinguere i tre casi.
 */
public final class UtenteFactory {

    // Classe di solo metodi statici: costruttore privato per non farla istanziare.
    private UtenteFactory() {
    }

    public static Utente creaUtente(UtenteRow row) {
        // In base al valore letto dal database creiamo l'oggetto della
        // sottoclasse corrispondente. I dati anagrafici (id, nome, cognome,
        // username, data di nascita, luogo di domicilio) sono uguali in
        // tutti e tre i casi, cambia solo la classe che viene istanziata.
        switch (row.ruolo) {
            case "cliente":
                return new Cliente(
                        row.idUtente, row.nome, row.cognome, row.username, row.dataNascita, row.luogoDomicilio);
            case "proiezionista":
                return new Proiezionista(
                        row.idUtente, row.nome, row.cognome, row.username, row.dataNascita, row.luogoDomicilio);
            case "bigliettaio":
                return new Bigliettaio(
                        row.idUtente, row.nome, row.cognome, row.username, row.dataNascita, row.luogoDomicilio);
            default:
                // Non dovrebbe mai succedere se il database e' popolato correttamente
                // (il ruolo ha un vincolo CHECK sui tre valori validi), ma lo
                // gestiamo comunque per non lasciare il metodo senza un return.
                throw new IllegalStateException("Ruolo utente sconosciuto: " + row.ruolo);
        }
    }
}
