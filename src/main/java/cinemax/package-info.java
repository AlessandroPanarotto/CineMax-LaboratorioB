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
