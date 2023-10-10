package zone;

// Represents if the zone exists in the API or is user-defined
// REAL: exists in the API
// POINTLESS: exists in the API but gives no points (is already owned by user)
// CROSSING: user-defined, gives no points
public enum ZoneType {
    REAL, POINTLESS, CROSSING;
}
