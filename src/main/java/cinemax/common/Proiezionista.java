package cinemax.common;

import java.time.LocalDate;

// Utente con ruolo "proiezionista": gestisce il palinsesto, cioe' film e proiezioni
public class Proiezionista extends Utente {

    private static final long serialVersionUID = 1L;

    public Proiezionista(long idUtente, String nome, String cognome, String username,
                          LocalDate dataNascita, String luogoDomicilio) {
        super(idUtente, nome, cognome, username, dataNascita, luogoDomicilio);
    }

    @Override
    public String getRuolo() {
        return "proiezionista";
    }
}
