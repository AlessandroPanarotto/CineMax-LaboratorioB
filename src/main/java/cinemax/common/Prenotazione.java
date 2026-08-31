/*
 * Progetto CineMax - Laboratorio Interdisciplinare B (a.a. 2025/2026)
 * Universita' degli Studi dell'Insubria
 *
 * Autore: Panarotto Alessandro - matricola 757930 - sede di Varese (VA)
 */
package cinemax.common;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Rappresenta la prenotazione di un certo numero di posti (numPosti) per una
 * proiezione, fatta da un cliente (tabella "prenotazioni").
 *
 * Insieme all'id del cliente teniamo anche nome e cognome: cosi' il
 * bigliettaio puo' visualizzarli subito senza dover fare un'altra chiamata
 * al server. Per il cliente non e' un problema, dato che sono i suoi
 * stessi dati.
 *
 * Il costo totale non viene salvato nel database ma calcolato al volo
 * (vedi getCostoTotale sotto): questo va bene perche' una volta che una
 * proiezione ha delle prenotazioni non puo' piu' essere modificata, quindi
 * il prezzo del biglietto usato nel calcolo resta sempre valido.
 */
public class Prenotazione implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String codicePrenotazione;
    private final long idUtente;
    private final String nomeCliente;
    private final String cognomeCliente;
    private final Proiezione proiezione;
    private final int numPosti;
    private final LocalDateTime dataPrenotazione;

    // costruttore che riceve tutti i dati della prenotazione, gia' pronti (arrivano dal server)
    public Prenotazione(String codicePrenotazione, long idUtente, String nomeCliente,
                         String cognomeCliente, Proiezione proiezione, int numPosti,
                         LocalDateTime dataPrenotazione) {
        this.codicePrenotazione = codicePrenotazione;
        this.idUtente = idUtente;
        this.nomeCliente = nomeCliente;
        this.cognomeCliente = cognomeCliente;
        this.proiezione = proiezione;
        this.numPosti = numPosti;
        this.dataPrenotazione = dataPrenotazione;
    }

    public String getCodicePrenotazione() {
        return codicePrenotazione;
    }

    public long getIdUtente() {
        return idUtente;
    }

    public String getNomeCliente() {
        return nomeCliente;
    }

    public String getCognomeCliente() {
        return cognomeCliente;
    }

    public Proiezione getProiezione() {
        return proiezione;
    }

    public int getNumPosti() {
        return numPosti;
    }

    public LocalDateTime getDataPrenotazione() {
        return dataPrenotazione;
    }

    // il costo totale e' semplicemente il prezzo di un biglietto moltiplicato per il numero di posti
    public BigDecimal getCostoTotale() {
        return proiezione.getCostoBiglietto().multiply(BigDecimal.valueOf(numPosti));
    }

    @Override
    public String toString() {
        return codicePrenotazione + " - " + proiezione + " (" + numPosti + " posti)";
    }
}
