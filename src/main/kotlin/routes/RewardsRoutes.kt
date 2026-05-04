package routes

import data.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.pebbletemplates.pebble.PebbleEngine
import java.io.StringWriter
import utils.jsMode
import utils.timed
import auth.*
import utils.EmailService

data class RewardTier(val name: String, val cost: Int)

val REWARD_TIERS = listOf(
    RewardTier("Lounge Access Voucher", 5000),
    RewardTier("Priority Boarding", 10000),
    RewardTier("Free Checked Bag", 15000),
    RewardTier("Free Flight Upgrade", 50000)
)

fun Route.rewardsRoutes() {
    get("/rewards") { call.handleRewardsLoad() }
    post("/rewards/redeem") { call.handleRewardRedeem() }

    get("/test/add-points") { call.handleTestAddPoints() }
}

private suspend fun ApplicationCall.handleRewardsLoad() {
    timed("T0_rewards", jsMode()) {
        val logged_state : LoggedInState = loggedIn()
        if (!logged_state.logged_in || logged_state.session == null) {
            respondRedirect("/login")
            return@timed
        }

        val userQuery = UserData.queryByToken(logged_state.session.token)
        if (userQuery.isEmpty()) {
            respondRedirect("/login")
            return@timed
        }

        val user = userQuery.first().dataClass

        val availableRewards = REWARD_TIERS.filter { it.cost <= user.loyalityPoints }
        val upcomingRewards = REWARD_TIERS.filter { it.cost > user.loyalityPoints }

        val model = mapOf(
            "title" to "Loyalty Rewards",
            "inNav" to true,
            "availableRewards" to availableRewards,
            "upcomingRewards" to upcomingRewards
        )

        val pebble = getEngine()
        val template = pebble.getTemplate("rewards/index.peb")
        val writer = StringWriter()
        fullEvaluate(template, writer, model)
        respondText(writer.toString(), ContentType.Text.Html)
    }
}

private suspend fun ApplicationCall.handleRewardRedeem() {
    timed("T1_rewards_redeem", jsMode()) {
        val logged_state : LoggedInState = loggedIn()
        if (!logged_state.logged_in || logged_state.session == null) {
            respondText("<div class='error'>Not logged in</div>", ContentType.Text.Html)
            return@timed
        }

        val params = receiveParameters()
        val cost = params["cost"]?.toIntOrNull() ?: 0
        val name = params["name"] ?: "Reward"

        if (cost <= 0) {
            respondText("<div class='error'>Invalid reward</div>", ContentType.Text.Html)
            return@timed
        }

        val userQuery = UserData.queryByToken(logged_state.session.token)
        if (userQuery.isEmpty()) {
            respondText("<div class='error'>User not found</div>", ContentType.Text.Html)
            return@timed
        }

        val user = userQuery.first().dataClass

        if (user.loyalityPoints < cost) {
            respondText("<div class='error'>Not enough points</div>", ContentType.Text.Html)
            return@timed
        }

        user.awardPoints(-cost)

        RedeemedRewardData(
            userId = user.id,
            rewardName = name,
            redeemedAt = java.sql.Timestamp(System.currentTimeMillis())
        ).insertIntoDatabase()


        val loginQuery = LoginData.queryDatabase(
            whereArgs = WhereArgs("id = ?", listOf(user.loginId))
        )
        if (loginQuery.isNotEmpty()) {
            val userEmail = loginQuery.first().dataClass.email
            EmailService.sendRewardConfirmation(userEmail, name)
        }

        val successMessage = """
            <div class='success' style='color: var(--success); font-weight: bold; margin-bottom: 15px; padding: 10px; border: 1px solid var(--success); border-radius: 6px; background-color: #edf9f0;'>
                Successfully redeemed $name! It has been added to your account and a confirmation email was sent.
            </div>
            <script>setTimeout(() => window.location.reload(), 2000);</script>
        """.trimIndent()

        respondText(successMessage, ContentType.Text.Html)
    }
}


private suspend fun ApplicationCall.handleTestAddPoints() {
    timed("T2_test_add_points", jsMode()) {
        val logged_state = loggedIn()
        if (logged_state.logged_in && logged_state.session != null) {
            val user = UserData.queryByToken(logged_state.session.token).firstOrNull()?.dataClass
            if (user != null) {

                val amount = request.queryParameters["amount"]?.toIntOrNull() ?: 5000
                user.awardPoints(amount)

                respondText("Success! Added $amount points. Your new balance is: ${user.loyalityPoints}", ContentType.Text.Plain)
                return@timed
            }
        }
        respondText("Error: You must be logged in to add test points.", ContentType.Text.Plain, status = HttpStatusCode.Unauthorized)
    }
}
