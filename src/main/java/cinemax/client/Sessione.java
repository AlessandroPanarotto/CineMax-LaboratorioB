/*
 * Progetto CineMax - Laboratorio Interdisciplinare B (a.a. 2025/2026)
 * Universita' degli Studi dell'Insubria
 *
 * Autore: Panarotto Alessandro - matricola 757930 - sede di Varese (VA)
 */
package cinemax.client;

import cinemax.common.Cliente;
import cinemax.common.Utente;

/**
 * Rappresenta lo stato di login del client: memorizza semplicemente quale
 * utente ha effettuato l'accesso (se nessuno, siamo nella modalita' "guest"
 * e utenteCorrente resta null). MainFrame crea un'unica Sessione e la
 * condivide con tutti i controller, cosi' tutti sanno chi e' loggato senza
 * doverselo passare a mano in ogni metodo.
 */
public class Sessione {

    private Utente utenteCorrente;

    // true se qualcuno ha fatto login, false se siamo ancora in modalita' guest
    public boolean isAutenticato() {
        return utenteCorrente != null;
    }

    // utile per distinguere velocemente un cliente dagli altri ruoli (bigliettaio/proiezionista)
    public boolean isCliente() {
        return utenteCorrente instanceof Cliente;
    }

    public Utente getUtenteCorrente() {
        return utenteCorrente;
    }

    // salva l'utente autenticato (chiamato dal LoginController dopo un login/registrazione riusciti)
    public void login(Utente utente) {
        this.utenteCorrente = utente;
    }

    // torna in modalita' guest
    public void logout() {
        this.utenteCorrente = null;
    }
}
