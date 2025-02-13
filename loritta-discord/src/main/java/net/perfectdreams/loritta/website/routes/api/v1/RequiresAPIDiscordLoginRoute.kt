package net.perfectdreams.loritta.website.routes.api.v1

import com.mrpowergamerbr.loritta.utils.WebsiteUtils
import com.mrpowergamerbr.loritta.website.LoriWebCode
import com.mrpowergamerbr.loritta.website.WebsiteAPIException
import io.ktor.application.ApplicationCall
import io.ktor.http.HttpStatusCode
import mu.KotlinLogging
import net.perfectdreams.loritta.platform.discord.LorittaDiscord
import net.perfectdreams.loritta.website.routes.BaseRoute
import net.perfectdreams.loritta.website.session.LorittaJsonWebSession
import net.perfectdreams.loritta.website.utils.extensions.lorittaSession
import net.perfectdreams.temmiediscordauth.TemmieDiscordAuth

abstract class RequiresAPIDiscordLoginRoute(loritta: LorittaDiscord, path: String) : BaseRoute(loritta, path) {
	companion object {
		private val logger = KotlinLogging.logger {}
	}

	abstract suspend fun onAuthenticatedRequest(call: ApplicationCall, discordAuth: TemmieDiscordAuth, userIdentification: LorittaJsonWebSession.UserIdentification)

	override suspend fun onRequest(call: ApplicationCall) {
		val session = call.lorittaSession

		val discordAuth = session.getDiscordAuthFromJson()
		val userIdentification = session.getUserIdentification(call)

		if (discordAuth == null || userIdentification == null)
			throw WebsiteAPIException(
					HttpStatusCode.Unauthorized,
					WebsiteUtils.createErrorPayload(
							LoriWebCode.UNAUTHORIZED,
							"Invalid Discord Authorization"
					)
			)

		val profile = com.mrpowergamerbr.loritta.utils.loritta.getOrCreateLorittaProfile(userIdentification.id)

		if (profile.isBanned)
			throw WebsiteAPIException(
					HttpStatusCode.Unauthorized,
					WebsiteUtils.createErrorPayload(
							LoriWebCode.BANNED,
							"You are Loritta Banned!"
					)
			)

		onAuthenticatedRequest(call, discordAuth, userIdentification)
	}
}