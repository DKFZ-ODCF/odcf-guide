package de.dkfz.odcf.guide.configuration

import de.dkfz.odcf.guide.SequencingTechnologyRepository
import de.dkfz.odcf.guide.ValidationLevelRepository
import org.springframework.beans.factory.InitializingBean
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

@Component
class ConfigurationGuard(
    private val env: Environment,
    private val validationLevelRepository: ValidationLevelRepository,
    private val sequencingTechnologyRepository: SequencingTechnologyRepository,
) : InitializingBean {

    /**
     * List of required properties that are needed after the application is started
     */
    private val requiredProperties = setOf(
        "application.mails.reminders.cronExpression",
        "application.mails.reminders.hoursSinceMetadataAvailable",
        "application.mails.reminders.sendmail",
        "application.mails.senderAddress",
        "application.mails.sendmail",
        "application.mails.submitterMails",
        "application.mails.ticketSystemAddress",
        "application.mails.ticketSystemBaseUrl",
        "application.mails.ticketSystemPrefix",
        "application.midterm",
        "application.projectOverview.cron.otp",
        "application.projectOverview.cron.filesystem",
        "application.projectOverview.cron.ldap",
        "application.projectOverview.pathToFolderSizeDeterminationScript",
        "application.projectOverview.lsfOutputPath",
        "application.projectPathTemplate",
        "application.ssh.fingerprint",
        "application.ssh.host",
        "application.ssh.privateKeyFile",
        "application.timeout",
        "application.serverUrl",
        "backdoor.user",
        "backdoor.user.mail",
        "ilse.api.password",
        "ilse.api.url",
        "ilse.api.username",
        "ldap.base.dn",
        "ldap.password",
        "ldap.urls",
        "ldap.user.search.filter",
        "ldap.username",
        "logging.config",
        "logging.file",
        "logging.mailFile",
        "externalMetadataSourceService.adapter.url",
        "otp.url.overview",
        "otp.url.userManagement",
        "quota.datasource.password",
        "quota.datasource.url",
        "quota.datasource.username",
        "quota.rootPath",
        "server.port",
        "server.tomcat.max-parameter-count",
        "spring.datasource.password",
        "spring.datasource.url",
        "spring.datasource.username",
        "spring.flyway.baseline-on-migrate",
        "spring.jpa.hibernate.ddl-auto",
        "spring.jpa.properties.hibernate.enable_lazy_load_no_trans",
        "spring.mail.host",
        "spring.mail.port",
        "spring.mail.properties.mail.smtp.auth",
        "spring.mail.properties.mail.smtp.starttls.enable",
        "spring.mail.protocol",
        "spring.mvc.favicon.enabled",
        "spring.thymeleaf.cache",
    )

    /**
     * @return true if tests are running
     */
    private val isTestEnvironment: Boolean
        get() {
            for (element in Thread.currentThread().stackTrace) {
                if (element.className.startsWith("org.junit.")) {
                    return true
                }
            }
            return false
        }

    /**
     * @return true if application is running
     */
    private val isNotTestEnvironment: Boolean
        get() = !isTestEnvironment

    /**
     * Checks all required properties after sourcing the properties file if application is started
     */
    override fun afterPropertiesSet() {
        if (isNotTestEnvironment) {
            requiredProperties.forEach {
                env.getRequiredProperty(it)
            }
            val validationLevels = validationLevelRepository.findByDefaultObjectIsTrue()
            if (validationLevels.size != 1) {
                throw IllegalStateException(
                    "No or too many [${validationLevels.size}] validation levels are set as default " +
                        "[${validationLevels.joinToString { it.name }}]"
                )
            }
            val sequencingTechnologies = sequencingTechnologyRepository.findByDefaultObjectIsTrue()
            if (sequencingTechnologies.size != 1) {
                throw IllegalStateException(
                    "No or too many [${sequencingTechnologies.size}] sequencing technologies are set as default " +
                        "[${sequencingTechnologies.joinToString { it.name }}]"
                )
            }
        }
    }
}
