package site.weixing.natty.domain.common.filestorage.utils

import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.util.*
import java.util.regex.Pattern

open class FilenameUtils {

    open fun getDownloadName(filename: String, userAgent: String): String {
        var filename = filename
        var userAgent = userAgent
        val p = Pattern.compile("\\s*|\t|\r|\n")
        val m = p.matcher(filename)
        filename = m.replaceAll("")

        try {
            userAgent = userAgent.uppercase(Locale.getDefault())
            if (userAgent.indexOf("SAFARI") > 0 && userAgent.indexOf("IPHONE") > 0) {
                var encodeFileName = URLEncoder.encode(filename, "UTF-8")

                var count: Int
                count = 0
                while (encodeFileName.length > 100 && count < 200) {
                    filename = filename.substring(0, filename.length - 1)
                    encodeFileName = URLEncoder.encode(filename, "UTF-8")
                    ++count
                }

                if (count != 0) {
                    encodeFileName = encodeFileName + ".."
                }

                return encodeFileName
            }

            if (userAgent.indexOf("MSIE") > 0) {
                filename = URLEncoder.encode(filename, "UTF-8")
            } else if (userAgent.indexOf("SAFARI") > 0) {
                if (userAgent.lastIndexOf("CHROME") > -1) {
                    filename = URLEncoder.encode(filename, "UTF-8")
                } else {
                    filename = String(filename.toByteArray(charset("UTF-8")), charset("ISO8859-1"))
                }
            } else {
                filename = URLEncoder.encode(filename, "UTF-8")
            }
        } catch (var6: UnsupportedEncodingException) {
        }

        return filename
    }

}