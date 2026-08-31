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
/**
 * Package radice dell'applicazione CineMax (Laboratorio Interdisciplinare B).
 *
 * <p>Sotto-package principali:</p>
 * <ul>
 *   <li>{@link cinemax.common} - modello di dominio e interfacce RMI condivise
 *       fra {@code serverCM} e {@code clientCM};</li>
 *   <li>{@code cinemax.server} - implementazione dei servizi (DAO, accesso a
 *       PostgreSQL via JDBC, servizi RMI), incluso solo in {@code serverCM.jar};</li>
 *   <li>{@code cinemax.client} - interfaccia utente Swing e controller,
 *       incluso solo in {@code clientCM.jar}.</li>
 * </ul>
 */
package cinemax;
