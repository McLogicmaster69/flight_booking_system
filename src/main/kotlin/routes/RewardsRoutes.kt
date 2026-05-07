package routes

import data.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.StringWriter
import utils.jsMode
import utils.timed
import auth.*

data class RewardTier(
    val key: String,
    val name: String,
    val cost: Int,
    val supportsQuantity: Boolean = false,
)

data class SelectedReward(
    val key: String,
    val name: String,
    val cost: Int,
    val quantity: Int = 1,
)

val REWARD_TIERS =
    listOf(
        RewardTier("lounge", "Lounge Access Voucher", 5000),
        RewardTier("priority", "Priority Boarding", 10000),
        RewardTier("bag", "Free Checked Bag", 15000, true),
        RewardTier("upgrade", "Free Flight Upgrade", 30000, true),
        RewardTier("discount15", "15% Off Next Purchase", 40000),
    )

val SELECTED_REWARDS_BY_USER = mutableMapOf<String, List<SelectedReward>>()

fun Route.rewardsRoutes() {
    get("/rewards") { call.handleRewardsLoad() }
    post("/rewards/redeem") { call.handleRewardsRedeem() }
    get("/test/add-points") { call.handleTestAddPoints() }
}

private suspend fun ApplicationCall.handleRewardsLoad() {
    timed("T0_rewards", jsMode()) {
        val loggedState: LoggedInState = loggedIn()
        if (!loggedState.logged_in || loggedState.session == null) {
            respondRedirect("/login")
            return@timed
        }

        val userQuery = UserData.queryByToken(loggedState.session.token)
        if (userQuery.isEmpty()) {
            respondRedirect("/login")
            return@timed
        }

        val user = userQuery.first().dataClass

        val availableRewards = REWARD_TIERS.filter { it.cost <= user.loyaltyPoints }
        val upcomingRewards = REWARD_TIERS.filter { it.cost > user.loyaltyPoints }

        val model =
            mapOf(
                "title" to "Loyalty Rewards",
                "inNav" to true,
                "user" to user,
                "availableRewards" to availableRewards,
                "upcomingRewards" to upcomingRewards,
            )

        val pebble = getEngine()
        val template = pebble.getTemplate("rewards/index.peb")
        val writer = StringWriter()
        fullEvaluate(template, writer, model)
        respondText(writer.toString(), ContentType.Text.Html)
    }
}

private suspend fun ApplicationCall.handleRewardsRedeem() {
    timed("T1_rewards_redeem", jsMode()) {
        val loggedState = loggedIn()

        if (!loggedState.logged_in || loggedState.session == null) {
            respondRedirect("/login")
            return@timed
        }

        val user =
            UserData
                .queryByToken(loggedState.session.token)
                .firstOrNull()
                ?.dataClass

        if (user == null) {
            respondRedirect("/login")
            return@timed
        }

        val params = receiveParameters()
        val selected = mutableListOf<SelectedReward>()

        for (tier in REWARD_TIERS) {
            if (params["reward_${tier.key}"] == "on") {
                val quantity =
                    if (tier.supportsQuantity) {
                        params["quantity_${tier.key}"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
                    } else {
                        1
                    }

                selected.add(
                    SelectedReward(
                        key = tier.key,
                        name = tier.name,
                        cost = tier.cost,
                        quantity = quantity,
                    ),
                )
            }
        }

        val totalCost = selected.sumOf { it.cost * it.quantity }

        if (selected.isEmpty()) {
            respondText("Please select at least one reward.", status = HttpStatusCode.BadRequest)
            return@timed
        }

        if (totalCost > user.loyaltyPoints) {
            respondText("You do not have enough points for those rewards.", status = HttpStatusCode.BadRequest)
            return@timed
        }

        SELECTED_REWARDS_BY_USER[loggedState.session.token] = selected

        respondRedirect("/book")
    }
}

private suspend fun ApplicationCall.handleTestAddPoints() {
    timed("T2_test_add_points", jsMode()) {
        val loggedState = loggedIn()
        if (loggedState.logged_in && loggedState.session != null) {
            val user = UserData.queryByToken(loggedState.session.token).firstOrNull()?.dataClass
            if (user != null) {
                val amount = request.queryParameters["amount"]?.toIntOrNull() ?: 5000
                user.awardPoints(amount)

                respondText(
                    "Success! Added $amount points. Your new balance is: ${user.loyaltyPoints}",
                    ContentType.Text.Plain,
                )
                return@timed
            }
        }
        respondText(
            "Error: You must be logged in to add test points.",
            ContentType.Text.Plain,
            status = HttpStatusCode.Unauthorized,
        )
    }
}
