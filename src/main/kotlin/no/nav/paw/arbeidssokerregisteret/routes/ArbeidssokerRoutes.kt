package no.nav.paw.arbeidssokerregisteret.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import no.nav.paw.arbeidssokerregisteret.domain.Foedselsnummer
import no.nav.paw.arbeidssokerregisteret.domain.request.VeilederRequest
import no.nav.paw.arbeidssokerregisteret.services.ArbeidssokerService
import no.nav.paw.arbeidssokerregisteret.services.AutorisasjonService
import no.nav.paw.arbeidssokerregisteret.utils.getNavAnsatt
import no.nav.paw.arbeidssokerregisteret.utils.getPidClaim
import no.nav.paw.arbeidssokerregisteret.utils.logger

fun Route.arbeidssokerRoutes(arbeidssokerService: ArbeidssokerService, autorisasjonService: AutorisasjonService) {
    route("/api/v1") {
        route("/arbeidssoker/") {
            authenticate("tokenx") {
                route("/perioder") {
                    post {
                        logger.info("Starter ny arbeidssøkerperiode for bruker")

                        val foedselsnummer = call.getPidClaim()
                        arbeidssokerService.startArbeidssokerperiode(foedselsnummer)

                        logger.info("Startet arbeidssøkerperiode for bruker")
                        call.respond(HttpStatusCode.Accepted)
                    }

                    put {
                        logger.info("Avslutter arbeidssøkerperiode for bruker")

                        val foedselsnummer = call.getPidClaim()
                        arbeidssokerService.avsluttArbeidssokerperiode(foedselsnummer)

                        logger.info("Avsluttet arbeidssøkerperiode for bruker")
                        call.respond(HttpStatusCode.Accepted)
                    }
                }
            }
        }

        route("/veileder/arbeidssoker") {
            authenticate("azure") {
                route("/perioder") {
                    post {
                        logger.info("Veileder starter ny arbeidssøkerperiode for bruker")

                        val foedselsnummer = Foedselsnummer(call.receive<VeilederRequest>().foedselsnummer)
                        val navAnsatt = call.getNavAnsatt()

                        val harNavBrukerTilgang =
                            autorisasjonService.verifiserVeilederTilgangTilBruker(navAnsatt, foedselsnummer)

                        if (!harNavBrukerTilgang) {
                            return@post call.respond(HttpStatusCode.Forbidden, "NAV-ansatt har ikke tilgang")
                        }

                        arbeidssokerService.startArbeidssokerperiode(foedselsnummer)

                        logger.info("Startet arbeidssøkerperiode for bruker")

                        call.respond(HttpStatusCode.Accepted)
                    }
                }
            }
        }
    }
}