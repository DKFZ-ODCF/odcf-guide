package de.dkfz.odcf.guide.service.implementation

import de.dkfz.odcf.guide.service.interfaces.BrowserService
import org.springframework.stereotype.Service

@Service
class BrowserServiceImpl : BrowserService {

    override fun checkIfBrowserSupported(userAgent: String): Boolean {
        if (userAgent.contains("Android|webOS|iPhone|iPad|iPod|BlackBerry|Windows Phone".toRegex())) {
            return false
        }

        val browser = browserSpecs(userAgent)[0]
        val version: Int
        var subversion = 0

        if (browser == "Safari" || browser == "Opera") {
            version = browserSpecs(userAgent)[1].split('.')[0].toInt()
            subversion = browserSpecs(userAgent)[1].split('.')[1].toInt()
        } else {
            version = browserSpecs(userAgent)[1].toInt()
        }

        return !(
            (browser == "Chrome" && version < 5) ||
                (browser == "Firefox" && version < 4) ||
                (browser == "MSIE" && version < 10) ||
                (browser == "Edge" && version < 10) ||
                (browser == "Safari" && version < 10) ||
                (browser == "Safari" && version == 10 && subversion < 1) ||
                (browser == "Opera" && version < 9) ||
                (browser == "Opera" && version == 9 && subversion < 6)
            )
    }

    /**
     * Splits up the [userAgent] String to determine which browser and which browser version is being used.
     *
     * @param userAgent String in which the currently used browser and browser version is listed.
     * @return List of the browser name and its version.
     */
    private fun browserSpecs(userAgent: String): List<String> {
        val check = """(Opera|Chrome|Safari|Firefox|MSIE|Trident(?=/))/?\s*(\d+)""".toRegex()
        val match = check.find(userAgent)?.destructured?.toList()

        var name = if (match != null) match[0] else ""
        var version = if (match != null) match[1] else ""

        if (name == "Trident") {
            val temp = """\brv[ :]+(\d+)""".toRegex().find(userAgent)?.destructured?.toList()
            version = if (temp != null) temp[0] else ""
            return listOf("MSIE", version)
        }

        if (name == "Chrome") {
            val temp = """\b(OPR|Edge)/(\d+)""".toRegex().find(userAgent)?.destructured?.toList()

            if (temp != null) {
                return if (temp[0] == "OPR") {
                    val temp2 = """OPR/((\d+)(.(\d+))?)""".toRegex().find(userAgent)?.destructured?.toList()
                    if (temp2 != null) {
                        version = temp2[0]
                    }
                    listOf("Opera", version)
                } else {
                    listOf(temp[0], temp[1])
                }
            }
        }

        if (name == "Safari") {
            val temp = """Safari/((\d+)(.(\d+))?)""".toRegex().find(userAgent)?.destructured?.toList()
            if (temp != null)
                return listOf("Safari", temp[0])
        }

        val temp = """version/(\d+)""".toRegex().find(userAgent)?.destructured?.toList()
        if (temp != null)
            version = temp[0]

        return listOf(name, version)
    }
}
