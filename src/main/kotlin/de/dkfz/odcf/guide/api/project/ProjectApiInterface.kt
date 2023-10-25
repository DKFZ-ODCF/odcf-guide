package de.dkfz.odcf.guide.api.project

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import javax.validation.Valid

@Tag(name = "Project", description = "The v2 project API")
@RequestMapping("/api/v2/project")
interface ProjectApiInterface {

    @Operation(summary = "project overview", description = "get project overview only with token")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "show all projects", content = [Content()]),
        ]
    )
    @GetMapping(value = ["/overview"])
    fun getOverview(
        @Parameter(description = "token", required = true) @Valid @RequestHeader(value = "User-Token", required = true) token: String
    ): ResponseEntity<*>
}
