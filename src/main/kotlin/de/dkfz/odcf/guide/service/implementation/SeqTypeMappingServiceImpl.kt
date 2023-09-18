package de.dkfz.odcf.guide.service.implementation

import de.dkfz.odcf.guide.SeqTypeRepository
import de.dkfz.odcf.guide.entity.metadata.SeqType
import de.dkfz.odcf.guide.exceptions.DuplicatedImportAliasException
import de.dkfz.odcf.guide.service.interfaces.SeqTypeMappingService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
open class SeqTypeMappingServiceImpl(
    private val seqTypeRepository: SeqTypeRepository
) : SeqTypeMappingService {

    override fun getSeqType(name: String): SeqType? {
        return seqTypeRepository.findAll().find { it.name == name || it.importAliases?.contains(name) ?: false }
    }

    @Throws(DuplicatedImportAliasException::class)
    @Transactional(rollbackFor = [Exception::class])
    override fun saveSeqType(
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
    ): SeqType {
        val seqType = seqTypeId?.let { seqTypeRepository.getOne(it) } ?: SeqType()

        seqType.name = name
        seqType.basicSeqType = basicSeqType
        seqType.needAntibodyTarget = needAntibodyTarget
        seqType.needLibPrepKit = needLibPrepKit
        seqType.lowCoverageRequestable = lowCoverageRequestable
        seqType.isDisplayedForUser = isDisplayedForUser
        seqType.isRequested = newSeqTypeRequest

        val importAliases = ilseNames?.trim(',')
        if (!importAliases.isNullOrBlank()) {
            importAliases.split(",").forEach { alias ->
                val checkSeqTypeWithAlias = getSeqType(alias)
                if (checkSeqTypeWithAlias != null && checkSeqTypeWithAlias != seqType) {
                    throw DuplicatedImportAliasException("This ILSe Name already exists for a different Seq Type, please choose something else.")
                }
            }
            seqType.setImportAliases(importAliases)
        } else seqType.importAliases = null

        seqType.singleCell = singleCell
        seqType.tagmentation = tagmentation

        seqTypeRepository.save(seqType)
        return seqType
    }
}
