package cinemax.common;

import java.time.LocalDate;

/** Utente con ruolo {@code cliente}: puo' cercare proiezioni e prenotare posti. */
public class Cliente extends Utente {

    private static final long serialVersionUID = 1L;

    public Cliente(long idUtente, String nome, String cognome, String username,
                    LocalDate dataNascita, String luogoDomicilio) {
        super(idUtente, nome, cognome, username, dataNascita, luogoDomicilio);
    }

    @Override
    public String getRuolo() {
        return "cliente";
    }
}
