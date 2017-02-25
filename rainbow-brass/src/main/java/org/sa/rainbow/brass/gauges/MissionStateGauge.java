package org.sa.rainbow.brass.gauges;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sa.rainbow.core.error.RainbowException;
import org.sa.rainbow.core.gauges.RegularPatternGauge;
import org.sa.rainbow.core.models.commands.IRainbowOperation;
import org.sa.rainbow.core.util.TypedAttribute;
import org.sa.rainbow.core.util.TypedAttributeWithValue;

/**
 * Created by schmerl on 12/28/2016.
 */
public class MissionStateGauge extends RegularPatternGauge {
    private static final String   NAME        = "Mission State Gauge";
    protected static final String LOC         = "LocationRecording";
    protected static final String CHARGE = "BatteryCharge";

    protected static final String LOC_PATTERN = "topic: /amcl_pose/pose/pose.*position.*\\n.*x: (.*)\\n.*y: (.*)\\n.*z.*\\norientation.*\\n.*x: (.*)\\n.*y: (.*)\\n.*z: (.*)\\n.*w: (.*)";
    protected static final String CHARGE_PATTERN = "topic: /energy_monitor/voltage.*\\n.*data: (.*)\\n";
    protected String              last_x;
    protected String              last_y;
    private String                last_w;
    private int                   last_voltage   = 0;

    /**
     * Main Constructor the Gauge that is hardwired to the Probe.
     *
     * @param id
     *            the unique ID of the Gauge
     * @param beaconPeriod
     *            the liveness beacon period of the Gauge
     * @param gaugeDesc
     *            the type-name description of the Gauge
     * @param modelDesc
     *            the type-name description of the Model the Gauge updates
     * @param setupParams
     *            the list of setup parameters with their values
     * @param mappings
     *            the list of Gauge Value to Model Property mappings
     */
    public MissionStateGauge (String id, long beaconPeriod, TypedAttribute gaugeDesc, TypedAttribute modelDesc,
            List<TypedAttributeWithValue> setupParams, Map<String, IRainbowOperation> mappings)
                    throws RainbowException {
        super (NAME, id, beaconPeriod, gaugeDesc, modelDesc, setupParams, mappings);
        addPattern (LOC, Pattern.compile (LOC_PATTERN, Pattern.DOTALL));
        addPattern (CHARGE, Pattern.compile (CHARGE_PATTERN, Pattern.DOTALL));
    }

    @Override
    protected void doMatch (String matchName, Matcher m) {
        String group = m.group (1);
        if (LOC.equals (matchName)) {
            String x = group.trim ();
            String y = m.group (2).trim ();

            String a = m.group (3).trim ();
            String b = m.group (4).trim ();
            String c = m.group (5).trim ();
            String d = m.group (6).trim ();

            String w = yawFromQuarternion (a, b, c, d);

            if (locationDifferent (x, y, w)) {
                IRainbowOperation op = m_commands.get ("location");
                Map<String, String> pMap = new HashMap<> ();
                pMap.put (op.getParameters ()[0], x);
                pMap.put (op.getParameters ()[1], y);
                pMap.put (op.getParameters ()[2], w);
                issueCommand (op, pMap);
            }
        }
        else if (CHARGE.equals (matchName)) {
            int voltage = Integer.parseInt (group);
            if (voltageDifferent (voltage)) {
                double charge = PowerConverter.voltage2Charge (voltage);
                IRainbowOperation op = m_commands.get ("charge");
                Map<String, String> pMap = new HashMap<> ();
                pMap.put (op.getParameters ()[0], Double.toString (charge));
                issueCommand (op, pMap);
            }
        }
    }


    // Converstino to yaw from http://www.chrobotics.com/library/understanding-quaternions
    private String yawFromQuarternion (String a, String b, String c, String d) {
        double A = Double.parseDouble (a);
        double B = Double.parseDouble (b);
        double C = Double.parseDouble (c);
        double D = Double.parseDouble (d);

        double w = Math.atan ((2 * (A * B + C * D)) / (A * A - B * B - C * C + D * D));
        return Double.toString (w);

    }

    private boolean voltageDifferent (int voltage) {
        if (last_voltage != voltage) {
            last_voltage = voltage;
            return true;
        }
        return false;
    }

    private boolean locationDifferent (String x, String y, String w) {
        boolean different = !x.equals (last_x) || !y.equals (last_y) || !w.equals (last_w);
        if (different) {
            last_x = x;
            last_y = y;
            last_w = w;
        }
        return different;
    }
}
