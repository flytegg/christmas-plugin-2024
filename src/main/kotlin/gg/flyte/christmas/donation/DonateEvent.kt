package gg.flyte.christmas.donation

import gg.flyte.twilight.event.TwilightEvent
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * Called when a donation is made through the Tiltify API with a predetermined `campaignId`
 *
 * @property donorName The name of the donor.
 * @property comment Any comment made by the donor.
 * @property time The time the donation was made formatted in ISO-8601.
 * @property value The number value of the donation.
 * @property currency The currency in which the donation was made.
 * @property donationId The unique identifier of the donation.
 */
class DonateEvent(
    val donorName: String?,
    val comment: String?,
    val time: String?,
    val value: String?,
    val currency: String,
    val donationId: String
) : TwilightEvent()