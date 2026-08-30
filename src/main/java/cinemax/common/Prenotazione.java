package cinemax.common;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Prenotazione di {@code numPosti} posti per una {@link Proiezione}, effettuata
 * da un utente con ruolo {@code cliente} (tabella {@code prenotazioni}).
 *
 * <p>Porta con se' anche nome/cognome del cliente che ha effettuato la
 * prenotazione: comodo lato bigliettaio (che deve poterli visualizzare senza
 * un'ulteriore chiamata RMI) e non problematico lato cliente (sono i propri
 * dati). {@code costoTotale} e' un dato derivato non persistito (§3.3 di
 * {@code doc/01_progettazione_database.md}): una proiezione con prenotazioni
 * associate non e' piu' modificabile, quindi {@code numPosti * costoBiglietto}
 * resta valido per tutta la vita della prenotazione.</p>
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

    /** {@code numPosti * proiezione.getCostoBiglietto()} — dato derivato, non persistito. */
    public BigDecimal getCostoTotale() {
        return proiezione.getCostoBiglietto().multiply(BigDecimal.valueOf(numPosti));
    }

    @Override
    public String toString() {
        return codicePrenotazione + " - " + proiezione + " (" + numPosti + " posti)";
    }
}
