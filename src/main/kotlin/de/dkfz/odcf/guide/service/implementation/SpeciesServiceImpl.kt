package de.dkfz.odcf.guide.service.implementation

import de.dkfz.odcf.guide.service.interfaces.SpeciesService
import de.dkfz.odcf.guide.service.interfaces.external.ExternalMetadataSourceService
import org.springframework.stereotype.Service

@Service
class SpeciesServiceImpl(
    private val externalMetadataSourceService: ExternalMetadataSourceService,
) : SpeciesService {

    override fun getSpeciesForImport(): List<String> {
        return externalMetadataSourceService.getValuesAsSetMap("speciesInfos").filter { hasNoStrain(it) }.mapNotNull { it["species"] }
    }

    override fun getSpeciesWithStrainForSpecies(species: String): String {
        val speciesList = species.split("+").map { it.trim() }
        val speciesFromOtp = externalMetadataSourceService.getValuesAsSetMap("speciesInfos")
        val mappedSpecies = speciesList.mapNotNull { speciesElement ->
            speciesFromOtp.find {
                it["species_with_strain"] == speciesElement || (it["species"] == speciesElement && hasNoStrain(it))
            }?.get("species_with_strain")
        }
        return mappedSpecies.joinToString("+").takeIf { it.isNotEmpty() } ?: species
    }

    override fun compareSets(speciesListFromSampleOtp: Set<String>, speciesListFromSampleGuide: Set<String>): Boolean {
        if (speciesListFromSampleOtp.contains("NEW_SAMPLE")) {
            return true
        }
        if (speciesListFromSampleOtp.size != speciesListFromSampleGuide.size) {
            return false
        }
        return speciesListFromSampleOtp.toSortedSet()
            .zip(speciesListFromSampleGuide.toSortedSet())
            .all { it.first == it.second }
    }

    private fun hasNoStrain(element: Map<String, String>): Boolean {
        return element["strain"] == "Unknown" || element["strain"] == "No strain available"
    }
}
