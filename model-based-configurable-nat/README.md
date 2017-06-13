# MODEL BASED CONFIGURABLE NAT

This is an ONOS nat that uses the ONOS Configuration Agent in order to 
## Getting Started

### Prerequisites

You should have ONOS, to activate this bundle. You should have installed the ONOS Configuration Agent, and you should deploy it before you start activating the bundle.

### Installing

Modify the files in the configuration package:
- yangFile.yang : you eventually can change the content of this defaul yang model.
- yinFile.txt : you eventually have to change the content of this file with the yin model correspondent to the content of the yangFile.
- mappingFile.txt : you can change the content of this file, that is composed by raws of "varInTheYangModel : varInTheJavaCode ;" where the varialbles are specified by all the path from the root to the element you want to consider, the lists and the maps are indicated by the square brackets that contain the index value (* if maps). See the default file if you need an example.

You can change the the id of the SDN Application (that will be used by the ONOS Configuraion Agent to address this application) by changing the String parameter (the second one) at line 109 fo the StateListener.java file.

You shold change the address (address:port) where to find the ONOS Configuration Agent web service at line 27 and 29 of the file ConnectionModuleClient.java.

Install the bundle on ONOS using the appropriate onos-app command.

#### RUN

Activate the bundle.
