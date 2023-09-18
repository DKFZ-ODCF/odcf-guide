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
     * @param oldSeqTypeName Old name of the SeqType, is used to find the correct SeqType object in case the name of it has changed.
     * @param basicSeqType Name of the BasicSeqType
     * @param ilseNames Import Aliases from Ilse
     * @param needAntibodyTarget `true` if the SeqType requires an antibody target
     * @param singleCell `true` if the SeqType requires single cell
     * @param tagmentation `true` if the SeqType requires tagmentation
     * @param lowCoverageRequestable `true` if the SeqType can be used for lowCoverageRequested in a Sample
     * @param isDisplayedForUser `true` if the SeqType should be displayed to users
     * @return the newly saved SeqType
     */
    @Throws(DuplicatedImportAliasException::class)
    fun saveSeqType(
        name: String,
        seqTypeId: Int?,
        basicSeqType: String,
        ilseNames: String?,
        needAntibodyTarget: Boolean,
        needLibPrepKit: Boolean,
        singleCell: Boolean,
        tagmentation: Boolean,
        lowCoverageRequestable: Boolean,
        isDisplayedForUser: Boolean,
        newSeqTypeRequest: Boolean,
    ): SeqType
}
