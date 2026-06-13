package com.skylark.detectormonitor.model;

/**
 * Mirrors the Firebase "alarm_status" node structure:
 *
 *  alarm_status/
 *    MONDATA/battery raw value  ("01" = 1, range 0-255)
 *      B01   → buzzer flag         ("01" = ON,  "00" = OFF)
 *      D62   → battery raw value  ("01" = 1, range 0-255)
 *      L01   → LED / smoke flag    ("01" = ON,  "00" = OFF)
 *    device_id       → "192.168.x.x"
 *    device_location → "Kitchen"
 *    holder_name     → "Vineela"
 *    house_number    → "4-56/2"
 *    location        → "Nagaram"
 */
public class DetectorData {

    // From MONDATA sub-object
    public String led;         // L01
    public String buzzer;      // B01
    public String battery;  // D62

    public String Switch; //S01

    // From root-level fields
    public String devId;       // device_id       (BUG FIX #4: was "DEVID")
    public String devLocation; // device_location (BUG FIX #4: was "DEVNAME")
    public String holderName;  // holder_name
    public String houseNumber; // house_number
    public String location;    // location
}
