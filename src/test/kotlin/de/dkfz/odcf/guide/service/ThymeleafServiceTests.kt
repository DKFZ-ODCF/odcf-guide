package de.dkfz.odcf.guide.service

import de.dkfz.odcf.guide.helper.EntityFactory
import de.dkfz.odcf.guide.service.implementation.ThymeleafService
import de.dkfz.odcf.guide.service.interfaces.security.LdapService
import de.dkfz.odcf.guide.service.interfaces.validator.CollectorService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

/* NOTE:
 * only gui getter so test is not that much important (only for coverage)
 * also we have to lookup if the whole service can be removed
 */
@SpringBootTest
class ThymeleafServiceTests @Autowired constructor(private val thymeleafService: ThymeleafService) {

    private val entityFactory = EntityFactory()

    @InjectMocks
    lateinit var thymeleafServiceMock: ThymeleafService

    @Mock
    lateinit var ldapService: LdapService

    @Mock
    lateinit var collectorService: CollectorService

    @Test
    fun `test if get current user return the person from ldap service`() {
        val person = entityFactory.getPerson()

        `when`(ldapService.getPerson()).thenReturn(person)

        val currUser = thymeleafServiceMock.getCurrentUser()

        assertThat(currUser).isEqualTo(person)
    }

    @Test
    fun `test if get urls for current user is empty`() {
        val person = entityFactory.getPerson()

        `when`(ldapService.getPerson()).thenReturn(person)

        val urls = thymeleafServiceMock.getUrlsForCurrentUser()

        assertThat(urls).isEmpty()
    }

    @Test
    fun `test if get urls for current user`() {
        val person = entityFactory.getPerson()

        `when`(ldapService.getPerson()).thenReturn(person)
        `when`(collectorService.getUrlsByPerson(person)).thenReturn(mapOf("key" to "value"))

        val urls = thymeleafServiceMock.getUrlsForCurrentUser()

        assertThat(urls["key"]).isEqualTo("value")
    }
}
