package de.dkfz.odcf.guide.entity.requestedValues

import de.dkfz.odcf.guide.entity.metadata.SeqType
import javax.persistence.Entity
import javax.persistence.JoinColumn
import javax.persistence.OneToOne

@Entity
class SeqTypeRequestedValue() : RequestedValue() {

    @OneToOne
    @JoinColumn
    var requestedSeqType: SeqType? = null
}
