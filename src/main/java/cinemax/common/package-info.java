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
