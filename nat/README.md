# NAT ONOS APPLICATION

This applications implement a Nat through the reactive SDN paradigm on top of the onos controller.
Currently is used just as test app in the scenario of the dynamic exportation of the capabilities, so this application, other than the address translation logic, provides:
- a port configuration module that allows the local orchestrator to specify the interfaces that the NAT should use;
- a [data scructure](tools/functional_capability.json) that describes the functional capability with the specifications, that is taken by the [apps-capabilities](../apps-capabilities/) application when this app is installed.

## Install
To install the application on a running onos instance run the following steps.

- download and build the source code through maven:

        git clone https://github.com/netgroup-polito/onos-applications
        cd onos-applications/nat
        mvn clean install

- then install the application:

        onos-app {onos-address} reinstall target/nat-1.0-SNAPSHOT.oar

(onos-address is the ip-address of onos server, for example 192.168.123.1)


## Activate
After installing the application, you can activate it through the onos cli by typing:

        app activate it.polito.onosapp.nat

To check that the app has been activated type log:tail from the onos cli.


## Configure
After activating the application you need to configure functional ports that it should use. This is done by using the onos Network Configuration system.

- Send a REST request as follows:

    **POST http://{onos-address}:8181/onos/v1/network/configuration/**

    ```json
    {
      "apps": {
        "it.polito.onosapp.nat": {
          "nat": {
            "ports":
              {
                "USER:1": {
                  "device-id": "of:0000000000000002",
                  "port-number": 4,
                  "flow-priority": 10
                },
                "WAN:0": {
                  "device-id": "of:0000000000000003",
                  "port-number": 4,
                  "flow-priority": 10
                }
              }
          }
        }
      }
    }
    ```
WAN:0 is the private port of the NAT, while USER:1 is the public one.

After pushing the configuration, you can check the log by typing log:tail on the onos cli.
