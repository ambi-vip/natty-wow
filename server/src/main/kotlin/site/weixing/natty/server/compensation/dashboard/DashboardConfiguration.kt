package site.weixing.natty.server.compensation.dashboard

import org.springframework.boot.autoconfigure.web.WebProperties
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.util.ResourceUtils
import org.springframework.web.bind.annotation.GetMapping

@Controller
class DashboardConfiguration(private val webProperties: WebProperties) {
    companion object {
        const val HOME_FILE = "index.html"
        const val TO_RETRY_NAV = "/to-retry"
        const val EXECUTING_NAV = "/executing"
        const val NEXT_RETRY_NAV = "/next-retry"
        const val NON_RETRYABLE_NAV = "/non-retryable"
        const val SUCCEEDED_NAV = "/succeeded"
        const val UNRECOVERABLE_NAV = "/unrecoverable"
    }

    private val homePageContent by lazy {
        val indexFilePath = webProperties.resources.staticLocations.first() + HOME_FILE
        val indexFile = ResourceUtils.getFile(indexFilePath)
        check(indexFile.exists()) { "$HOME_FILE not found in ${indexFile.absolutePath}" }
        indexFile.readBytes()
    }

    @GetMapping(
        *[
            "/",
            TO_RETRY_NAV,
            EXECUTING_NAV,
            NEXT_RETRY_NAV,
            NON_RETRYABLE_NAV,
            SUCCEEDED_NAV,
            UNRECOVERABLE_NAV,
        ],
    )
    fun home(): ResponseEntity<ByteArray> {
        return ResponseEntity.ok()
            .contentType(org.springframework.http.MediaType.TEXT_HTML)
            .body(homePageContent)
    }

}