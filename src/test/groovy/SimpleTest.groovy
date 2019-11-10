import com.stehno.ersatz.Decoders
import com.stehno.ersatz.ErsatzServer
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

import static java.util.concurrent.TimeUnit.SECONDS
import static org.apache.http.entity.ContentType.APPLICATION_OCTET_STREAM

class SimpleTest extends Specification {
    ErsatzServer ersatzServer

    void 'Multiple responses for request'() {
        setup:
        ersatzServer = new ErsatzServer({
            timeout 5, SECONDS
        })
        def content = "TEXT"
        ersatzServer.expectations {
            head("/someFile") { responder { code(404) } } // the file is not there yet
            put('/someFile') {
                decoder APPLICATION_OCTET_STREAM.mimeType, Decoders.utf8String
                responder {
                    // validate request headers
                    header('Expect', '100-continue')

                    // request header is OK
                    code(100)

                    // validate request body
                    body(content, APPLICATION_OCTET_STREAM.mimeType)

                    // request body is OK
                    code(200)
                }
            }
        }

        when:
        def remoteFile = new URI("webdav://user:pwd@localhost:${ersatzServer.httpPort}/someFile")

        then:
        Files.copy(new ByteArrayInputStream(content.bytes), Paths.get(remoteFile), StandardCopyOption.REPLACE_EXISTING)

        and:
        ersatzServer.verify()
    }
}