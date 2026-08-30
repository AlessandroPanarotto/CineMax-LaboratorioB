package cinemax.common;

import java.io.Serializable;

/** Film del catalogo CineMax (tabella {@code film}). */
public class Film implements Serializable {

    private static final long serialVersionUID = 1L;

    private final long idFilm;
    private final String titolo;
    private final String genere;
    private final String regista;
    private final int anno;
    private final int durataMinuti;
    private final int etaMinima;

    public Film(long idFilm, String titolo, String genere, String regista,
                int anno, int durataMinuti, int etaMinima) {
        this.idFilm = idFilm;
        this.titolo = titolo;
        this.genere = genere;
        this.regista = regista;
        this.anno = anno;
        this.durataMinuti = durataMinuti;
        this.etaMinima = etaMinima;
    }

    public long getIdFilm() {
        return idFilm;
    }

    public String getTitolo() {
        return titolo;
    }

    public String getGenere() {
        return genere;
    }

    public String getRegista() {
        return regista;
    }

    public int getAnno() {
        return anno;
    }

    public int getDurataMinuti() {
        return durataMinuti;
    }

    public int getEtaMinima() {
        return etaMinima;
    }

    @Override
    public String toString() {
        return titolo + " (" + anno + ", " + regista + ")";
    }
}
