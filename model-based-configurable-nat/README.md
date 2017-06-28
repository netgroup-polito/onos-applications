# MODEL BASED CONFIGURABLE NAT

This is an ONOS nat that uses the ONOS Configuration Agent in order to export the State and to be configured.

## Getting Started

### Prerequisites

You should have ONOS, to activate this bundle. You should have installed the ONOS Configuration Agent, and you should deploy it before you start activating the bundle.

### Installing

Modify the files in the configuration package:
- yangFile.yang : you may have to change the content of this defaul yang model.
- yinFile.txt : you may have to change the content of this file with the yin model correspondent to the content of the yangFile.
- mappingFile.txt : you can change the content of this file, that is composed by raws of "varInTheYangModel : varInTheJavaCode ;" where the varialbles are specified by all the path from the root to the element you want to consider, the lists and the maps are indicated by the square brackets that contain the index value (* if maps). See the default file if you need an example.

The project contains a properties file: appProperties.properties:
- change the value of baseUri to change the URI to contact the ONOS Configuration Agent, and coherently change the value of eventsUri to change the URI to opern the SSE channel.

Compile and install the bundle on ONOS using the appropriate onos-app command.

### RUN

Activate the bundle.

### CONFIGURE
After activating the application you need to configure the tenant, graph and application IDs that are used by the [ONOS configuration agent](https://github.com/netgroup-polito/onos-configuration-agent/blob/master/README.md) in order to contact the application.
This is done by using the onos Network Configuration system.

- Send a REST request as follows:

**POST http://{onos-address}:8181/onos/v1/network/configuration/**

```json
{
  "apps": {
    "it.polito.modelbasedconfiguration": {
      "nf-id":{
          "user-id" : "userID",
          "graph-id": "graphID",
          "nf-id": "nfID"
   } 
   }
  }
}
```

After pushing the configuration, you can check the log by typing log:tail on the onos cli.
