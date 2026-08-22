-- Phase1 US: event travel radius is stored/displayed in Miles (not KM).
-- Safe to run on DBs that still have event_travel_radius_km.

ALTER TABLE user_identity_background
  CHANGE COLUMN event_travel_radius_km event_travel_radius_miles INT NULL;
