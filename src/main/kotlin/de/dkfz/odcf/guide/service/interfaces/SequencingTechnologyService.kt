package de.dkfz.odcf.guide.service.interfaces

import de.dkfz.odcf.guide.entity.metadata.SequencingTechnology

interface SequencingTechnologyService {

    fun getSequencingTechnology(name: String): SequencingTechnology?
}
