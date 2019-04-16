/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import com.mashape.unirest.http.Unirest
import io.javalin.core.util.Header
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class TestEtags {

    val etagsEnabledApp = Javalin.create { it.autogenerateEtags = true }

    @Test
    fun `default app does not set etags for GET`() = TestUtil.test { app, http ->
        app.get("/") { ctx -> ctx.result("Hello!") }
        assertThat(Unirest.get(http.origin + "/").asString().body).isEqualTo("Hello!")
        assertThat(Unirest.get(http.origin + "/").asString().status).isEqualTo(200)
        assertThat(Unirest.get(http.origin + "/").asString().headers.getFirst(Header.ETAG)).isNullOrEmpty()
    }

    @Test
    fun `autogenerated etags are added to GET request`() = TestUtil.test(etagsEnabledApp) { app, http ->
        app.get("/automatic") { ctx -> ctx.result("Hello!") }
        val response = Unirest.get(http.origin + "/automatic").asString()
        assertThat(response.status).isEqualTo(200)
        assertThat(response.body).isEqualTo("Hello!")
        val etag = response.headers.getFirst(Header.ETAG)
        val response2 = Unirest.get(http.origin + "/automatic").header(Header.IF_NONE_MATCH, etag).asString()
        val etag2 = response2.headers.getFirst(Header.ETAG)
        assertThat(response2.status).isEqualTo(304)
        assertThat(response2.body).isNullOrEmpty()
        assertThat(etag).isEqualTo(etag2)
    }

    @Test
    fun `autogenerated etags are not added to PUT request`() = TestUtil.test(etagsEnabledApp) { app, http ->
        app.put("/automatic") { ctx -> ctx.result("Hello!") }
        val response = Unirest.put(http.origin + "/automatic").asString()
        assertThat(response.headers.getFirst(Header.ETAG)).isNullOrEmpty()
    }

    @Test
    fun `manual etags overwrite autogenerated etags`() = TestUtil.test(etagsEnabledApp) { app, http ->
        app.get("/manual") { ctx -> ctx.result("Hello!").header(Header.ETAG, "1234") }
        val response = Unirest.get(http.origin + "/manual").asString()
        assertThat(response.status).isEqualTo(200)
        assertThat(response.body).isEqualTo("Hello!")
        val etag = response.headers.getFirst(Header.ETAG)
        val response2 = Unirest.get(http.origin + "/manual").header(Header.IF_NONE_MATCH, etag).asString()
        assertThat(response2.status).isEqualTo(304)
        assertThat(response2.body).isNullOrEmpty()
        assertThat(etag).isEqualTo("1234")
    }

    @Test
    fun `manual etags work for PUT request`() = TestUtil.test { app, http ->
        app.put("/manual") { ctx -> ctx.result("Hello!").header(Header.ETAG, "1234") }
        val response = Unirest.put(http.origin + "/manual").asString()
        assertThat(response.headers.getFirst(Header.ETAG)).isEqualTo("1234")
    }

}
