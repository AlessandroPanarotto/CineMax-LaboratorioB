/*
 * Progetto CineMax - Laboratorio Interdisciplinare B (a.a. 2025/2026)
 * Universita' degli Studi dell'Insubria
 *
 * Autore: Panarotto Alessandro - matricola 757930 - sede di Varese (VA)
 */
package cinemax.common;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.time.LocalDate;

/**
 * Interfaccia remota (RMI) per la registrazione e il login degli utenti.
 * Il server la implementa e la pubblica sul registry RMI; il client la usa
 * per chiamare questi metodi come se fossero locali.
 */
public interface IAutenticazioneService extends Remote {

    /**
     * Registra un nuovo utente con ruolo cliente.
     *
     * @param dataNascita facoltativa, puo' essere null se non fornita
     * @throws ServiceException se lo username scelto e' gia' occupato
     */
    Cliente registraCliente(String nome, String cognome, String username, String password,
                             LocalDate dataNascita, String luogoDomicilio)
            throws RemoteException, ServiceException;

    /**
     * Controlla username e password e, se sono corretti, restituisce
     * l'utente corrispondente (come Cliente, Proiezionista o Bigliettaio
     * a seconda del suo ruolo).
     *
     * @throws ServiceException se username o password non sono corretti
     */
    Utente login(String username, String password) throws RemoteException, ServiceException;
}
