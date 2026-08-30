/**
 * Modello di dominio (entita' applicative) e interfacce {@link java.rmi.Remote}
 * che costituiscono il contratto client/server di CineMax.
 *
 * <p>Compilato sia in {@code serverCM.jar} sia in {@code clientCM.jar}:
 * il server ne fornisce le implementazioni concrete dei servizi, il client
 * ne usa solo le interfacce e le classi di dominio (che viaggiano sulla
 * connessione RMI come oggetti serializzati).</p>
 */
package cinemax.common;
