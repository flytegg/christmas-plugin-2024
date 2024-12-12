package gg.flyte.christmas.donation

import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * Called when a new donation is detected when polling the API.
 *
 * @property donationId The unique identifier of the donation.
 * @property donorName The name of the donor.
 * @property comment Any comment made by the donor.
 * @property amount The number value of the donation.
 * @property timestamp The epoch millis of the donation.
 */
class DonateEvent(
    val donationId: String,
    val donorName: String?,
    val comment: String?,
    val amount: String,
    val finalAmount: String,
    val timestamp: Long
) : Event() {
    override fun getHandlers(): HandlerList {
        return HANDLERS_LIST
    }

    companion object {
        private val HANDLERS_LIST = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList {
            return HANDLERS_LIST
        }
    }
}