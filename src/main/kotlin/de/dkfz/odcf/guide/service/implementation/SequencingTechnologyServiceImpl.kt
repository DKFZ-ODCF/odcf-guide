package de.dkfz.odcf.guide.service.implementation

import de.dkfz.odcf.guide.SequencingTechnologyRepository
import de.dkfz.odcf.guide.entity.metadata.SequencingTechnology
import de.dkfz.odcf.guide.service.interfaces.SequencingTechnologyService
import org.springframework.stereotype.Service

@Service
class SequencingTechnologyServiceImpl(
    private val sequencingTechnologyRepository: SequencingTechnologyRepository,
) : SequencingTechnologyService {

    override fun getSequencingTechnology(name: String): SequencingTechnology? {
        return sequencingTechnologyRepository.findAll().find { it.name == name || it.importAliases.contains(name) }
    }
}
