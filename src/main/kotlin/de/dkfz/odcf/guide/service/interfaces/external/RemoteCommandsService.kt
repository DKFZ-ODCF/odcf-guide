package de.dkfz.odcf.guide.service.interfaces.external

import java.io.IOException

interface RemoteCommandsService {

    /**
     * Retrieves the result from a remote server based on the provided command.
     *
     * @param command the command to be executed on the remote server
     * @return the result obtained from the remote server as a String
     * @throws IOException if there is an error while communicating with the remote server
     */
    @Throws(IOException::class)
    fun getFromRemote(command: String): String
}
