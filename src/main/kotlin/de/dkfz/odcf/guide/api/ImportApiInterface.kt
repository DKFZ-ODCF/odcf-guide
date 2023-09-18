package de.dkfz.odcf.guide.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.validation.Valid

@Tag(name = "Import", description = "The v2 import API")
@RequestMapping("/api/v2/import")
interface ImportApiInterface {

    @Operation(summary = "import ilse", description = "import ilse only with token")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "submission successfully imported", content = [Content()]),
            ApiResponse(responseCode = "208", description = "submission already in the database", content = [Content()]),
            ApiResponse(responseCode = "400", description = "submission cannot be processed", content = [Content()]),
        ]
    )
    @PostMapping(value = ["/ilse/{identifier}"], consumes = ["application/json"])
    @Throws(Exception::class)
    fun importIlse(
        @Parameter(description = "ilse identifier of submission", required = true) @PathVariable identifier: Int,
        @Parameter(description = "body", required = false) @Valid @RequestBody body: String?,
        @Parameter(description = "token", required = true) @Valid @RequestHeader(value = "User-Token", required = true) token: String
    ): ResponseEntity<String>
}
