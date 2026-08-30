package cinemax.common;

import java.time.LocalDate;

/** Utente con ruolo {@code bigliettaio}: consulta e cerca le prenotazioni in biglietteria. */
public class Bigliettaio extends Utente {

    private static final long serialVersionUID = 1L;

    public Bigliettaio(long idUtente, String nome, String cognome, String username,
                        LocalDate dataNascita, String luogoDomicilio) {
        super(idUtente, nome, cognome, username, dataNascita, luogoDomicilio);
    }

    @Override
    public String getRuolo() {
        return "bigliettaio";
    }
}
