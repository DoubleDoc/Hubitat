/*
*	HomeSeer HS-FLS100+-G2 Floodlight Sensor
*
*   Original work of Bryan Copeland (Github djdizzyd) extended by David Witt for new -G2 device 2021/01/06
*	version: 1.5 added support for new -G2 controls and changes, including temperature sensor reporting
*   Note: HomeSeer documentation states parameter 7 temp offset is in units Deg.C. Testing indicates actual units are Deg.F. Configuration description adjusted accordingly.
*
*
*
* Licensed under the Apache License, Version 2.0 (the "License");
*   you may not use this file except in compliance with the License.
*   You may obtain a copy of the License at
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
*   Unless required by applicable law or agreed to in writing, software
*   distributed under the License is distributed on an "AS IS" BASIS,
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*   See the License for the specific language governing permissions and
*   limitations under the License.
*/

import groovy.transform.Field

metadata {
    definition (name: "HomeSeer HS-FLS100+-G2 Floodlight Sensor", namespace: "DoubleDoc", author: "David Witt", importUrl: "https://raw.githubusercontent.com/DoubleDoc/hubitat-drivers/HS-FLS100+-G2 Floodlight.groovy") {
        capability "Switch"
        capability "Refresh"
        capability "Actuator"
        capability "Sensor"
        capability "Configuration"
        capability "MotionSensor"
        capability "IlluminanceMeasurement"
        capability "TemperatureMeasurement"

        fingerprint mfr:"000C", prod:"0513", deviceId:"000C", inClusters:"0x5E,0x85,0x8E,0x59,0x55,0x86,0x72,0x5A,0x73,0x9F,0x6C,0x7A,0x71,0x25,0x31,0x70,0x30", deviceJoinName: "HomeSeer HS-FLS100+-G2"
    }
    preferences {
        configParams.each { input it.value.input }
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
        input name: "txtEnable", type: "bool", title: "TXT Descriptive logging", defaultValue: false
    }
}
@Field static Map configParams = [
        1: [input: [name: "configParam1", type: "number", title: "PIR Trigger Off period", description: "seconds 8-720", defaultValue: 15, range:"8..720"], parameterSize: 2],
        2: [input: [name: "configParam2", type: "number", title: "Lux Threshold Settings", description: "lux 10-900", defaultValue: 50, range:"0..900"], parameterSize: 2],
        3: [input: [name: "configParam3", type: "number", title: "Lux and Temp reporting time", description: "minutes 0 to 1440 minutes", defaultValue: 10, range:"0..1440"], parameterSize: 2],
        4: [input: [name: "configParam4", type: "enum",   title: "PIR Trigger Alert", description: "", defaultValue: 1, options:[0:"Disable sending",1:"Enable sending"]], parameterSize: 1],
        5: [input: [name: "configParam5", type: "enum",   title: "Floodlight control mode", description: "", defaultValue: 1, options:[0:"Z-wave only",1:"Local and Z-wave"]], parameterSize: 1],
        6: [input: [name: "configParam6", type: "enum",   title: "Lux Sensor Mode for Light", description: "", defaultValue: 0, options:[0:"Night and motion",1:"All night"]], parameterSize: 1],    
        7: [input: [name: "configParam7", type: "decimal", title: "Temperature Calbration Offset", description: "-10.0 to +10.0 DegF", range:"-10..10"], parameterSize: 1, parameterPrecision: 1, parameterSigned: 1],
        8: [input: [name: "configParam8", type: "enum",   title: "PIR Sensitivity", description: "", defaultValue: 2, options:[0:"Low",1:"Mid",2:"High"]], parameterSize: 1]
]
@Field static Map ZWAVE_NOTIFICATION_TYPES=[0:"Reserverd", 1:"Smoke", 2:"CO", 3:"CO2", 4:"Heat", 5:"Water", 6:"Access Control", 7:"Home Security", 8:"Power Management", 9:"System", 10:"Emergency", 11:"Clock", 12:"First"]
@Field static Map CMD_CLASS_VERS=[0x20:1,0x86:2,0x72:2,0x5B:3,0x70:1,0x85:2,0x59:1,0x31:5,0x71:4]


void logsOff(){
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable",[value:"false",type:"bool"])
}

void configure() {
    if (!state.initialized) initializeVars()
    runIn(5,pollDeviceData)
}

void initializeVars() {
    // first run only
    state.initialized=true
    runIn(5, refresh)
}

void updated() {
    log.info "updated..."
    log.warn "debug logging is: ${logEnable == true}"
    unschedule()
    if (logEnable) runIn(1800,logsOff)
    List<hubitat.zwave.Command> cmds=[]
    cmds.addAll(runConfigs())
    sendToDevice(cmds)
}

 
// Sends all Hubitat configuration data parameters to the device, iterates through config items
List<hubitat.zwave.Command> runConfigs() {
    List<hubitat.zwave.Command> cmds=[]
      configParams.each { param, data -> cmds.addAll(configCmd(param, data.parameterSize, settings[data.input.name], data.parameterPrecision))}
    return cmds
}

List<hubitat.zwave.Command> pollConfigs() {
    List<hubitat.zwave.Command> cmds=[]
    configParams.each { param, data ->
        if (settings[data.input.name]) {
            cmds.add(zwave.configurationV1.configurationGet(parameterNumber: param.toInteger()))
        }
    }
    return cmds
}

// Sends individual Hubitat configuration data parameters to the device
List<hubitat.zwave.Command> configCmd(parameterNumber, size, scaledConfigurationValue, Precision) {
    List<hubitat.zwave.Command> cmds = []
    int paramd
    float paramf
    if (Precision != null && Precision != 0 )
        paramd = scaledConfigurationValue * Precision * 10
    else
    {    // watch out for errant decimal values, clear any out
        paramf = scaledConfigurationValue.toFloat();
        paramd = paramf
    }
    //queus cmd to send data out
    cmds.add(zwave.configurationV1.configurationSet(parameterNumber: parameterNumber.toInteger(), size: size.toInteger(), scaledConfigurationValue: paramd))
    // queue cmds to retreive the data just written for sync
    cmds.add(zwave.configurationV1.configurationGet(parameterNumber: parameterNumber.toInteger()))
    return cmds
}

// Loads returned Z-wave configuration parameter data back into Hubitat configuration fields
void zwaveEvent(hubitat.zwave.commands.configurationv1.ConfigurationReport cmd) {
    if(configParams[cmd.parameterNumber.toInteger()]) {
        Map configParam=configParams[cmd.parameterNumber.toInteger()]
        int scaledValue
        cmd.configurationValue.reverse().eachWithIndex { v, index -> scaledValue=scaledValue | v << (8*index) }
        // negative config paramaters support
        if (configParam.parameterSigned==1)
        {   // adjust for 2s-complement negative values
            log.info "sign work, ${configParam.parameterSize}, ${1<<7}"
            if (configParam.parameterSize==1)
            {    // 8 bit byte values                
                if (scaledValue >= (1<<7))
                {
                    scaledValue ^= 0xff
                    scaledValue += 1
                    scaledValue *= -1
                }
            }else
            {    //16 bit word values
                if (scaledValue >= (1<<15))
                {
                    scaledValue ^= 0x0ffff
                    scaledValue += 1
                    scaledValue *= -1
                }
            }
        }
        float paramd = scaledValue
        if (configParam.parameterPrecision != null && configParam.parameterPrecision != 0)
        {   // adjust for decimal placement
            paramd = scaledValue / (configParam.parameterPrecision * 10)
            // return the polled data to the configuraiton screen data field as decimal data
            device.updateSetting(configParam.input.name, [value: "${paramd}", type: configParam.input.type])
        }else
        {    // no decimal fields, do not place any
            // return the polled data to the configuraiton screen data field as int data
            device.updateSetting(configParam.input.name, [value: "${scaledValue}", type: configParam.input.type])
        }
    }
}

void pollDeviceData() {
    List<hubitat.zwave.Command> cmds = []
    cmds.add(zwave.versionV2.versionGet())
    cmds.add(zwave.manufacturerSpecificV2.deviceSpecificGet(deviceIdType: 1))
    cmds.addAll(processAssociations())
    cmds.addAll(pollConfigs())
    cmds.add(zwave.sensorMultilevelV5.sensorMultilevelGet(scale: 1, sensorType: 3))     // illuminance
    cmds.add(zwave.notificationV4.notificationGet(notificationType: 7, event:0))        // PIR
    cmds.add(zwave.switchBinaryV1.switchBinaryGet())                                    // light switch
    cmds.add(zwave.sensorMultilevelV5.sensorMultilevelGet(scale: 1, sensorType: 1))     // temperature
    sendToDevice(cmds)
}

void refresh() {
    List<hubitat.zwave.Command> cmds=[]
    cmds.add(zwave.switchBinaryV1.switchBinaryGet())                                   // light switch
    cmds.add(zwave.sensorMultilevelV5.sensorMultilevelGet(scale: 1, sensorType: 3))    // illuminance
    cmds.add(zwave.notificationV4.notificationGet(notificationType: 7, event:0))       // PIR
    cmds.add(zwave.sensorMultilevelV5.sensorMultilevelGet(scale: 1, sensorType: 1))    // temperature
    sendToDevice(cmds)
}

void installed() {
    if (logEnable) log.debug "installed()..."
    initializeVars()
}

void eventProcess(Map evt) {
    if (device.currentValue(evt.name).toString() != evt.value.toString() || !eventFilter) {
        evt.isStateChange=true
        sendEvent(evt)
    }
}

void zwaveEvent(hubitat.zwave.commands.sensormultilevelv5.SensorMultilevelReport cmd) {
    if (logEnable) log.debug "${cmd}"
    Map evt = [isStateChange:false]
    switch (cmd.sensorType) {
        case 1:
            evt.name = "temperature"
            evt.value = cmd.scaledSensorValue
            evt.unit = "DegF"
            evt.isStateChange=true
            evt.descriptionText="${device.displayName}: Temperature report received: ${evt.value}"
            break
        case 3:
            evt.name = "illuminance"
            evt.value = cmd.scaledSensorValue.toInteger()
            evt.unit = "lux"
            evt.isStateChange=true
            evt.descriptionText="${device.displayName}: Illuminance report received: ${evt.value}"
            break
    }
    if (evt.isStateChange) {
        if (txtEnable) log.info evt.descriptionText
        eventProcess(evt)
    }
}

void zwaveEvent(hubitat.zwave.commands.sensorbinaryv1.SensorBinaryReport cmd) {
    if (logEnable) log.debug "Sensor binary report: ${cmd}"
    // redundant and un-needed function
}

void zwaveEvent(hubitat.zwave.commands.notificationv4.NotificationReport cmd) {
    Map evt = [isStateChange:false]
    if (logEnable) log.info "Notification: " + ZWAVE_NOTIFICATION_TYPES[cmd.notificationType.toInteger()]
    if (cmd.notificationType==7) {
        // home security
        switch (cmd.event) {
            case 0:
                // state idle
                if (cmd.eventParametersLength > 0) {
                    switch (cmd.eventParameter[0]) {
                        case 7:
                            evt.name = "motion"
                            evt.value = "inactive"
                            evt.isStateChange = true
                            evt.descriptionText = "${device.displayName} motion became ${evt.value}"
                            break
                        case 8:
                            evt.name = "motion"
                            evt.value = "inactive"
                            evt.isStateChange = true
                            evt.descriptionText = "${device.displayName} motion became ${evt.value}"
                            break
                    }
                } else {
                    evt.name = "motion"
                    evt.value = "inactive"
                    evt.descriptionText = "${device.displayName} motion became ${evt.value}"
                    evt.isStateChange = true
                }
                break
            case 7:
                // motion detected (location provided)
                evt.name = "motion"
                evt.value = "active"
                evt.isStateChange = true
                evt.descriptionText = "${device.displayName} motion became ${evt.value}"
                break
            case 8:
                // motion detected
                evt.name = "motion"
                evt.value = "active"
                evt.isStateChange = true
                evt.descriptionText = "${device.displayName} motion became ${evt.value}"
                break
            case 254:
                // unknown event/state
                log.warn "Device sent unknown event / state notification"
                break
        }
    }else if (cmd.notificationType==8) {
        switch (cmd.event) {
            case 1:
            // Device powered on
            if (txtEnable) log.info "${device.displayName} powered on"
            break
        }
    }
    if (evt.isStateChange) {
        if (txtEnable) log.info evt.descriptionText
        eventProcess(evt)
    }
}

void zwaveEvent(hubitat.zwave.commands.basicv1.BasicReport cmd) {
    if (logEnable) log.debug cmd
    switchEvents(cmd)
}

private void switchEvents(hubitat.zwave.Command cmd) {
    String value = (cmd.value ? "on" : "off")
    String description = "${device.displayName} was turned ${value}"
    if (txtEnable) log.info description
    eventProcess(name: "switch", value: value, descriptionText: description, type: state.isDigital?"digital":"physical")
    state.isDigital=false
}

void zwaveEvent(hubitat.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd) {
    if (logEnable) log.debug cmd
    switchEvents(cmd)
}

void on() {
    state.isDigital=true
    sendToDevice(zwave.basicV1.basicSet(value: 0xFF))
}

void off() {
    state.isDigital=true
    sendToDevice(zwave.basicV1.basicSet(value: 0x00))
}

void zwaveEvent(hubitat.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) {
    hubitat.zwave.Command encapsulatedCommand = cmd.encapsulatedCommand(CMD_CLASS_VERS)
    if (encapsulatedCommand) {
        zwaveEvent(encapsulatedCommand)
    }
}

void parse(String description) {
    if (logEnable) log.debug "parse:${description}"
    hubitat.zwave.Command cmd = zwave.parse(description, CMD_CLASS_VERS)
    if (cmd) {
        zwaveEvent(cmd)
    }
}

void zwaveEvent(hubitat.zwave.commands.supervisionv1.SupervisionGet cmd) {
    if (logEnable) log.debug "Supervision get: ${cmd}"
    hubitat.zwave.Command encapsulatedCommand = cmd.encapsulatedCommand(CMD_CLASS_VERS)
    if (encapsulatedCommand) {
        zwaveEvent(encapsulatedCommand)
    }
    sendToDevice(new hubitat.zwave.commands.supervisionv1.SupervisionReport(sessionID: cmd.sessionID, reserved: 0, moreStatusUpdates: false, status: 0xFF, duration: 0))
}

void zwaveEvent(hubitat.zwave.commands.manufacturerspecificv2.DeviceSpecificReport cmd) {
    if (logEnable) log.debug "Device Specific Report: ${cmd}"
    switch (cmd.deviceIdType) {
        case 1:
            // serial number
            def serialNumber=""
            if (cmd.deviceIdDataFormat==1) {
                cmd.deviceIdData.each { serialNumber += hubitat.helper.HexUtils.integerToHexString(it & 0xff,1).padLeft(2, '0')}
            } else {
                cmd.deviceIdData.each { serialNumber += (char) it }
            }
            device.updateDataValue("serialNumber", serialNumber)
            break
    }
}

void zwaveEvent(hubitat.zwave.commands.versionv2.VersionReport cmd) {
    if (logEnable) log.debug "version2 report: ${cmd}"
    device.updateDataValue("firmwareVersion", "${cmd.firmware0Version}.${cmd.firmware0SubVersion}")
    device.updateDataValue("protocolVersion", "${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}")
    device.updateDataValue("hardwareVersion", "${cmd.hardwareVersion}")
}

void sendToDevice(List<hubitat.zwave.Command> cmds) {
    sendHubCommand(new hubitat.device.HubMultiAction(commands(cmds), hubitat.device.Protocol.ZWAVE))
}

void sendToDevice(hubitat.zwave.Command cmd) {
    sendHubCommand(new hubitat.device.HubAction(secureCommand(cmd), hubitat.device.Protocol.ZWAVE))
}

void sendToDevice(String cmd) {
    sendHubCommand(new hubitat.device.HubAction(secureCommand(cmd), hubitat.device.Protocol.ZWAVE))
}

List<String> commands(List<hubitat.zwave.Command> cmds, Long delay=300) {
    return delayBetween(cmds.collect{ secureCommand(it) }, delay)
}

String secureCommand(hubitat.zwave.Command cmd) {
    secureCommand(cmd.format())
}

String secureCommand(String cmd) {
    String encap=""
    if (getDataValue("zwaveSecurePairingComplete") != "true") {
        return cmd
    } else {
        encap = "988100"
    }
    return "${encap}${cmd}"
}

void zwaveEvent(hubitat.zwave.Command cmd) {
    if (logEnable) log.debug "skip:${cmd}"
}

List<hubitat.zwave.Command> setDefaultAssociation() {
    List<hubitat.zwave.Command> cmds=[]
    cmds.add(zwave.associationV2.associationSet(groupingIdentifier: 1, nodeId: zwaveHubNodeId))
    cmds.add(zwave.associationV2.associationGet(groupingIdentifier: 1))
    return cmds
}

List<hubitat.zwave.Command> processAssociations(){
    List<hubitat.zwave.Command> cmds = []
    cmds.addAll(setDefaultAssociation())
    if (logEnable) log.debug "processAssociations cmds: ${cmds}"
    return cmds
}


void zwaveEvent(hubitat.zwave.commands.associationv2.AssociationReport cmd) {
    if (logEnable) log.debug "${device.label?device.label:device.name}: ${cmd}"
    List<String> temp = []
    if (cmd.nodeId != []) {
        cmd.nodeId.each {
            temp.add(it.toString().format( '%02x', it.toInteger() ).toUpperCase())
        }
    }
    updateDataValue("zwaveAssociationG${cmd.groupingIdentifier}", "$temp")
}

void zwaveEvent(hubitat.zwave.commands.associationv2.AssociationGroupingsReport cmd) {
    if (logEnable) log.debug "${device.label?device.label:device.name}: ${cmd}"
    log.info "${device.label?device.label:device.name}: Supported association groups: ${cmd.supportedGroupings}"
    state.associationGroups = cmd.supportedGroupings
}
