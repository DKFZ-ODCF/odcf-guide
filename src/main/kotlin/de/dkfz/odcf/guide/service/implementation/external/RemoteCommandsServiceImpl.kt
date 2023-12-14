package de.dkfz.odcf.guide.service.implementation.external

import de.dkfz.odcf.guide.annotation.ExcludeFromJacocoGeneratedReport
import de.dkfz.odcf.guide.service.interfaces.external.RemoteCommandsService
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.common.IOUtils
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import java.io.IOException
import java.util.concurrent.TimeUnit

@Service
class RemoteCommandsServiceImpl(private val env: Environment) : RemoteCommandsService {

    /**
     * Represents an SSH client used for establishing SSH connections.
     */
    val client = SSHClient()

    /**
     * Initializes the SSH client.
     *
     * @throws IOException if an I/O error occurs during the SSH client initialization.
     */
    @Throws(IOException::class)
    @ExcludeFromJacocoGeneratedReport
    fun initClient() {
        client.addHostKeyVerifier(env.getRequiredProperty("application.ssh.fingerprint"))
        client.connect(env.getRequiredProperty("application.ssh.host"))
        client.authPublickey("icgcdata", env.getRequiredProperty("application.ssh.privateKeyFile"))
    }

    @Throws(IOException::class)
    @ExcludeFromJacocoGeneratedReport
    override fun getFromRemote(command: String): String {
        if (!client.isConnected) {
            initClient()
        }

        return client.startSession().use { session ->
            val cmd = session.exec("$command 2>&1")
            val response = IOUtils.readFully(cmd.inputStream).toString()
            cmd.join(5, TimeUnit.SECONDS)
            response
        }
    }
}
