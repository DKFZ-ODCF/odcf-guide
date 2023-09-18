package de.dkfz.odcf.guide.service.interfaces.external

import net.schmizz.sshj.SSHClient
import java.io.IOException

interface RemoteCommandsService {

    /**
     * Sets up new remote SSH client and connects with the host specified in the property `application.ssh.host`
     * in the YML file for the run configurations.
     */
    @Throws(IOException::class)
    fun getSshClient(): SSHClient

    /**
     * Returns the result of running a command on a remote SSH client.
     * Opens a connection to a client, triggers [runCmd] and closes the connection again.
     *
     * @param command Command to be run
     * @return Resulting response string
     */
    @Throws(IOException::class)
    fun getFromRemote(command: String): String

    /**
     * Runs a command on a remote SSH client.
     *
     * @param sshClient SSHClient object where the command should be run.
     * @param command Command to be run
     *
     * @return The response string that is the result of running the command.
     */
    @Throws(IOException::class)
    fun runCmd(sshClient: SSHClient, command: String): String
}
