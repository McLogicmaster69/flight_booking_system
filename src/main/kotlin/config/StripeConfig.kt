package config

import com.stripe.Stripe
import io.github.cdimascio.dotenv.dotenv

object StripeConfig {
    private val env = dotenv()

    fun init() {
        Stripe.apiKey =
            env["STRIPE_SECRET_KEY"]
                ?: error("STRIPE_SECRET_KEY missing in .env")
    }
}
