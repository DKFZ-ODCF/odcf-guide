package de.dkfz.odcf.guide.service.interfaces

import de.dkfz.odcf.guide.entity.metadata.SeqType
import de.dkfz.odcf.guide.exceptions.DuplicatedImportAliasException

interface SeqTypeMappingService {

    /**
     * Searches for a sequencing type object by a given name or `importAlias`.
     *
     * @param name Name or importAlias to which a seqType is searched for
     * @return SeqType object, if one with a matching name or importAlias is found
     */
    fun getSeqType(name: String): SeqType?

    /**
     * Save or update a sequencing type.
     *
     * @param name Name of the SeqType
     * @param seqTypeId SeqType ID, is used to find the correct SeqType object in case the name of it has changed.
     * @param basicSeqType Name of the BasicSeqType
     * @param ilseNames Import Aliases from Ilse
     * @param seqTypeOptions All the additional information the SeqType requires as a String,
     * e.g.: "needAntibodyTarget, singleCell, tagmentation, lowCoverageRequestable, isHiddenForUser"
     * @return the newly saved SeqType
     */
    @Throws(DuplicatedImportAliasException::class)
    fun saveSeqType(
        name: String,
        seqTypeId: Int?,
        basicSeqType: String,
        ilseNames: String?,
        seqTypeOptions: String?
    ): SeqType
}
