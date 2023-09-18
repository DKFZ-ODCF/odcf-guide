package de.dkfz.odcf.guide.service.interfaces

interface SpeciesService {

    /**
     * Returns a list of species from OTP filtered by the strains 'Unknown' and 'No strain available'.
     *
     * @return list of species - format: `common name(scientific name)`
     */
    fun getSpeciesForImport(): List<String>

    /**
     * Returns a list of species that might have been extended by the strain.
     *
     * @param species list of species as string, seperated with `+`. Could also contain a strain.
     *
     * @return list of species with strain as string -
     *
     * format: `common name(scientific name) [strain]+common name(scientific name) [strain]`
     */
    fun getSpeciesWithStrainForSpecies(species: String): String

    /**
     * Compares the species lists from customer input with the list from OTP
     *
     * @param speciesListFromSampleOtp list from OTP
     * @param speciesListFromSampleGuide list from customer input
     *
     * @return `true` if list from otp is empty or lists contain the same elements
     */
    fun compareSets(speciesListFromSampleOtp: Set<String>, speciesListFromSampleGuide: Set<String>): Boolean
}
