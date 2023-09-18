package de.dkfz.odcf.guide.service.interfaces

/**
 * Browser service - Service to check the specifications of the currently used browser.
 */
interface BrowserService {

    /**
     * Checks if the browser that is currently in use is a version that can support all the features of the GUIDE.
     *
     * @param userAgent String in which the currently used browser and browser version is listed.
     * @return `false` if the currently used browser is too old to support all features of the GUIDE.
     */
    fun checkIfBrowserSupported(userAgent: String): Boolean
}
