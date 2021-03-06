package de.techjava.mqtt.tf.device;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.tinkerforge.BrickletAmbientLight;
import com.tinkerforge.BrickletDistanceUS;
import com.tinkerforge.BrickletNFCRFID;
import com.tinkerforge.Device;
import com.tinkerforge.IPConnection;
import com.tinkerforge.NotConnectedException;
import com.tinkerforge.TimeoutException;

import de.techjava.mqtt.tf.comm.MqttSender;
import de.techjava.mqtt.tf.core.DeviceController;
import de.techjava.mqtt.tf.core.DeviceFactory;
import de.techjava.mqtt.tf.core.DeviceFactoryRegistry;
import de.techjava.mqtt.tf.core.EnvironmentHelper;

@Component
public class Ambilight implements DeviceFactory<BrickletAmbientLight>, DeviceController<BrickletAmbientLight> {

    private static final Logger logger = LoggerFactory.getLogger(Ambilight.class);

    @Value("${tinkerforge.ambilight.topic?:illuminance}")
    private String topic;
    @Value("${tinkerforge.ambilight.callbackperiod?:10000}")
    private long callbackperiod;
    @Value("${tinkerforge.ambilight.disabled?:no}")
    private String disabled;

    @Autowired
    private IPConnection ipcon;
    @Autowired
    private MqttSender sender;
    @Autowired
    private DeviceFactoryRegistry registry;
    @Autowired
    private EnvironmentHelper envHelper;

    @PostConstruct
    public void init() {
        registry.registerDeviceFactory(BrickletAmbientLight.DEVICE_IDENTIFIER, this);
        registry.registerDeviceController(BrickletAmbientLight.DEVICE_IDENTIFIER, this);
    }

    @Override
    public BrickletAmbientLight createDevice(String uid) {
        BrickletAmbientLight bricklet = new BrickletAmbientLight(uid, ipcon);
        return bricklet;
    }

    @Override
    public void setupDevice(final String uid, final BrickletAmbientLight bricklet) {
        boolean enable = !envHelper.isDisabled(uid, disabled);

        if (enable) {
            bricklet.addIlluminanceListener((illuminance) -> {
                sender.sendMessage(envHelper.getTopic(uid) + topic, illuminance);
            });
        } else {
            logger.info("Ambilight listener disabled");
        }
        try {
            if (enable) {
                bricklet.setIlluminanceCallbackPeriod(envHelper.getCallback(uid, callbackperiod));
            }
        } catch (
                 TimeoutException | NotConnectedException e) {
            logger.error("Error setting Illuminance Callback Period", e);
        }
        logger.info("Ambilight sensor with uid {} initialized.", uid);
    }
}
