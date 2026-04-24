# jEEBus.PowerLimitation.ControllableSystem

[![maven-central](https://img.shields.io/maven-central/v/org.openmuc.jeebus.usecase.powerlimitation/abstract-controllablesystem?logo=apachemaven)](https://central.sonatype.com/namespace/org.openmuc.jeebus.usecase.powerlimitation)
[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/openmuc/jeebus.usecase.powerlimitation.controllablesystem)

This repository contains Java implementations of the Controllable System (CS) Actors
of the following EEBus Use Cases:

- Limitation of Power Consumption (LPC) according to specification version 1.0.0
   - UC Implementation Guideline specification version 1.0.0
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

There are four subprojects in the `projects` folder:
- `abstract-controllablesystem` contains the common parts of the LPC and LPP CS.
- `lpc-controllablesystem` contains the parts of the CS specific to LPC.
- `lpp-controllablesystem` contains the parts of the CS specific to LPP.
- `demo` contains a minimal example application showing how jEEBus can be configured
   and used.

## Quick Start

What sets our implementation apart from other EEBus stacks is that we also
implemented the high level Use Case specific logic present in the specification. This
includes the complete Controllable System state machine as well as automated
monitoring of the connection to the Energy Guard using heartbeats.

That means you can have a basic instance of an LPC/LPP Controllable System running
in your application by configuring and initializing jEEBus.SHIP, jEEBus.SPINE and
the desired Use Case. Then, register your own custom listener that is automatically
called whenever the active power limit of your device changes as defined in the
specification:

```java
lpcCs.addListener(((event, state, activeLimit) -> {
    // TODO: your listener goes here
}));
```

For the complete, executable example application, see the `demo` subproject.

## Contributing

We are currently working out the contribution process.

If you would like to contribute to this project, please get in touch with the
development team here on GitHub or use
[the contact form on our website](https://www.openmuc.org/contact/).

## License

Copyright (c) 2026 Fraunhofer ISE

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
https://www.eclipse.org/legal/epl-2.0 or the provided `LICENSE` file.

SPDX-License-Identifier: `EPL-2.0`
