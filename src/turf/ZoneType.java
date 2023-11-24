package turf;

// Represents if the zone has points
// REAL: gives points (exists in the API)
// CROSSING: gives no points (usually user-defined but could also be already owned by the user)
public enum ZoneType {
    REAL, CROSSING;
}
