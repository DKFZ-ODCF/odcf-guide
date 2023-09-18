package de.dkfz.odcf.guide.controller

import org.springframework.boot.web.servlet.error.ErrorController
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import javax.servlet.RequestDispatcher
import javax.servlet.http.HttpServletRequest

@Controller
@RequestMapping("/error")
class CustomErrorController : ErrorController {

    @RequestMapping("")
    fun handleError(model: Model, request: HttpServletRequest): String {
        val status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)
        if (status == 404) {
            return "error404"
        }
        model["code"] = status
        model["errorMessage"] = (request.getAttribute(RequestDispatcher.ERROR_MESSAGE) as String).takeIf { it.isNotBlank() }
            ?: (request.getAttribute(RequestDispatcher.ERROR_EXCEPTION) as Exception).message
            ?: "No error message available"
        return "error"
    }

    @GetMapping("/{code}")
    fun showErrorPage(model: Model, @PathVariable code: Int, @RequestParam(required = false) parameter: String?): String {
        when (code) {
            403 -> model["errorMessage"] = "You are not allowed to access this page!"
            404 -> model["errorMessage"] = "We can not find any submission with the uuid: $parameter."
            412 -> model["errorMessage"] = "We have detected that you are using an older version of your browser.\n" +
                "Since we do not support older versions, please update to the current version in order to fill out your submission."
            503 -> model["errorMessage"] = "ODCF Guide is currently down for maintenance. Please try again later."
        }
        model["code"] = code
        return "error"
    }

    override fun getErrorPath(): String? {
        return null
    }
}
