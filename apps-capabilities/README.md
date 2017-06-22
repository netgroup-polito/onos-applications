# APPS-CAPABILITIES ONOS APPLICATION

This onos application is in charge to listen the life cycle of other apps to keep an updated structure containing the functional capabilities currently available on the onos domain.

Any application that want to provide a funcional capability should maintain a json file describing it, that have to follow the relative portion of [this data model](https://github.com/netgroup-polito/frog4-domain-data-model).
The json **must be the content of the 'readme' field of the onos application**, with single quotation marks, like this:

    <![CDATA[ {'type':'function-type', ....} ]]>

This onos module performs following operations basing on other applications state:
- When a new application that provides a FC is installed it is added to the data structure.
- When the application becames **ACTIVE**, the FC is marked as **not-ready** becouse is not possible to have multiple instances of the same opp.
- When the state becomes **DEACTIVE** it is maked as **ready**.
- When the app is uninstalled from the system, it is also removed from the structure.
The application provides a REST interface that allow the onos domain orchestrator to dynamically request the FC list.

## Install
To install the application on a running onos instance run the following steps.

- download and build the source code through maven:

        $ sudo apt-get install maven
        $ git clone https://github.com/netgroup-polito/onos-applications
        $ cd onos-applications/apps-capabilities
        $ mvn clean install

- Finally you can install the application through the command:

        $ onos-app {onos-address} reinstall target/apps-capabilities-1.0-SNAPSHOT.oar

(onos-address is the ip-address of ONOS server, e.g., 192.168.123.1)


## Activate
After installing the application, you can activate it through the onos cli by typing:

        # Open the ONOS cli (in this example, we suppose that ONOS is listening at the address 192.168.123.1)
        $ client -h 192.192.123.1
        onos> app activate it.polito.onosapp.apps-capabilities

To check that the app has been activated type the following command in the onos cli:

        onos> log:tail


## API

- Retrieve the list of all the current functional capabilities:

    **GET http://{onos-address}:8181/onos/apps-capabilities/capability**

- Retrieve the description of a single functional capability by its name (application name):

    **GET http://{onos-address}:8181/onos/apps-capabilities/capability/{app-name}**
