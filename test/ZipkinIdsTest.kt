package mjs.ktor.features.zipkin

import assertk.assertThat
import assertk.assertions.*
import io.ktor.application.install
import io.ktor.http.HttpMethod
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import mjs.ktor.features.zipkin.ZipkinIds.Feature.tracingPartsKey
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ZipkinIdsTest {

    @Nested
    inner class IdGeneration {
        @Test
        fun `should generate a 64-bit ID by default`() {
            assertThat(nextId()).hasLength(16)
        }

        @Test
        fun `should generate a 128-bit ID when specified`() {
            assertThat(nextId(IdLength.ID_128_BITS)).hasLength(32)
        }
    }

    @Nested
    inner class PathPrefixMatching {
        @Test
        fun `should initiate a trace if the path starts with a specified prefix`(): Unit = withTestApplication {
            application.install(ZipkinIds) {
                initiateTracePathPrefixes = arrayOf("/api")
            }
            handleRequest(HttpMethod.Post, "/api/v1/premium-sms").apply {
                assertThat(request.call.attributes.contains(tracingPartsKey)).isTrue()
            }
        }

        @Test
        fun `should not initiate a trace if the path does not start a specified prefix`(): Unit = withTestApplication {
            application.install(ZipkinIds) {
                initiateTracePathPrefixes = arrayOf("/api")
            }
            handleRequest(HttpMethod.Get, "/health").apply {
                assertThat(request.call.attributes.contains(tracingPartsKey)).isFalse()
            }
        }
    }

    @Nested
    inner class HttpHeaders {
        @Test
        fun `headers should not be set if the feature is not installed`(): Unit = withTestApplication {
            handleRequest(HttpMethod.Get, "/").apply {
                with(response.headers) {
                    assertThat(get(TRACE_ID_HEADER)).isNull()
                    assertThat(get(SPAN_ID_HEADER)).isNull()
                }
            }
        }

        @Test
        fun `b3 header should be read and set if present`(): Unit = withTestApplication {
            application.install(ZipkinIds)
            val traceId = nextId()
            val spanId = nextId()
            handleRequest(HttpMethod.Get, "/") {
                addHeader(B3_HEADER, "$traceId-$spanId")
            }.apply {
                with(response.headers) {
                    assertThat(get(B3_HEADER)).isEqualTo("$traceId-$spanId")
                }
            }
        }

        @Test
        fun `X-B3-TraceId and X-B3-SpanId headers should be read and set if present`(): Unit = withTestApplication {
            application.install(ZipkinIds) {
                b3Header = true
            }
            val traceId = nextId()
            val spanId = nextId()
            handleRequest(HttpMethod.Get, "/") {
                addHeader(TRACE_ID_HEADER, traceId)
                addHeader(SPAN_ID_HEADER, spanId)
            }.apply {
                with(response.headers) {
                    assertThat(get(TRACE_ID_HEADER)).isEqualTo(traceId)
                    assertThat(get(SPAN_ID_HEADER)).isEqualTo(spanId)
                }
            }
        }

        @Nested
        inner class HeaderTypeConfiguration {
            @Test
            fun `should set new X-B3- headers if b3 not configured and there are no tracing headers in request`(): Unit =
                withTestApplication {
                    application.install(ZipkinIds)
                    handleRequest(HttpMethod.Get, "/").apply {
                        with(response.headers) {
                            assertThat(contains(TRACE_ID_HEADER)).isTrue()
                            assertThat(contains(SPAN_ID_HEADER)).isTrue()
                            assertThat(contains(B3_HEADER)).isFalse()
                        }
                    }
                }

            @Test
            fun `should set new b3 headers if b3 is configured and there are no tracing headers in request`(): Unit =
                withTestApplication {
                    application.install(ZipkinIds) {
                        b3Header = true
                    }
                    handleRequest(HttpMethod.Get, "/").apply {
                        with(response.headers) {
                            assertThat(contains(TRACE_ID_HEADER)).isFalse()
                            assertThat(contains(SPAN_ID_HEADER)).isFalse()
                            assertThat(contains(B3_HEADER)).isTrue()
                        }
                    }
                }

        }
    }

    @Nested
    inner class CallAttributes {
        @Test
        fun `should not be set if the feature is not installed`(): Unit = withTestApplication {
            handleRequest(HttpMethod.Get, "/").apply {
                assertThat(request.call.attributes.getOrNull(tracingPartsKey)).isNull()
            }
        }

        @Test
        fun `should be set if the feature is installed`(): Unit = withTestApplication {
            application.install(ZipkinIds)
            val traceId = nextId(IdLength.ID_128_BITS)
            val spanId = nextId()
            handleRequest(HttpMethod.Get, "/") {
                addHeader(TRACE_ID_HEADER, traceId)
                addHeader(SPAN_ID_HEADER, spanId)
            }.apply {
                with(request.call.attributes[tracingPartsKey]) {
                    assertThat(this.traceId).isEqualTo(traceId)
                    assertThat(this.spanId).isEqualTo(spanId)
                }
            }
        }
    }
}
