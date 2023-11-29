package de.dkfz.odcf.guide.service

import de.dkfz.odcf.guide.helper.AnyObject
import de.dkfz.odcf.guide.service.implementation.SpeciesServiceImpl
import de.dkfz.odcf.guide.service.interfaces.external.ExternalMetadataSourceService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
class SpeciesServiceTests : AnyObject {

    @InjectMocks
    lateinit var speciesServiceMock: SpeciesServiceImpl

    @Mock
    lateinit var externalMetadataSourceService: ExternalMetadataSourceService

    @Test
    fun `check getSpeciesForImport`() {
        `when`(externalMetadataSourceService.getValuesAsSetMap("speciesInfos")).thenReturn(
            setOf(
                mapOf(
                    "species" to "Human (Homo sapiens)",
                    "species_with_strain" to "Human (Homo sapiens) [No strain available]",
                    "strain" to "No strain available"
                ),
                mapOf(
                    "species" to "Mouse (Mus Musculus)",
                    "species_with_strain" to "Mouse (Mus Musculus) [Unknown]",
                    "strain" to "Unknown"
                ),
                mapOf(
                    "species" to "Mouse (Mus Musculus)",
                    "species_with_strain" to "Mouse (Mus Musculus) [abc]",
                    "strain" to "abc"
                ),
            )
        )

        val result = speciesServiceMock.getSpeciesForImport()

        assertThat(result).hasSize(2)
        assertThat(result).contains("Human (Homo sapiens)")
        assertThat(result).contains("Mouse (Mus Musculus)")
    }

    @TestFactory
    fun `check getSpeciesWithStrainForSpecies`() = listOf(
        "Human (Homo sapiens)" to "Human (Homo sapiens) [No strain available]",
        "Human (Homo sapiens)+Mouse (Mus Musculus)" to "Human (Homo sapiens) [No strain available]+Mouse (Mus Musculus) [Unknown]",
        "Human (Homo sapiens) [No strain available]" to "Human (Homo sapiens) [No strain available]",
        "Human (Homo sapiens) [Some strain]" to "Human (Homo sapiens) [Some strain]",
        "Human (Homo sapiens) [Some other strain]" to "Human (Homo sapiens) [Some other strain]",
        "Rat (Rattus norvegicus)" to "Rat (Rattus norvegicus)",
    ).map { (input, expected) ->
        DynamicTest.dynamicTest("when getSpeciesWithStrainForSpecies with $input then return $expected") {
            `when`(externalMetadataSourceService.getValuesAsSetMap("speciesInfos")).thenReturn(
                setOf(
                    mapOf(
                        "species" to "Human (Homo sapiens)",
                        "species_with_strain" to "Human (Homo sapiens) [No strain available]",
                        "strain" to "No strain available"
                    ),
                    mapOf(
                        "species" to "Human (Homo sapiens)",
                        "species_with_strain" to "Human (Homo sapiens) [Some strain]",
                        "strain" to "Some strain"
                    ),
                    mapOf(
                        "species" to "Mouse (Mus Musculus)",
                        "species_with_strain" to "Mouse (Mus Musculus) [Unknown]",
                        "strain" to "Unknown"
                    ),
                    mapOf(
                        "species" to "Rat (Rattus norvegicus)",
                        "species_with_strain" to "Rat (Rattus norvegicus) [Copenhagen]",
                        "strain" to "Copenhagen"
                    ),
                )
            )

            val result = speciesServiceMock.getSpeciesWithStrainForSpecies(input)

            assertThat(result).isEqualTo(expected)
        }
    }

    @TestFactory
    fun `check compareSets`() = listOf(
        listOf(setOf("a", "b"), setOf("a", "b")) to true,
        listOf(setOf("a", "b"), setOf("b", "a")) to true,
        listOf(setOf("NEW_SAMPLE"), setOf("a", "b")) to true,
        listOf(setOf("a", "NEW_SAMPLE"), setOf("a", "b")) to true,
        listOf(setOf("a"), setOf("a", "b")) to false,
        listOf(setOf("a", "b"), setOf("A", "B")) to false,
        listOf(setOf("a", "b", "c"), setOf("a", "b")) to false,
        listOf(setOf("a", "b"), setOf("c", "d")) to false,
        listOf(setOf("a", "b"), emptySet()) to false,
    ).map { (input, expected) ->
        DynamicTest.dynamicTest("when compareSets with ${input.first()} and ${input.last()} then return $expected") {

            val result = speciesServiceMock.compareSets(input.first(), input.last())

            assertThat(result).isEqualTo(expected)
        }
    }
}
