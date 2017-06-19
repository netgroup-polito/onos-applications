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
- change the value of the property appId if you want to change the Application Id
- change the value of baseUri to change the URI to contact the ONOS Configuration Agent, and coherently change the value of eventsUri to change the URI to opern the SSE channel.

Compile and install the bundle on ONOS using the appropriate onos-app command.

#### RUN

Activate the bundle.
