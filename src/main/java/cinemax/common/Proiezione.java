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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Rappresenta una singola proiezione: un film mostrato in una certa data e
 * ora, con il relativo prezzo del biglietto (tabella "proiezioni").
 *
 * Il campo postiLiberi non e' salvato direttamente nel database, ma viene
 * calcolato dal server come differenza tra i 200 posti della sala e il
 * numero di posti gia' prenotati per questa proiezione.
 */
public class Proiezione implements Serializable {

    private static final long serialVersionUID = 1L;

    private final long idProiezione;
    private final Film film;
    private final LocalDate dataProiezione;
    private final LocalTime oraProiezione;
    private final BigDecimal costoBiglietto;
    private final int postiLiberi;

    public Proiezione(long idProiezione, Film film, LocalDate dataProiezione,
                       LocalTime oraProiezione, BigDecimal costoBiglietto, int postiLiberi) {
        this.idProiezione = idProiezione;
        this.film = film;
        this.dataProiezione = dataProiezione;
        this.oraProiezione = oraProiezione;
        this.costoBiglietto = costoBiglietto;
        this.postiLiberi = postiLiberi;
    }

    public long getIdProiezione() {
        return idProiezione;
    }

    public Film getFilm() {
        return film;
    }

    public LocalDate getDataProiezione() {
        return dataProiezione;
    }

    public LocalTime getOraProiezione() {
        return oraProiezione;
    }

    public BigDecimal getCostoBiglietto() {
        return costoBiglietto;
    }

    public int getPostiLiberi() {
        return postiLiberi;
    }

    // controlliamo se la proiezione deve ancora avvenire, confrontando data e ora
    // con l'istante attuale: uniamo data e ora in un unico LocalDateTime per poterli confrontare
    public boolean isFutura() {
        return LocalDateTime.of(dataProiezione, oraProiezione).isAfter(LocalDateTime.now());
    }

    @Override
    public String toString() {
        return film.getTitolo() + " - " + dataProiezione + " " + oraProiezione;
    }
}
