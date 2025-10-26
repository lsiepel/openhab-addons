# Public Transport Switzerland Binding

Connects to the Swiss Public Transport API to provide real-time public transport information. See the [Swiss Public Transport API documentation](https://transport.opendata.ch/).

For example, here is a station board in HABPanel. Download the [HABPanel Departure Board example](https://github.com/StefanieJaeger/HABPanel-departure-board).

![Departure board in HABPanel](doc/departure_board_habpanel.png)

## Supported Things

### Stationboard

Upcoming departures for a single station (as displayed on a station departure board).

#### Channels

| channel        | type   | description                                                                                  |
|----------------|--------|----------------------------------------------------------------------------------------------|
| departures#n   | String | A dynamic channel for each upcoming departure                                                |
| tsv (advanced) | String | A TSV containing the fields:<br />`identifier, departureTime, destination, track, delay`     |

#### UI-based configuration

`station` is the station name for which to display departures.
The name must match the Swiss Federal Railways (SBB) naming.
See the [SBB official website](https://sbb.ch/en).

#### Textual configuration

##### Thing

```java
Thing publictransportswitzerland:stationboard:zurich [ station="Zürich HB" ]
```

##### Items

```java
String Next_Departure             "Next Departure"             { channel="publictransportswitzerland:stationboard:zurich:departures#1" }
String Upcoming_Departures_TSV    "Upcoming_Departures_TSV"    { channel="publictransportswitzerland:stationboard:zurich:tsv" }
```

## Discovery

This binding does not support auto-discovery.
