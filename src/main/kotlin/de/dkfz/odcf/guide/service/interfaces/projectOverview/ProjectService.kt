package de.dkfz.odcf.guide.service.interfaces.projectOverview

interface ProjectService {

    /**
     * Supplements the information from a call of [externalMetadataSourceService.getInfosByPerson] by the information about each project
     * taken from the projects cached in the GUIDE database.
     *
     * @param it List elements from a call of [externalMetadataSourceService.getInfosByPerson]
     * @return The list elements supplemented by the information
     */
    fun prepareMap(it: Map<String, String>): Map<String, String?>

    /**
     * Returns the size of the analysis folder in GB of a specific organizational unit as a String of the format "1 GB".
     *
     * @param organizationalUnit Name of the organizational unit to which the analysis folder belongs.
     * @return The size of the folder in GB as a string with the unit. If it is empty then returns "N/A".
     */
    fun getOEAnalysisFolderSize(organizationalUnit: String): String
}
