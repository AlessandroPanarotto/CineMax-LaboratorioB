package cinemax.common;

import java.time.LocalDate;

// Utente con ruolo "cliente": e' chi puo' cercare le proiezioni e prenotare i posti
public class Cliente extends Utente {

    private static final long serialVersionUID = 1L;

    // non aggiunge campi rispetto a Utente, serve solo a fissare il ruolo
    public Cliente(long idUtente, String nome, String cognome, String username,
                    LocalDate dataNascita, String luogoDomicilio) {
        super(idUtente, nome, cognome, username, dataNascita, luogoDomicilio);
    }

    @Override
    public String getRuolo() {
        return "cliente";
    }
}
