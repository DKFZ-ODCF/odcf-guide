package de.dkfz.odcf.guide.service.implementation

import de.dkfz.odcf.guide.entity.Person
import de.dkfz.odcf.guide.service.interfaces.security.LdapService
import de.dkfz.odcf.guide.service.interfaces.validator.CollectorService
import org.springframework.stereotype.Service

@Service
open class ThymeleafService(
    private val ldapService: LdapService,
    private val collectorService: CollectorService
) {

    fun getCurrentUser(): Person {
        return ldapService.getPerson()
    }

    fun getCurrentUserIsAdmin(): Boolean {
        return ldapService.isCurrentUserAdmin()
    }

    fun getUrlsForCurrentUser(): Map<String, String> {
        return collectorService.getUrlsByPerson(ldapService.getPerson())
    }
}
