package de.dkfz.odcf.guide.service.implementation.external

import de.dkfz.odcf.guide.service.interfaces.external.RemoteCommandsService
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.common.IOUtils
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import java.io.IOException
import java.util.concurrent.TimeUnit

@Service
class RemoteCommandsServiceImpl(private val env: Environment) : RemoteCommandsService {

    @Throws(IOException::class)
    override fun getSshClient(): SSHClient {
        val client = SSHClient()
        client.addHostKeyVerifier(env.getRequiredProperty("application.ssh.fingerprint"))
        client.connect(env.getRequiredProperty("application.ssh.host"))
        client.authPublickey("icgcdata", env.getRequiredProperty("application.ssh.privateKeyFile"))
        return client
    }

    @Throws(IOException::class)
    override fun runCmd(sshClient: SSHClient, command: String): String {
        var response: String

        sshClient.startSession().use { session ->
            val cmd = session.exec("$command 2>&1")
            response = IOUtils.readFully(cmd.inputStream).toString()
            cmd.join(5, TimeUnit.SECONDS)
        }
        return response
    }

    @Throws(IOException::class)
    override fun getFromRemote(command: String): String {
        val client = getSshClient()
        val response = runCmd(client, command)
        client.close()
        return response
    }
}
