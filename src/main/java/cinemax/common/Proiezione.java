package cinemax.common;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Singola proiezione di un {@link Film} in una data/ora, con relativo prezzo
 * (tabella {@code proiezioni}). {@code postiLiberi} e' un dato derivato, non
 * persistito (viene calcolato dalla vista {@code v_proiezioni_disponibilita},
 * vedi {@code doc/01_progettazione_database.md}, §3.3): 200 posti sala meno
 * la somma dei posti gia' prenotati per questa proiezione.
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

    /** {@code true} se la proiezione e' successiva all'istante corrente. */
    public boolean isFutura() {
        return LocalDateTime.of(dataProiezione, oraProiezione).isAfter(LocalDateTime.now());
    }

    @Override
    public String toString() {
        return film.getTitolo() + " - " + dataProiezione + " " + oraProiezione;
    }
}
