package org.apache.synapse.core.axis2;

import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.*;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.modules.Module;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.MDC;
import org.apache.neethi.Assertion;
import org.apache.neethi.Policy;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.ServerManager;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.config.SynapseConfigurationBuilder;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * This is the Synapse Module implementation class, which would initialize Synapse when it is
 * deployed onto an Axis2 configuration.
 */
public class SynapseInitializationModule implements Module {

    private static final Log log = LogFactory.getLog(SynapseInitializationModule.class);
    private SynapseConfiguration synCfg;

    public void init(ConfigurationContext configurationContext,
        AxisModule axisModule) throws AxisFault {

        log.info("Initializing Synapse at : " + new Date());
        try {
            InetAddress addr = InetAddress.getLocalHost();
            if (addr != null) {
                String ipAddr = addr.getHostAddress();
                if (ipAddr != null) {
                    MDC.put("ip", ipAddr);
                }

                String hostname = addr.getHostName();
                if (hostname == null) {
                    hostname = ipAddr;
                }
                MDC.put("host", hostname);
            }
        } catch (UnknownHostException e) {
            log.warn("Unable to determine hostname or IP address of the server for logging", e);
        }

        log.info("Loading mediator extensions...");
        configurationContext.getAxisConfiguration().getConfigurator().loadServices();

        log.info("Initializing the Synapse configuration ...");
        synCfg = getConfiguration(configurationContext);

        log.info("Deploying the Synapse service..");
        AxisConfiguration axisCfg = configurationContext.getAxisConfiguration();
        AxisService synapseService = new AxisService(SynapseConstants.SYNAPSE_SERVICE_NAME);
        AxisOperation mediateOperation = new InOutAxisOperation(
            SynapseConstants.SYNAPSE_OPERATION_NAME);
        mediateOperation.setMessageReceiver(new SynapseMessageReceiver());
        synapseService.addOperation(mediateOperation);
        List transports = new ArrayList();
        transports.add(Constants.TRANSPORT_HTTP);
        transports.add(Constants.TRANSPORT_HTTPS);
        synapseService.setExposedTransports(transports);
        axisCfg.addService(synapseService);
        
        String thisServerName = ServerManager.getInstance().getServerName();
        if(thisServerName == null || thisServerName.equals("")) {
          try {
            InetAddress addr = InetAddress.getLocalHost();
            thisServerName = addr.getHostName();

          } catch (UnknownHostException e) {
            log.warn("Could not get local host name", e);
          }
          
          if(thisServerName == null || thisServerName.equals("")) {
            thisServerName = "localhost";
          }
        }
        log.info("Synapse server name : " + thisServerName);
        
        log.info("Deploying Proxy services...");
        
        for (ProxyService proxy : synCfg.getProxyServices()) {

            List pinnedServers = proxy.getPinnedServers();
            if (pinnedServers != null && !pinnedServers.isEmpty()) {
                if (!pinnedServers.contains(thisServerName)) {
                    log.info("Server name not in pinned servers list. Not deploying Proxy service : " + proxy.getName());
                    continue;
                }
            }

            proxy.buildAxisService(synCfg, axisCfg);
            log.info("Deployed Proxy service : " + proxy.getName());
            if (!proxy.isStartOnLoad()) {
                proxy.stop(synCfg);
            }
        }
        
        log.info("Synapse initialized successfully...!");
    }

    private static SynapseConfiguration getConfiguration(ConfigurationContext cfgCtx) {

        cfgCtx.setProperty("addressing.validateAction", Boolean.FALSE);
        AxisConfiguration axisConfiguration = cfgCtx.getAxisConfiguration();
        SynapseConfiguration synapseConfiguration;

        String config = ServerManager.getInstance().getSynapseXMLPath();

        if (config != null) {
            synapseConfiguration = SynapseConfigurationBuilder.getConfiguration(config);
        } else {
            log.warn("System property or init-parameter '" + SynapseConstants.SYNAPSE_XML +
                "' is not specified. Using default configuration..");
            synapseConfiguration = SynapseConfigurationBuilder.getDefaultConfiguration();
        }

        synapseConfiguration.setAxisConfiguration(cfgCtx.getAxisConfiguration());

        Parameter synapseCtxParam = new Parameter(SynapseConstants.SYNAPSE_CONFIG, null);
        synapseCtxParam.setValue(synapseConfiguration);
        MessageContextCreatorForAxis2.setSynConfig(synapseConfiguration);

        Parameter synapseEnvParam = new Parameter(SynapseConstants.SYNAPSE_ENV, null);
        Axis2SynapseEnvironment synEnv = new Axis2SynapseEnvironment(cfgCtx, synapseConfiguration);
        synapseEnvParam.setValue(synEnv);
        MessageContextCreatorForAxis2.setSynEnv(synEnv);

        try {
            axisConfiguration.addParameter(synapseCtxParam);
            axisConfiguration.addParameter(synapseEnvParam);

        } catch (AxisFault e) {
            String msg =
                "Could not set parameters '" + SynapseConstants.SYNAPSE_CONFIG +
                    "' and/or '" + SynapseConstants.SYNAPSE_ENV +
                    "'to the Axis2 configuration : " + e.getMessage();
            log.fatal(msg, e);
            throw new SynapseException(msg, e);
        }
        synapseConfiguration.init(synEnv);
        
        return synapseConfiguration;
    }

    public void engageNotify(AxisDescription axisDescription) throws AxisFault {
    }

    public boolean canSupportAssertion(Assertion assertion) {
        return false;
    }

    public void applyPolicy(Policy policy, AxisDescription axisDescription) throws AxisFault {
    }

    public void shutdown(ConfigurationContext configurationContext)
        throws AxisFault {
    	synCfg.destroy();
    }
}