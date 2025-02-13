pluginManagement {
	resolutionStrategy {
		eachPlugin {
			if (requested.id.id == "kotlin2js") {
				useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:${requested.version}")
			}
		}
		eachPlugin {
			if (requested.id.id == "kotlin-multiplatform") {
				useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:${requested.version}")
			}
			if (requested.id.id == "kotlinx-serialization") {
				useModule("org.jetbrains.kotlin:kotlin-serialization:${requested.version}")
			}
		}
	}
}

rootProject.name = "loritta-parent"
include(":loritta-api")
include(":loritta-discord")
include(":loritta-eris")

// Plugins
include(":loritta-plugins")
include(":loritta-plugins:artsy-joy-lori")
include(":loritta-plugins:minecraft-stuff")
include(":loritta-plugins:quirky-stuff")
// include(":loritta-plugins:github-issue-sync")
include(":loritta-plugins:fortnite-stuff")
include(":loritta-plugins:profile-designs")
// include(":loritta-plugins:cloudflare-web-firewall")
// include(":loritta-plugins:christmas-2019-event")
include(":loritta-plugins:automated-locales")
include(":loritta-plugins:api-only-test")
include(":loritta-plugins:rosbife")
include(":loritta-plugins:funfunfun")
include(":loritta-plugins:funky")
include(":loritta-plugins:parallax-routes")
include(":loritta-plugins:donators-ostentation")
include(":loritta-plugins:staff-lorittaban")
include(":loritta-plugins:auto-banner-changer")
include(":loritta-plugins:loritta-birthday-2020-event")
include(":loritta-plugins:helping-hands")

// Website
include(":loritta-website")
// include(":loritta-website:sweet-morenitta")
include(":loritta-website:spicy-morenitta")
include(":loritta-website:lotrunfo-server")
include(":parallax-code-server")
include(":shard-controller")

// Watchdoggo
include(":loritta-watchdog-bot")

// Misc
include(":temmie-discord-auth")
include(":loritta-premium")
