package de.dkfz.odcf.guide.controller

import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping

@Controller
class StaticPagesController {

    @RequestMapping("/frag-doch-mal-die-maus")
    fun showFaq(): String {
        return "faq"
    }

    @RequestMapping("/first-steps")
    fun showFirstSteps(): String {
        return "redirect:/first-steps/start"
    }

    @RequestMapping("/first-steps/{page}")
    fun showFirstSteps(model: Model, @PathVariable page: String): String {
        model["template"] = page
        return "first-steps"
    }

    // please note if you change this url also @LoginPageInterceptor
    @RequestMapping("/privacy-policy")
    fun showDataSecurityStatement(): String {
        return "privacy-policy"
    }

    @RequestMapping("/imprint")
    fun showImprint(): String {
        return "imprint"
    }
}
