package com.libre.alexa.alexa_signin;

import com.libre.alexa.constants.LSSDPCONST;
import com.libre.alexa.constants.MIDCONST;
import com.libre.alexa.luci.LUCIControl;

/**
 * Created by bhargav on 25/10/17.
 */

public class AlexaUtils {

    public static void sendAlexaMetaDataRequest(String ipAddress){
        new LUCIControl(ipAddress).SendCommand(MIDCONST.ALEXA_COMMAND, "DEVICE_METADATA_REQUEST", LSSDPCONST.LUCI_SET);
    }

    public static void sendAlexaRefreshTokenRequest(String ipAddress){
        String readAlexaRefreshToken = "READ_AlexaRefreshToken";
        new LUCIControl(ipAddress).SendCommand(208, readAlexaRefreshToken, 1);    }
}
