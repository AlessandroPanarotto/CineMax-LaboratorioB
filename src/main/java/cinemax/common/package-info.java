/**
 * Contiene le classi condivise tra client e server di CineMax: le classi
 * del modello (Utente, Film, Proiezione, Prenotazione, ...) e le
 * interfacce dei servizi RMI (Remote).
 *
 * Questo pacchetto viene incluso sia nel jar del server sia in quello del
 * client: il server implementa davvero i servizi, il client usa solo le
 * interfacce per chiamarli da remoto. Le classi del modello viaggiano
 * tra client e server come oggetti serializzati.
 */
package cinemax.common;
