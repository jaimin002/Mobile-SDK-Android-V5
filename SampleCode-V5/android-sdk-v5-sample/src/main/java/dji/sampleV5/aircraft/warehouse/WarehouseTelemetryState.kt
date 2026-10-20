package dji.sampleV5.aircraft.warehouse

/**
 * Application-level telemetry model representing aircraft status and readings
 * for the warehouse inspection application.
 *
 * Decoupled from direct DJI SDK callbacks so that UI components and business logic
 * do not depend on SDK-specific listener implementations.
 *
 * Default state represents a disconnected aircraft with no available telemetry.
 *
 * @property isConnected Aircraft connection status (true = connected, false = disconnected).
 * @property batteryPercent Remaining battery percentage (0..100), or null if unavailable.
 * @property altitudeMeters Flight altitude in meters relative to takeoff point, or null if unavailable.
 * @property horizontalSpeedMps Aircraft horizontal velocity in meters per second, or null if unavailable.
 * @property gpsSatelliteCount Number of connected GPS satellites, or null if unavailable.
 * @property flightMode Current flight controller mode or connection status string.
 */
data class WarehouseTelemetryState(
    val isConnected: Boolean = false,
    val batteryPercent: Int? = null,
    val altitudeMeters: Double? = null,
    val horizontalSpeedMps: Double? = null,
    val gpsSatelliteCount: Int? = null,
    val flightMode: String = "DISCONNECTED",
)
