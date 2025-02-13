package net.perfectdreams.loritta.website.routes.api.v1.loritta

import com.github.salomonbrys.kotson.*
import com.google.gson.JsonParser
import com.mrpowergamerbr.loritta.network.Databases
import com.mrpowergamerbr.loritta.utils.WebsiteUtils
import com.mrpowergamerbr.loritta.website.LoriWebCode
import com.mrpowergamerbr.loritta.website.WebsiteAPIException
import io.ktor.application.ApplicationCall
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveText
import net.perfectdreams.loritta.platform.discord.LorittaDiscord
import net.perfectdreams.loritta.tables.SpicyStacktraces
import net.perfectdreams.loritta.website.routes.BaseRoute
import net.perfectdreams.loritta.website.utils.extensions.respondJson
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.transaction

class PostErrorRoute(loritta: LorittaDiscord) : BaseRoute(loritta, "/api/v1/loritta/error/{type}") {
	override suspend fun onRequest(call: ApplicationCall) {
		val body = call.receiveText()
		val type = call.parameters["type"]

		val json = JsonParser.parseString(body).obj

		when (type) {
			"spicy" -> {
				val errorCodeId = transaction(Databases.loritta) {
					SpicyStacktraces.insertAndGetId {
						it[message] = json["message"].string
						it[spicyHash] = json["spicyHash"].nullString
						it[file] = json["file"].string
						it[line] = json["line"].int
						it[column] = json["column"].int
						it[userAgent] = json["userAgent"].nullString
						it[url] = json["url"].string
						it[spicyPath] = json["spicyPath"].nullString
						it[localeId] = json["localeId"].string
						it[isLocaleInitialized] = json["isLocaleInitialized"].bool
						it[userId] = json["userId"].nullLong
						it[currentRoute] = json["currentRoute"].nullString
						it[stack] = json["stack"].nullString
						it[receivedAt] = System.currentTimeMillis()
					}
				}

				call.respondJson(
						jsonObject(
								"errorCodeId" to errorCodeId.value
						)
				)
			}
			else -> throw WebsiteAPIException(
					HttpStatusCode.NotImplemented,
					WebsiteUtils.createErrorPayload(
							LoriWebCode.MISSING_PAYLOAD_HANDLER,
							"Type $type is not implemented yet!"
					)
			)
		}

		call.respondJson(jsonObject())
	}
}