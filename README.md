# jEEBus.PowerLimitation.ControllableSystem

This repository contains Java implementations for Controllable Systems of the
following EEBus Use Cases:

- Limitation of Power Consumption (LPC) according to specification version 1.0.0
  - And the UC Implementation Guideline specification version 1.0.0
- Limitation of Power Production (LPP) according to specification version 1.0.0

LPC enables devices like EV wallboxes, heat pumps or energy storage systems to have
their power consumption controlled by a control box or EMS. It is a technical
implementation for controllable devices ("Steuerbare Verbrauchseinrichtungen")
compliant to
[German regulation §14a EnGW](https://www.gesetze-im-internet.de/enwg_2005/__14a.html).

LPP offers the same features for energy producers like PV inverters. This enables
compliance to
[§9 EEG in Germany](https://www.gesetze-im-internet.de/eeg_2014/__9.html).

## Project Setup

The implementations are based on our
Libraries [jEEBus.SHIP](https://github.com/openmuc/jeebus.ship)
and [jEEBus.SPINE](https://github.com/openmuc/jeebus.spine).

This is a Gradle Project that comes with a packaged Gradle Wrapper (use `./gradlew`
on Linux and `gradlew.bat` on Windows systems). You can run the following command to
download all necessary dependencies and build the project:

```bash
./gradlew clean build
```

To run all contained unit tests, run

```bash
./gradlew test
```
